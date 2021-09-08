package no.nav.eessi.pensjon.prefill

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.eessi.pensjon.fagmodul.models.PensjonCollection
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentType
import no.nav.eessi.pensjon.personoppslag.pdl.model.NorskIdent
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModel
import no.nav.eessi.pensjon.utils.Fodselsnummer
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.sak.V1Sak
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import javax.annotation.PostConstruct

@Service
class InnhentingService(
    private val personDataService: PersonDataService,
    @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper(SimpleMeterRegistry()),
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
        val buc = request.buc ?: throw MangelfulleInndataException("Mangler Buc")
        return when (buc) {
            "P_BUC_02" -> {
                val norskIdent = request.riktigAvdod() ?: run {
                    logger.error("Mangler fnr for avdød")
                    throw MangelfulleInndataException("Mangler fnr for avdød")
                }
                if (norskIdent.isBlank()) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ident har tom input-verdi")
                }
                hentIdent(IdentType.AktoerId, NorskIdent(norskIdent))
            }
            "P_BUC_05", "P_BUC_06", "P_BUC_10" -> {
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

    fun hentPensjoninformasjon(prefillData: PrefillDataModel): PensjonCollection {
        return PensjonCollection(
            vedtak = hentVedtak(prefillData),
            sak = hentSak(prefillData)
        )
    }

    fun hentVedtak(prefillData: PrefillDataModel): Pensjonsinformasjon? {
        return try { prefillData.vedtakId?.let { vedtakid -> pensjonsinformasjonService.hentVedtak(vedtakid) }
            } catch (ex: Exception) {
                logger.error("Error henting av vedtak", ex)
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Mangler vedtakID")
            }
    }

    fun hentSak(prefillData: PrefillDataModel): V1Sak? {
        val aktorId = prefillData.bruker.aktorId
        val penSaksnummer = prefillData.penSaksnummer

        val pensjoninformasjon = try {
            pensjonsinformasjonService.hentPensjonInformasjon(aktorId)
        } catch (ex: Exception) {
            logger.error("Error henting av pensjoninformasjon aktoerid", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ingen pensjoninformasjon funnet")
        }

        logger.info("Søker brukersSakerListe etter sakId: $penSaksnummer")
        val v1saklist = pensjoninformasjon.brukersSakerListe.brukersSakerListe
        return v1saklist.firstOrNull { sak -> "${sak.sakId}" == penSaksnummer  }
    }





}