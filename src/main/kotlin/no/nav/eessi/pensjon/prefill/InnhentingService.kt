package no.nav.eessi.pensjon.prefill

import no.nav.eessi.pensjon.eux.model.BucType.*
import no.nav.eessi.pensjon.eux.model.SedType.*
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.personoppslag.pdl.model.Ident
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentGruppe
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType
import no.nav.eessi.pensjon.shared.api.ApiRequest
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.utils.toJson
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class InnhentingService(
    private val personDataService: PersonDataService,
    @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper.ForTest(),
    private val pesysService: PesysService,
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
                    logger.error("Mangler fnr eller npid for avdød")
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Mangler fnr for avdød")
                }
                return try {
                    hentIdent(Ident.bestemIdent(norskIdent))
                } catch (ex: Exception) {
                    throw ResponseStatusException(HttpStatus.NOT_FOUND, "Korrekt aktoerIdent ikke funnet")
                }
            }
            P_BUC_05, P_BUC_06,P_BUC_10 -> {
                val norskIdent = request.riktigAvdod() ?: return null
                return try {
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
        val eessipensjonSakTyper = listOf(
            EessiSakType.ALDER,
            EessiSakType.BARNEP, EessiSakType.GJENLEV, EessiSakType.UFOREP
        )
        val pensakTyper = listOf(EessiSakType.GENRL, EessiSakType.OMSORG) + eessipensjonSakTyper
        return when (val sedType = prefillData.sedType) {

            P2000 -> {
                val p2000data = prefillData.vedtakId?.let { pesysService.hentP2000data(prefillData.vedtakId) }
                if (p2000data?.sak?.sakType != EessiSakType.ALDER) {
                    throw ResponseStatusExceptionFeilSak(prefillData, p2000data?.sak?.sakType)
                }
                PensjonCollection(
                    p2xxxMeldingOmPensjonDto = p2000data.takeIf { p2000data.sak.sakType == EessiSakType.ALDER } ,
                    vedtakId = prefillData.vedtakId,
                    sedType = sedType
                )
            }
            P2100 -> {
                val p2100data = prefillData.vedtakId?.let { pesysService.hentP2100data(prefillData.vedtakId) }
                if (p2100data?.sak?.sakType !in eessipensjonSakTyper) {
                    throw ResponseStatusExceptionFeilSak(prefillData, p2100data?.sak?.sakType)
                }
                PensjonCollection(
                    p2xxxMeldingOmPensjonDto = p2100data.takeIf { p2100data?.sak?.sakType in eessipensjonSakTyper } ,
                    vedtakId = prefillData.vedtakId,
                    sedType = sedType
                )
            }
            P2200 -> {
                val p2200data = prefillData.vedtakId?.let { pesysService.hentP2200data(prefillData.vedtakId) }
                if (p2200data?.sak?.sakType != EessiSakType.UFOREP) {
                    throw ResponseStatusExceptionFeilSak(prefillData, p2200data?.sak?.sakType)
                }
                PensjonCollection(
                    p2xxxMeldingOmPensjonDto = p2200data.takeIf { p2200data.sak.sakType == EessiSakType.UFOREP } ,
                    vedtakId = prefillData.vedtakId,
                    sedType = sedType
                )
            }
            P6000 -> {
                validerVedtak(prefillData)
                return PensjonCollection(
                    p6000Data = prefillData.vedtakId?.let { pesysService.hentP6000data(it) },
                    vedtakId = prefillData.vedtakId,
                    sedType = sedType
                ).also { logger.debug("Svar fra Pesys nytt endepunkt: ${it.toJson()}")}
            }
            P8000 -> {
                if (prefillData.buc == P_BUC_05) {
                        try {
                            val p8000 = prefillData.vedtakId?.let { pesysService.hentP8000data(it) }
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
                    p15000Data = prefillData.vedtakId?.let { pesysService.hentP15000data(it) },
                    sedType = sedType
                )
            }
            else -> PensjonCollection(sedType = prefillData.sedType)
        }
    }

    private fun ResponseStatusExceptionFeilSak(
        prefillData: PrefillDataModel,
        sakType: EessiSakType?
    ): ResponseStatusException = ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Du kan ikke opprette en ${prefillData.sedType} med saktype ${sakType}. (PESYS-saksnr: ${prefillData.penSaksnummer} har sakstype ${sakType})"
    )

    fun validerVedtak(prefillData: PrefillDataModel): String {
        val vedtakId = prefillData.vedtakId
        vedtakId?.let { return it }

        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Mangler vedtakID")
    }

//    fun hentrelevantPensjonSak(penData: P2xxxMeldingOmPensjonDto, sakTypeIsed: EessiSakType) {
//
//        if (penData?.sak?. .isNullOrBlank()) throw ManglendeSakIdException("Mangler sakId")
//        if (fnr.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Mangler norskident")
//
//    }

//    fun hentRelevantPensjonSak(prefillData: PrefillDataModel, akseptabelSakstypeForSed: (String) -> Boolean): P2xxxMeldingOmPensjonDto.Sak? {
//        logger.debug("sakNr er: ${prefillData.penSaksnummer} aktoerId er: ${prefillData.bruker.aktorId} prøver å hente Sak")
//        return pensjonsinformasjonService.hentRelevantPensjonSak(prefillData, akseptabelSakstypeForSed)
//    }

//    private fun hentRelevantVedtak(prefillData: PrefillDataModel): Vedtak? {
//        prefillData.vedtakId.let {
//            logger.debug("vedtakId er: $it, prøver å hente vedtaket")
//            return pensjonsinformasjonService.hentRelevantVedtakHvisFunnet(it ?: "")
//        }
//    }

//    private fun hentRelevantPensjonsinformasjon(prefillData: PrefillDataModel): Pensjonsinformasjon? {
//        return prefillData.vedtakId?.let {
//            logger.debug("vedtakid er: $it, prøver å hente pensjonsinformasjon for vedtaket")
//            pensjonsinformasjonService.hentMedVedtak(it)
//        }
//    }

}