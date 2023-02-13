package no.nav.eessi.pensjon.prefill

import jakarta.annotation.PostConstruct
import no.nav.eessi.pensjon.eux.model.BucType.*
import no.nav.eessi.pensjon.eux.model.SedType.*
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.pensjonsinformasjon.models.EPSaktype
import no.nav.eessi.pensjon.pensjonsinformasjon.models.EPSaktype.*
import no.nav.eessi.pensjon.personoppslag.Fodselsnummer
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentType
import no.nav.eessi.pensjon.personoppslag.pdl.model.NorskIdent
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.shared.api.ApiRequest
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
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
    private val pensjonsinformasjonService: PensjonsinformasjonService
) {

    private lateinit var HentPerson: MetricsHelper.Metric
    private lateinit var addInstutionAndDocumentBucUtils: MetricsHelper.Metric

    private val logger = LoggerFactory.getLogger(InnhentingService::class.java)


    @PostConstruct
    fun initMetrics() {
        HentPerson = metricsHelper.init("HentPerson", ignoreHttpCodes = listOf(HttpStatus.BAD_REQUEST))
        addInstutionAndDocumentBucUtils = metricsHelper.init(
            "AddInstutionAndDocumentBucUtils",
            ignoreHttpCodes = listOf(HttpStatus.BAD_REQUEST)
        )
    }

    //Hjelpe funksjon for å validere og hente aktoerid for evt. avdodfnr fra UI (P2100) - PDL
    fun getAvdodAktoerIdPDL(request: ApiRequest): String? {
        val buc = request.buc ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Mangler Buc")
        return when (buc) {
            P_BUC_02 -> {
                val norskIdent = request.riktigAvdod() ?: run {
                    logger.error("Mangler fnr for avdød")
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Mangler fnr for avdød")
                }
                if (norskIdent.isBlank()) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ident har tom input-verdi")
                }
                hentIdent(IdentType.AktoerId, NorskIdent(norskIdent))
            }
            P_BUC_05, P_BUC_06,P_BUC_10 -> {
                val norskIdent = request.riktigAvdod() ?: return null
                if (norskIdent.isBlank()) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ident har tom input-verdi")
                }
                val gyldigNorskIdent = Fodselsnummer.fra(norskIdent)
                return try {
                    hentIdent(IdentType.AktoerId, NorskIdent(norskIdent))
                } catch (ex: Exception) {
                    if (gyldigNorskIdent == null) logger.error("NorskIdent er ikke gyldig")
                    throw ResponseStatusException(HttpStatus.NOT_FOUND, "Korrekt aktoerIdent ikke funnet")
                }
            }
            else -> null
        }
    }

    fun hentPersonData(prefillData: PrefillDataModel) : PersonDataCollection = personDataService.hentPersonData(prefillData)

    fun hentFnrfraAktoerService(aktoerid: String?): String = personDataService.hentFnrfraAktoerService(aktoerid)

    fun hentIdent(aktoerId: IdentType.AktoerId, norskIdent: NorskIdent): String = personDataService.hentIdent(aktoerId, norskIdent).id

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
            P6000 ->  PensjonCollection(pensjoninformasjon = pensjonsinformasjonService.hentVedtak(hentVedtak(prefillData)), sedType = sedType)
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