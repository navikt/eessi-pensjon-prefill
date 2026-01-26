package no.nav.eessi.pensjon.prefill

import no.nav.eessi.pensjon.eux.model.BucType.*
import no.nav.eessi.pensjon.eux.model.SedType.*
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.pensjonsinformasjon.models.EPSaktype
import no.nav.eessi.pensjon.pensjonsinformasjon.models.EPSaktype.*
import no.nav.eessi.pensjon.personoppslag.pdl.model.Ident
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentGruppe
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.shared.api.ApiRequest
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.utils.toJson
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.sak.V1Sak
import no.nav.pensjon.v1.vedtak.V1Vedtak
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class InnhentingService(
    private val personDataService: PersonDataService,
    @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper.ForTest(),
    private val pensjonsinformasjonService: PensjonsinformasjonService,
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
                PensjonCollection(sak = hentRelevantPensjonSak(prefillData) { pensakType -> EPSaktype.valueOf(pensakType) == ALDER }, vedtak = hentRelevantVedtak(prefillData), sedType = sedType)
            }
            P2200 -> {
                PensjonCollection(sak = hentRelevantPensjonSak(prefillData) { pensakType -> EPSaktype.valueOf(pensakType) == UFOREP }, vedtak = hentRelevantVedtak(prefillData), sedType = sedType)
            }
            P2100 -> {
                PensjonCollection(
                    sak = hentRelevantPensjonSak(prefillData) { pensakType -> (EPSaktype.valueOf(pensakType) in eessipensjonSakTyper) },
                    vedtak = hentRelevantVedtak(prefillData),
                    sedType = sedType
                )
            }
            P6000 -> PensjonCollection(
                p6000Data = prefillData.vedtakId?.let { pesysService.hentP6000data(it) },
                sedType = sedType
            ).also { logger.debug("Svar fra Pesys nytt endepunkt: ${it.toJson()} Svar fra gammel løsning: ${PensjonCollection(pensjoninformasjon = pensjonsinformasjonService.hentVedtak(hentVedtak(prefillData)), sedType = sedType).toJson()}")}
            P8000 -> {
                if (prefillData.buc == P_BUC_05) {
                        try {
                            val sak = hentRelevantPensjonSak(prefillData) { pensakType -> EPSaktype.valueOf(pensakType) in pensakTyper }
                            PensjonCollection(sak = sak , sedType = sedType)
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
                    pensjoninformasjon = hentRelevantPensjonsinformasjon(prefillData),
                    sak = hentRelevantPensjonSak(prefillData) { pensakType -> EPSaktype.valueOf(pensakType) in pensakTyper },
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

    fun hentRelevantPensjonSak(prefillData: PrefillDataModel, akseptabelSakstypeForSed: (String) -> Boolean): V1Sak? {
        logger.debug("sakNr er: ${prefillData.penSaksnummer} aktoerId er: ${prefillData.bruker.aktorId} prøver å hente Sak")
        return pensjonsinformasjonService.hentRelevantPensjonSak(prefillData, akseptabelSakstypeForSed)
    }

    private fun hentRelevantVedtak(prefillData: PrefillDataModel): V1Vedtak? {
        prefillData.vedtakId.let {
            logger.debug("vedtakId er: $it, prøver å hente vedtaket")
            return pensjonsinformasjonService.hentRelevantVedtakHvisFunnet(it ?: "")
        }
    }

    private fun hentRelevantPensjonsinformasjon(prefillData: PrefillDataModel): Pensjonsinformasjon? {
        return prefillData.vedtakId?.let {
            logger.debug("vedtakid er: $it, prøver å hente pensjonsinformasjon for vedtaket")
            pensjonsinformasjonService.hentMedVedtak(it)
        }
    }

}