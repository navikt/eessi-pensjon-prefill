package no.nav.eessi.pensjon.prefill

import no.nav.eessi.pensjon.eux.model.SedType.*
import no.nav.eessi.pensjon.eux.model.sed.Adresse
import no.nav.eessi.pensjon.eux.model.sed.AnmodningOmTilleggsInfo
import no.nav.eessi.pensjon.eux.model.sed.Bruker
import no.nav.eessi.pensjon.eux.model.sed.EessisakItem
import no.nav.eessi.pensjon.eux.model.sed.Nav
import no.nav.eessi.pensjon.eux.model.sed.P5000
import no.nav.eessi.pensjon.eux.model.sed.P5000Pensjon
import no.nav.eessi.pensjon.eux.model.sed.P6000
import no.nav.eessi.pensjon.eux.model.sed.P8000
import no.nav.eessi.pensjon.eux.model.sed.P8000Pensjon
import no.nav.eessi.pensjon.eux.model.sed.Pensjon
import no.nav.eessi.pensjon.eux.model.sed.Person
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.eux.model.sed.SED.Companion.setSEDVersion
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.prefill.etterlatte.EtterlatteService
import no.nav.eessi.pensjon.prefill.etterlatte.EtterlatteVedtakResponseData
import no.nav.eessi.pensjon.prefill.models.DigitalKontaktinfo
import no.nav.eessi.pensjon.prefill.models.DigitalKontaktinfo.Companion.validateEmail
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.sed.PrefillP8000.PersonenRolle
import no.nav.eessi.pensjon.prefill.sed.krav.PrefillP2100
import no.nav.eessi.pensjon.prefill.sed.vedtak.PrefillP6000GjennyPensjon
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
    private val eessiInformasjon: EessiInformasjon,
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

                logger.debug("----------------------------------------------------------")
                logger.debug("Preutfylling NAV     : ${prefillPdlNav::class.java} ")
                logger.debug("------------------| Preutfylling START |------------------ ")
                logger.debug("[${prefillData.sedType}] Preutfylling Utfylling Data")

                val sed = when(prefillData.sedType) {
                    P2100 -> PrefillP2100(prefillPdlNav).prefillSed(prefillData, personcollection, null).second
                    P5000 -> prefillP5000(prefillData, personcollection).also { logger.info("Preutfyll gjenny P5000: ") }
                    P6000 -> prefillP6000(
                        prefillData, personcollection, listeOverVedtak(prefillData, personcollection),
                        prefillNav = prefillPdlNav,
                        eessiInfo = eessiInformasjon,
                    )
                    P8000 -> prefillP8000(eessiInformasjon, prefillData, personcollection, prefillPdlNav).also { logger.info("Preutfyll P8000: ") }
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
        val sedType = prefillData.sedType

        val prefillPensjon = try {
            val gjenlevendePerson = if (prefillData.avdod != null) {
                logger.info("Preutfylling Utfylling Pensjon Avdod (etterlatt)")
                prefillPdlNav.createBruker(personData.forsikretPerson!!, null, null, prefillData.bruker)
            } else {
                logger.info("Preutfylling Utfylling Pensjon Gjenlevende (etterlatt)")
                prefillPdlNav.createBruker(personData.gjenlevendeEllerAvdod!!, null, null, prefillData.bruker)
            }

            logger.debug("[${prefillData.sedType}] Preutfylling Utfylling Pensjon")
            Pensjon(gjenlevende = gjenlevendePerson)
        } catch (ex: Exception) {
            logger.error(ex.message, ex)
            Pensjon()
        }

        //henter opp persondata
        val navSed = prefillPdlNav.prefill(
            penSaksnummer = prefillData.penSaksnummer,
            bruker = null,
            personData = personData,
            bankOgArbeid = prefillData.getBankOgArbeidFromRequest(),
            krav = prefillPensjon.kravDato,
            annenPerson = annenPersonHvisGjenlevende(prefillData, prefillPensjon.gjenlevende)
        )
        val avdodGjenny = navSed.copy(bruker = null)
        val sed = SED(sedType, nav = avdodGjenny, pensjon = prefillPensjon)

        return P5000(
            nav = sed.nav,
            pensjon = P5000Pensjon(
                gjenlevende = sed.pensjon?.gjenlevende
            )
        )

    }

    fun prefillP6000(
        prefillData: PrefillDataModel,
        personData: PersonDataCollection,
        etterlatteRespData: EtterlatteVedtakResponseData?,
        prefillNav: PrefillPDLNav,
        eessiInfo: EessiInformasjon
    ): P6000 {
        val sedType = prefillData.sedType

        logger.debug("Prefiller med vedtaksdata fra Gjenny: $etterlatteRespData")
        val gjenlevendePerson = prefillNav.createBruker(personData.gjenlevendeEllerAvdod!!, null, null, prefillData.bruker)
        val p6000Pensjon = PrefillP6000GjennyPensjon().prefillP6000GjennyPensjon(
                gjenlevendePerson,
                etterlatteRespData,
                eessiInfo
        )

        val nav = prefillNav.prefill(
            penSaksnummer = prefillData.penSaksnummer,
            bruker = prefillData.bruker,
            personData = personData,
            bankOgArbeid = prefillData.getBankOgArbeidFromRequest(),
            krav = p6000Pensjon?.kravDato,
            annenPerson = null
        )
        val navUtenBruker = nav.copy(bruker = null)

        return P6000(
            type = sedType,
            nav = navUtenBruker,
            pensjon = p6000Pensjon
        )
    }

    fun prefillP8000(
        eessiInfo: EessiInformasjon,
        prefillData: PrefillDataModel,
        personData: PersonDataCollection,
        prefillNav: PrefillPDLNav
    ): P8000 {
        val gjenlevendePerson = prefillNav.createBruker(personData.forsikretPerson!!, null, null, prefillData.bruker)

        logger.debug("gjenlevendeBruker: ${gjenlevendePerson?.person?.fornavn} PIN: ${gjenlevendePerson?.person?.pin?.firstOrNull()?.identifikator}, ReferanseTilPerson: ${prefillData.refTilPerson},  ")

        return if (gjenlevendePerson != null) {
            logger.info("Prefill P8000 forenklet preutfylling for gjenlevende uten avdød, Ferdig.")
            sedP8000(eessiInfo, null, null, prefillData, gjenlevendePerson)
        } else {
            throw HttpClientErrorException(HttpStatus.NOT_IMPLEMENTED, "Prefilling for gjenny av sed type ${prefillData.sedType} er ikke implementert")
        }
    }

    private fun sedP8000(eessiInformasjon: EessiInformasjon, forsikretPerson: Person?, adresse: Adresse?, prefillData: PrefillDataModel, annenPerson: Bruker?): P8000 {
        logger.info("forsikretPerson: ${forsikretPerson != null} annenPerson: ${annenPerson != null}"  )
        return P8000(
            nav = Nav(
                eessisak = listOf(
                    EessisakItem(
                        institusjonsid = eessiInformasjon.institutionid,
                        institusjonsnavn = eessiInformasjon.institutionnavn,
                        land = eessiInformasjon.institutionLand
                    )
                ),
                bruker = Bruker(
                    person = Person(
                        etternavn = forsikretPerson?.etternavn,
                        fornavn = forsikretPerson?.fornavn,
                        foedselsdato = forsikretPerson?.foedselsdato,
                        kjoenn = forsikretPerson?.kjoenn,
                        pin = forsikretPerson?.pin,
                        kontakt = forsikretPerson?.kontakt),
                    adresse = Adresse(
                        postnummer = adresse?.postnummer,
                        gate = adresse?.gate,
                        by = adresse?.by,
                        land = adresse?.land,
                        region = adresse?.region,
                        bygning = adresse?.bygning
                    )
                ),
                annenperson = utfyllAnnenperson(annenPerson)
            ),
            p8000Pensjon = utfyllReferanseTilPerson(prefillData)
        )

    }

    private fun utfyllReferanseTilPerson(prefillData: PrefillDataModel): P8000Pensjon? {
        val refTilperson = prefillData.refTilPerson ?: return null
        return P8000Pensjon(anmodning = AnmodningOmTilleggsInfo(referanseTilPerson = refTilperson.verdi))
    }

    private fun utfyllAnnenperson(gjenlevende: Bruker?): Bruker? {
        if (gjenlevende == null) return null
        gjenlevende.person?.rolle = PersonenRolle.SOEKER_ETTERRLATTEPENSJON.value
        return gjenlevende
    }

    private fun annenPersonHvisGjenlevende(prefillData: PrefillDataModel, gjenlevende: Bruker?): Bruker? {
        return if (prefillData.avdod != null) {
            gjenlevende?.person?.rolle = PersonenRolle.SOEKER_ETTERRLATTEPENSJON.value  //Claimant - etterlatte
            gjenlevende
        } else null
    }

    fun listeOverVedtak(prefillData: PrefillDataModel, personDataCollection: PersonDataCollection): EtterlatteVedtakResponseData? {
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