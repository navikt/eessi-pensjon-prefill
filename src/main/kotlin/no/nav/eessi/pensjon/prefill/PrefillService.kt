package no.nav.eessi.pensjon.prefill

import io.micrometer.core.instrument.Metrics
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.shared.api.ApiRequest
import no.nav.eessi.pensjon.statistikk.AutomatiseringStatistikkService
import no.nav.eessi.pensjon.utils.eessiRequire
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException

@Service
class PrefillService(
    private val prefillSedService: PrefillSEDService,
    private val innhentingService: InnhentingService,
    private val automatiseringStatistikkService: AutomatiseringStatistikkService,
    @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper.ForTest()
) {

    private val logger = LoggerFactory.getLogger(PrefillService::class.java)

    private lateinit var PrefillSed: MetricsHelper.Metric

    private val LATEST_RINA_SED_VERSION = "v4.2"

    init {
        PrefillSed = metricsHelper.init("PrefillSed",
            ignoreHttpCodes = listOf(HttpStatus.BAD_REQUEST, HttpStatus.FORBIDDEN))
    }

    fun prefillSedtoJson(request: ApiRequest): String {
        return PrefillSed.measure {
            logger.info(" ******* Starter med preutfylling ******* ")
            if (request.aktoerId.isNullOrEmpty()) throw HttpClientErrorException(HttpStatus.NOT_FOUND)

            try {
                val norskIdent = innhentingService.hentFnrEllerNpidFraAktoerService(request.aktoerId)!!
                val prefillData = ApiRequest.buildPrefillDataModelOnExisting(request, norskIdent, innhentingService.getAvdodAktoerIdPDL(request))

                eessiRequire(prefillData.sedType.kanPrefilles() ) {"SedType ${prefillData.sedType} kan ikke prefilles!"}

                val personcollection = innhentingService.hentPersonData(prefillData)

                //TODO: midlertidig løsning
                val sed = if(request.gjenny && request.sed == SedType.P6000){
                    prefillSedService.prefill(prefillData, personcollection)
                }
                else {
                    val pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)
                    prefillSedService.prefill(prefillData, personcollection, pensjonCollection)
                }

                logger.debug("Sed ferdig utfylt: $sed")

                //synk sed versjon med buc versjon
                updateSEDVersion(sed, LATEST_RINA_SED_VERSION)

                try {
                    Metrics.counter("Sed_Prefill","type", sed.type.name).increment()
                } catch (e: Exception) {
                    logger.warn("Metrics feilet på Sed_Prefill")
                }

                automatiseringStatistikkService.genererAutomatiseringStatistikk(sed,  prefillData.buc)

                logger.info(" ******* Prefutfylling ferdig ******* ")

                return@measure sed.toJsonSkipEmpty()

            } catch (ex: Exception) {
                logger.error("Noe gikk galt under prefill: ", ex)
                throw ex
            }
        }
    }

    //flyttes til prefill / en eller annen service?
    private fun updateSEDVersion(sed: SED, bucVersion: String) {
        when(bucVersion) {
            "v4.2" -> sed.sedVer="2"
            else -> sed.sedVer="1"
        }
        logger.debug("SED version: v${sed.sedGVer}.${sed.sedVer} + BUC version: $bucVersion")
    }
}