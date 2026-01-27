package no.nav.eessi.pensjon.prefill

import no.nav.eessi.pensjon.eux.model.BucType.*
import no.nav.eessi.pensjon.eux.model.SedType.*
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.personoppslag.pdl.model.Ident
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentGruppe
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.SakInterface
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiSakType
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiSakType.*
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P6000MeldingOmVedtakDto.Vedtak
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
        val eessipensjonSakTyper = listOf(ALDER, BARNEP, GJENLEV, UFOREP)
        val pensakTyper = listOf(GENRL, OMSORG) + eessipensjonSakTyper
        return when (val sedType = prefillData.sedType) {

            P2000 -> {
                val p2000data = pesysService.hentP2000data(prefillData.vedtakId!!)
                PensjonCollection(
                    sak = p2000data?.sak,
                    vedtak = p2000data?.vedtak,
                    sedType = sedType
                )
            }
            P2200 -> {
                val p2200data = pesysService.hentP2200data(prefillData.vedtakId!!)
                PensjonCollection(
                    sak = p2200data?.sak,
                    vedtak = p2200data?.vedtak,
                    sedType = sedType
                )
            }
            P2100 -> {
                val p2100data = pesysService.hentP2100data(prefillData.vedtakId!!)
                PensjonCollection(
                    sak = p2100data?.sak,
                    vedtak = p2100data?.vedtak,
                    sedType = sedType
                )
            }
            P6000 -> PensjonCollection(
                p6000Data = prefillData.vedtakId?.let { pesysService.hentP6000data(it) },
                sedType = sedType
            ).also { logger.debug("Svar fra Pesys nytt endepunkt: ${it.toJson()}")}
            P8000 -> {
                if (prefillData.buc == P_BUC_05) {
                        try {
                            PensjonCollection(
                                p8000Data = prefillData.vedtakId?.let { pesysService.hentP8000data(it) },
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

    fun hentVedtak(prefillData: PrefillDataModel): String {
        val vedtakId = prefillData.vedtakId
        vedtakId?.let { return it }

        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Mangler vedtakID")
    }

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