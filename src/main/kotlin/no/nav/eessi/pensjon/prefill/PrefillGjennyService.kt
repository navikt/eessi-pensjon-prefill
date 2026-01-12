package no.nav.eessi.pensjon.prefill

import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.SedType.*
import no.nav.eessi.pensjon.eux.model.sed.Bruker
import no.nav.eessi.pensjon.eux.model.sed.P5000
import no.nav.eessi.pensjon.eux.model.sed.P5000Pensjon
import no.nav.eessi.pensjon.eux.model.sed.Pensjon
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.eux.model.sed.SED.Companion.setSEDVersion
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.prefill.etterlatte.EtterlatteService
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
class PrefillGjennyService(
    private val krrService: KrrService,
    private val innhentingService: InnhentingService,
    private val etterlatteService: EtterlatteService,
    private val automatiseringStatistikkService: AutomatiseringStatistikkService,
    private val prefillPdlNav: PrefillPDLNav,
    @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper.ForTest()
) {
    private val logger = LoggerFactory.getLogger(PrefillGjennyService::class.java)
    private val secureLog = LoggerFactory.getLogger("secureLog")
    private var PrefillSed: MetricsHelper.Metric

    init {
        PrefillSed = metricsHelper.init("PrefillSed",
            ignoreHttpCodes = listOf(HttpStatus.BAD_REQUEST, HttpStatus.FORBIDDEN))
    }

    fun prefillGjennySedtoJson(request: ApiRequest): String {
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

                logger.info("Begynner preutfylling for gjenny")
                val sed = when(prefillData.sedType) {
                    P2100 -> throw HttpClientErrorException(HttpStatus.NOT_IMPLEMENTED, "Prefilling for gjenny av sed type P2100 er ikke implementert")
                    P4000 -> throw HttpClientErrorException(HttpStatus.NOT_IMPLEMENTED, "Prefilling for gjenny av sed type P2100 er ikke implementert")
                    P5000 -> prefillP5000(prefillData, personcollection).also { logger.info("Preutfyll P5000: ") }
                    P6000 -> throw HttpClientErrorException(HttpStatus.NOT_IMPLEMENTED, "Prefilling for gjenny av sed type P2100 er ikke implementert")
                    P7000 -> throw HttpClientErrorException(HttpStatus.NOT_IMPLEMENTED, "Prefilling for gjenny av sed type P2100 er ikke implementert")
                    P8000 -> throw HttpClientErrorException(HttpStatus.NOT_IMPLEMENTED, "Prefilling for gjenny av sed type P2100 er ikke implementert")
                    P9000 -> throw HttpClientErrorException(HttpStatus.NOT_IMPLEMENTED, "Prefilling for gjenny av sed type P2100 er ikke implementert")
                    P10000 -> throw HttpClientErrorException(HttpStatus.NOT_IMPLEMENTED, "Prefilling for gjenny av sed type P2100 er ikke implementert")
                    else -> {
                        throw HttpClientErrorException(HttpStatus.NOT_IMPLEMENTED, "Prefilling for gjenny av sed type ${prefillData.sedType} er ikke implementert")
                    }
                }.also { secureLog.info("PrefillGjennySedtoJson: ${it.toJson()}") }

                setSEDVersion(sed.sedVer)
                automatiseringStatistikkService.genererAutomatiseringStatistikk(sed,  prefillData.buc)

                logger.info(" ******* Prefutfylling ferdig ******* ")

                return@measure sed.toJsonSkipEmpty()

            } catch (ex: Exception) {
                logger.error("Noe gikk galt under prefill: ", ex)
                throw ex
            }
        }
    }
    fun prefillP5000(prefillData: PrefillDataModel, personData: PersonDataCollection): SED {
        logger.debug("----------------------------------------------------------")
        logger.debug("Preutfylling NAV     : ${prefillPdlNav::class.java} ")
        logger.debug("------------------| Preutfylling START |------------------ ")
        logger.debug("[${prefillData.sedType}] Preutfylling Utfylling Data")

        val sedType = prefillData.sedType

        val prefillPensjon = try {
            val pensjon = if(prefillData.avdod != null) {
                 prefillData.avdod.let {
                    logger.info("Preutfylling Utfylling Pensjon Avdod (etterlatt)")
                    val gjenlevendePerson = prefillPdlNav.createBruker(personData.forsikretPerson!!, null, null, prefillData.bruker)
                    Pensjon(gjenlevende = gjenlevendePerson)
                }
            }
            else {
                logger.info("Preutfylling Utfylling Pensjon Gjenlevende (etterlatt)")
                val gjenlevendePerson = prefillPdlNav.createBruker(personData.gjenlevendeEllerAvdod!!, null, null, prefillData.bruker)
                Pensjon(gjenlevende = gjenlevendePerson)
            }
            logger.debug("[${prefillData.sedType}] Preutfylling Utfylling Pensjon")
            pensjon
        } catch (ex: Exception) {
            logger.error(ex.message, ex)
            Pensjon()
        }

        //henter opp persondata
        val navSed = prefillPdlNav.prefill(
            penSaksnummer = prefillData.penSaksnummer,
            bruker = prefillData.bruker,
            avdod = prefillData.avdod,
            personData = personData,
            bankOgArbeid = prefillData.getBankOgArbeidFromRequest(),
            krav = prefillPensjon.kravDato,
            annenPerson = annenPersonHvisGjenlevende(prefillData, prefillPensjon.gjenlevende)
        )
        logger.debug("[${prefillData.sedType}] Preutfylling Utfylling NAV")

        logger.debug("-------------------| Preutfylling END |------------------- ")
        val sed = SED(sedType, nav = navSed, pensjon = prefillPensjon)

        return P5000(
            nav = sed.nav,
            pensjon = P5000Pensjon(
                gjenlevende = sed.pensjon?.gjenlevende
            )
        )

    }
    private fun annenPersonHvisGjenlevende(prefillData: PrefillDataModel, gjenlevende: Bruker?): Bruker? {
        return if (prefillData.avdod != null) {
            gjenlevende?.person?.rolle = "01"  //Claimant - etterlatte
            gjenlevende
        } else null
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