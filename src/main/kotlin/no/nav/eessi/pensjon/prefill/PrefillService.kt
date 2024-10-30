package no.nav.eessi.pensjon.prefill

import io.micrometer.core.instrument.Metrics
import no.nav.eessi.pensjon.eux.model.sed.SED.Companion.setSEDVersion
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.shared.api.ApiRequest
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.statistikk.AutomatiseringStatistikkService
import no.nav.eessi.pensjon.utils.eessiRequire
import no.nav.eessi.pensjon.utils.toJson
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException

@Service
class PrefillService(
    private val krrService: KrrService,
    private val prefillSedService: PrefillSEDService,
    private val innhentingService: InnhentingService,
    private val automatiseringStatistikkService: AutomatiseringStatistikkService,
    @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper.ForTest()
) {
    private val logger = LoggerFactory.getLogger(PrefillService::class.java)
    private lateinit var PrefillSed: MetricsHelper.Metric

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
                val krrPerson = krrService.hentPersonFraKrr(norskIdent)

                val personInfo = if (krrPerson.reservert == true) {
                    PersonInfo(norskIdent, request.aktoerId).also { logger.info("Personen har reservert seg mot digital kommunikasjon")}
                } else {
                    PersonInfo(norskIdent, request.aktoerId, krrPerson.reservert, krrPerson.epostadresse, krrPerson.mobiltelefonnummer)
                        .also { logger.info("Hentet telefon og epost fra KRR: ${krrPerson.toJson()}") }
                }

                val prefillData = ApiRequest.buildPrefillDataModelOnExisting(request, personInfo, innhentingService.getAvdodAktoerIdPDL(request))

                eessiRequire(prefillData.sedType.kanPrefilles() ) {"SedType ${prefillData.sedType} kan ikke prefilles!"}

                val personcollection = innhentingService.hentPersonData(prefillData)

                //TODO: midlertidig løsning
                val sed = if(request.gjenny){
                    logger.info("Begynner preutfylling for gjenny")
                    prefillSedService.prefill(prefillData, personcollection)
                }
                else {
                    val pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)
                    prefillSedService.prefill(prefillData, personcollection, pensjonCollection)
                }

                logger.debug("Sed ferdig utfylt: $sed")

                //synk sed versjon med buc versjon
                setSEDVersion(sed.sedVer)

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

}