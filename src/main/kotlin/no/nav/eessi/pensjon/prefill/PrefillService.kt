package no.nav.eessi.pensjon.prefill

import io.micrometer.core.instrument.Metrics
import no.nav.eessi.pensjon.eux.model.sed.SED.Companion.setSEDVersion
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.prefill.etterlatte.EtterlatteService
import no.nav.eessi.pensjon.prefill.etterlatte.EtterlatteVedtakResponseData
import no.nav.eessi.pensjon.prefill.models.DigitalKontaktinfo
import no.nav.eessi.pensjon.prefill.models.DigitalKontaktinfo.Companion.validateEmail
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.shared.api.ApiRequest
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
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
    private val etterlatteService: EtterlatteService,
    private val automatiseringStatistikkService: AutomatiseringStatistikkService,
    private val prefillPdlNav: PrefillPDLNav,
    @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper.ForTest()
) {
    private val logger = LoggerFactory.getLogger(PrefillService::class.java)
    private val secureLog = LoggerFactory.getLogger("secureLog")
    private var PrefillSed: MetricsHelper.Metric

    init {
        PrefillSed = metricsHelper.init("PrefillSed",
            ignoreHttpCodes = listOf(HttpStatus.BAD_REQUEST, HttpStatus.FORBIDDEN))
    }

    fun prefillSedtoJson(request: ApiRequest): String {
        return PrefillSed.measure {
            logger.info(" ******* Starter med preutfylling ******* ")
            if (request.aktoerId.isNullOrEmpty()) throw HttpClientErrorException(HttpStatus.NOT_FOUND)

            try {
                logger.info(" Buc:${request.buc}, sed: ${request.sed}, RinaID:${request.euxCaseId}, sed: ${request.documentid}, versjon: ${request.processDefinitionVersion}, gjenny: ${request.gjenny}")
                val norskIdent = innhentingService.hentFnrEllerNpidFraAktoerService(request.aktoerId)!!
                val personInfo = hentKrrPerson(norskIdent, request)

                val prefillData = ApiRequest.buildPrefillDataModelOnExisting(request, personInfo, innhentingService.getAvdodAktoerIdPDL(request))

                eessiRequire(prefillData.sedType.kanPrefilles() ) {"SedType ${prefillData.sedType} kan ikke prefilles!"}

                val personcollection = innhentingService.hentPersonData(prefillData)

                //TODO: midlertidig løsning
                val sed = if(request.gjenny){
                    logger.info("Begynner preutfylling for gjenny")
                    prefillSedService.prefillGjenny(prefillData, personcollection,
                        listeOverVedtak(prefillData, personcollection))
                }
                else {
                    val pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)
                    secureLog.info("PensjonCollection: ${pensjonCollection.toJson()}")
                    prefillSedService.prefill(prefillData, personcollection, pensjonCollection, null,)
                }

                secureLog.info("Sed ferdig utfylt: $sed")

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

    private fun listeOverVedtak(prefillData: PrefillDataModel, personDataCollection: PersonDataCollection): EtterlatteVedtakResponseData? {
        val gjenlevende = prefillData.avdod?.let {
            prefillPdlNav.createGjenlevende(personDataCollection.forsikretPerson, prefillData.bruker)
        }

        val identifikator = gjenlevende?.person?.pin?.firstOrNull()?.identifikator ?: return null
        val resultatEtterlatteRespData = etterlatteService.hentGjennyVedtak(identifikator)

        if (resultatEtterlatteRespData.isFailure) {
            logger.error(resultatEtterlatteRespData.exceptionOrNull()?.message)
        }
        return resultatEtterlatteRespData.getOrNull()
    }

    private fun hentKrrPerson(norskIdent: String, request: ApiRequest): PersonInfo {
        val krrPerson = krrService.hentPersonerFraKrr(norskIdent)?.let { personResponse ->
            DigitalKontaktinfo(
                reservert = personResponse.reservert,
                epostadresse = personResponse.epostadresse.validateEmail(request.processDefinitionVersion),
                mobiltelefonnummer = personResponse.mobiltelefonnummer,
                aktiv = true,
                personident = norskIdent
            ).also { logger.debug("KrrPerson: ${it.toJson()}") }
        } ?: DigitalKontaktinfo(
            reservert = false,
            epostadresse = null,
            mobiltelefonnummer = null,
            aktiv = true,
            personident = norskIdent
        )

        val personInfo = if (krrPerson.reservert == true) {
            PersonInfo(
                norskIdent,
                request.aktoerId
            ).also { logger.info("Personen har reservert seg mot digital kommunikasjon") }
        } else {
            PersonInfo(
                norskIdent = norskIdent,
                aktorId = request.aktoerId,
                reservert = krrPerson.reservert,
                epostKrr = krrPerson.epostadresse,
                telefonKrr = krrPerson.mobiltelefonnummer
            ).also { secureLog.info("Hentet telefon og epost fra KRR: ${krrPerson.toJson()}") }
        }
        return personInfo
    }

}