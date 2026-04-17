package no.nav.eessi.pensjon.prefill

import no.nav.eessi.pensjon.eux.model.BucType.*
import no.nav.eessi.pensjon.eux.model.SedType.*
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.personoppslag.pdl.model.Ident
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentGruppe
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType.ALDER
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType.BARNEP
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType.GENRL
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType.GJENLEV
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType.OMSORG
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType.UFOREP
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.prefill.models.pensjon.PensjonCollection
import no.nav.eessi.pensjon.shared.api.ApiRequest
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class InnhentingService(
    private val personDataService: PersonDataService,
    private val pesysService: PesysService,
    @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper.ForTest(),
) {
    private var HentPerson: MetricsHelper.Metric
    private var addInstutionAndDocumentBucUtils: MetricsHelper.Metric

    private val logger = LoggerFactory.getLogger(InnhentingService::class.java)


    init {
        HentPerson = metricsHelper.init("HentPerson", ignoreHttpCodes = listOf(HttpStatus.BAD_REQUEST))
        addInstutionAndDocumentBucUtils = metricsHelper.init(
            "AddInstutionAndDocumentBucUtils",
            ignoreHttpCodes = listOf(HttpStatus.BAD_REQUEST)
        )
    }

    //Hjelpe funksjon for å validere og hente aktoerid for evt. avdodfnr eller avdødNpid fra UI (P2100) - PDL
    fun getAvdodAktoerIdPDL(request: ApiRequest): String? {
        val buc = request.buc ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Mangler Buc")
                if (request.riktigAvdod()?.isBlank() == true) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Bruker mangler fnr/dnr/npid.")
                }
        return when (buc) {
            P_BUC_02 -> {
                val norskIdent = request.riktigAvdod() ?: run {
                    if(request.gjenny) return null
                    logger.error("Mangler fnr eller npid for avdød")
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Mangler fnr for avdød")
                }
                try {
                    hentIdent(Ident.bestemIdent(norskIdent))
                } catch (ex: Exception) {
                    throw ResponseStatusException(HttpStatus.NOT_FOUND, "Korrekt aktoerIdent ikke funnet")
                }
            }
            P_BUC_05, P_BUC_06,P_BUC_10 -> {
                val norskIdent = request.riktigAvdod() ?: return null
                try {
                    hentIdent(Ident.bestemIdent(norskIdent))
                } catch (ex: Exception) {
                    throw ResponseStatusException(HttpStatus.NOT_FOUND, "Korrekt aktoerIdent ikke funnet")
                }
            }
            else -> null
        }
    }

    fun hentPersonData(prefillData: PrefillDataModel) : PersonDataCollection = personDataService.hentPersonData(prefillData)

    fun hentFnrEllerNpidFraAktoerService(aktoerid: String?): String? = aktoerid?.let { personDataService.hentFnrEllerNpidFraAktoerService(it) }

    fun hentIdent(norskIdent: Ident): String? = personDataService.hentIdent(IdentGruppe.AKTORID, norskIdent)?.id

    fun hentPensjoninformasjonCollection(prefillData: PrefillDataModel): PensjonCollection {
        val eessipensjonSakTyper = listOf(ALDER, BARNEP, GJENLEV, UFOREP)
        val pensakTyper = listOf(GENRL, OMSORG) + eessipensjonSakTyper

        val penSak = prefillData.penSaksnummer
        val fnr = prefillData.bruker.norskIdent
        val vedtaksId = prefillData.vedtakId
        return when (val sedType = prefillData.sedType) {

            P2000 -> {
                validateInputs(penSak to "sakId", fnr to "fnr")
                val p2000data = penSak?.let { pesysService.hentP2000data(fnr, penSak) }
                sjekkSakTypeKravSed(p2000data, ALDER)
                PensjonCollection(
                    p2xxxMeldingOmPensjonDto = p2000data.takeIf { p2000data?.sak?.sakType == ALDER } ,
                    vedtakId = vedtaksId,
                    sedType = sedType
                )
            }
            P2100 -> {
                validateInputs(penSak to "sakId", fnr to "fnr")

                val p2100data = penSak?.let { pesysService.hentP2100data(fnr, penSak) }
                PensjonCollection(
                    p2xxxMeldingOmPensjonDto = p2100data.takeIf { p2100data?.sak?.sakType in eessipensjonSakTyper } ,
                    vedtakId = vedtaksId,
                    sedType = sedType
                )
            }
            P2200 -> {
                validateInputs(penSak to "sakId", fnr to "fnr")

                val p2200data = penSak?.let { pesysService.hentP2200data(fnr, penSak) }
                sjekkSakTypeKravSed(p2200data, UFOREP)

                PensjonCollection(
                    p2xxxMeldingOmPensjonDto = p2200data.takeIf { p2200data?.sak?.sakType == UFOREP } ,
                    vedtakId = vedtaksId,
                    sedType = sedType
                )
            }
            P6000 -> {
                if (penSak.isNullOrBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Mangler sakId")
                validerVedtak(prefillData)
                PensjonCollection(
                    p6000Data = pesysService.hentP6000data(penSak),
                    vedtakId = vedtaksId,
                    sedType = sedType
                )
            }
            P8000 -> {
                if (prefillData.buc == P_BUC_05) {
                        try {
                            val p8000 = penSak?.let { pesysService.hentP8000data(it) }
                            if (p8000?.sakType !in pensakTyper ) {
                                throw ResponseStatusExceptionFeilSak(prefillData, p8000?.sakType)
                            }
                            PensjonCollection(
                                p8000Data = p8000.takeIf { p8000?.sakType != null && p8000.sakType in pensakTyper },
                                sedType = sedType
                            )
                        } catch (ex: Exception) {
                            logger.warn("Ingen pensjon!", ex)
                            PensjonCollection(sedType = sedType)
                        }
                } else {
                    PensjonCollection(sedType = sedType)
                }
            }
            P15000 -> {
                PensjonCollection(
                    p15000Data = penSak?.let { pesysService.hentP15000data(it) },
                    sedType = sedType
                )
            }
            else -> PensjonCollection(sedType = prefillData.sedType)
        }
    }

    fun sjekkSakTypeKravSed(sak: P2xxxMeldingOmPensjonDto?, akseptabelSakstypeForSed: EessiSakType) {
        val sakType = sak?.sak?.sakType

        if (sakType != akseptabelSakstypeForSed) {
            logger.warn("Du kan ikke opprette SED med saktype $sakType")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ugyldig saktype: $sakType")
        }
    }

    private fun ResponseStatusExceptionFeilSak(
        prefillData: PrefillDataModel,
        sakType: EessiSakType?
    ): ResponseStatusException = ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Du kan ikke opprette en ${prefillData.sedType} med saktype ${sakType}. (PESYS-saksnr: ${prefillData.penSaksnummer} har sakstype ${sakType})"
    )

    private fun validateInputs(vararg inputs: Pair<String?, String>) {
        inputs.forEach { (value, name) ->
            if (value.isNullOrBlank()) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Mangler $name")
            }
        }
    }

    fun validerVedtak(prefillData: PrefillDataModel): String {
        val vedtakId = prefillData.vedtakId
        if (vedtakId.isNullOrEmpty()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Mangler vedtakID")
        else return vedtakId
    }

}