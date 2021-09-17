package no.nav.eessi.pensjon.prefill

import com.github.wnameless.json.flattener.JsonFlattener
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import net.minidev.json.JSONObject
import net.minidev.json.parser.JSONParser
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.eux.model.sed.SedType
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.prefill.models.InstitusjonItem
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModel
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.prefill.sed.krav.ValidationException
import no.nav.eessi.pensjon.utils.eessiRequire
import no.nav.eessi.pensjon.utils.toJson
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class PrefillService(
    private val prefillSedService: PrefillSEDService,
    private val innhentingService: InnhentingService,
    @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper(SimpleMeterRegistry())
) {

    private val logger = LoggerFactory.getLogger(PrefillService::class.java)

    private lateinit var PrefillSed: MetricsHelper.Metric

    private val LATEST_RINA_SED_VERSION = "v4.2"

    @PostConstruct
    fun initMetrics() {
        PrefillSed = metricsHelper.init("PrefillSed",
            ignoreHttpCodes = listOf(HttpStatus.BAD_REQUEST, HttpStatus.FORBIDDEN))
    }

    fun prefillSedtoJson(request: ApiRequest): String {
        return PrefillSed.measure {
            logger.info(" ******* Starter med preutfylling ******* ")
            try {
                val norskIdent = innhentingService.hentFnrfraAktoerService(request.aktoerId)
                val prefillData = ApiRequest.buildPrefillDataModelOnExisting(request, norskIdent, innhentingService.getAvdodAktoerIdPDL(request))

                eessiRequire(prefillData.sedType.kanPrefilles() ) {"SedType ${prefillData.sedType} kan ikke prefilles!"}

                val personcollection = innhentingService.hentPersonData(prefillData)
                val pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)

                val sed = prefillSedService.prefill(prefillData, personcollection, pensjonCollection)

                logger.debug("SedType: ${sed.type.name}")

                //synk sed versjon med buc versjon
                updateSEDVersion(sed, LATEST_RINA_SED_VERSION)

                try {
                    Metrics.counter("Sed_Prefill","type", sed.type.name).increment()
                } catch (e: Exception) {
                    logger.warn("Metrics feilet på Sed_Prefill")
                }

                flattenAndCount(prefillData, sed.toJson())

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

    private fun flattenAndCount(data: PrefillDataModel, json: String) {
        val doprint = false
        val parser = JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE)

        val obj = parser.parse(json)
        val jsonObject = obj as JSONObject
        val flattenedJson = JsonFlattener.flatten(jsonObject.toString())
        val flattenedJsonMap = JsonFlattener.flattenAsMap(jsonObject.toString())

        if (doprint) {
            logger.debug("\n=====Flatten As Map=====\n$flattenedJson")
            flattenedJsonMap.forEach { (k: String, v: Any) -> logger.debug("$k : $v") }
            logger.debug("==".repeat(80))
        }

        val listOfValues = flattenedJsonMap.filter { it.value != null }
        val listOfEmptyValues = flattenedJsonMap.filter { it.value == null }

        val total = flattenedJsonMap.size
        val prefylt = listOfValues.size
        val tomme = listOfEmptyValues.size
        val prosentprefylt = ((prefylt.toDouble() / total.toDouble()) * 100).toInt()


        logger.info("Buctype: ${data.buc}, SedType: ${data.sedType}, antall utfylt felt: $prefylt, prosent utfylt: $prosentprefylt, antall tomme felt: $tomme, Total: $total")
        logger.debug("==".repeat(80))
    }

    /**
     * Prefill for X005 - Legg til ny institusjon
     */
    @Throws(ValidationException::class)
    fun prefillEnX005ForHverInstitusjon(
        nyeDeltakere: List<InstitusjonItem>,
        data: PrefillDataModel,
        personcollection: PersonDataCollection
    ) =
            nyeDeltakere.map {
                logger.debug("Legger til Institusjon på X005 ${it.institution}")
                // ID og Navn på X005 er påkrevd må hente innn navn fra UI.
                val datax005 = data.copy(avdod = null, sedType = SedType.X005, institution = listOf(it))

                prefillSedService.prefill(datax005, personcollection, PensjonCollection(sedType = SedType.X005))
            }
}