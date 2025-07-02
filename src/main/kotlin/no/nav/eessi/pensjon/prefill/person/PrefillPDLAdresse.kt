package no.nav.eessi.pensjon.prefill.person

import no.nav.eessi.pensjon.eux.model.sed.Adresse
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.model.*
import no.nav.eessi.pensjon.personoppslag.pdl.model.Ident.Companion.bestemIdent
import no.nav.eessi.pensjon.utils.toJson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import no.nav.eessi.pensjon.personoppslag.pdl.model.PdlPerson

@Component
class PrefillPDLAdresse (
    private val kodeverkClient: KodeverkClient,
    private val personService: PersonService,
    @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper.ForTest()
) {

    private lateinit var hentLandkodeMetric: MetricsHelper.Metric
    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillPDLAdresse::class.java) }

    init {
        hentLandkodeMetric = metricsHelper.init("hentLandkodeMetric")
    }

    /**
     *  2.2.2 adresse informasjon
     */
    fun createPersonAdresse(pdlperson: PdlPerson): Adresse? {
        logger.debug("2.2.2         Adresse")

        if (sjekkForDiskresjonKodeAdresse(pdlperson)) {
            return null
        }

        logger.debug("              Sjekk for og preutfyll adresse")
        return sjekkOgPreutfyllAdresse(pdlperson)
    }

    private fun preutfullNorskBostedVegadresse(vegadresse: Vegadresse?): Adresse {
        if (vegadresse == null) return tomAdresse()
        logger.info("              preutfyller bostedadresse land NO")
        val husnr = listOfNotNull(vegadresse.husnummer, vegadresse.husbokstav)
            .joinToString(separator = " ")

        return Adresse(
            gate = "${vegadresse.adressenavn} $husnr",
            postnummer = vegadresse.postnummer,
            by = kodeverkClient.hentPostSted(vegadresse.postnummer)?.sted,
            land = "NO"
        ).also { logger.info("              preutfyller bostedadresse land NO, by: ${it.by}") }
    }

    private fun preutfyllNorskPostadresseIFrittFormat(postadresseIFrittFormat: PostadresseIFrittFormat?): Adresse {
        if (postadresseIFrittFormat == null) return tomAdresse()
        logger.info("              preutfyller postadresseIFrittFormat land NO")
        return Adresse(
            gate = postadresseIFrittFormat.adresselinje1,
            bygning = postadresseIFrittFormat.adresselinje2,
            by = postadresseIFrittFormat.adresselinje3,
            land = "NO"
        )
    }

    private fun sjekkOgPreutfyllAdresse(pdlperson: PdlPerson): Adresse {
        if (pdlperson.erDoed()) {
            logger.info("              person er død. sjekker kontaktinformasjonForDoedsbo")
            if (pdlperson.kontaktinformasjonForDoedsbo != null) {
                logger.info("              preutfyller kontaktinformasjonForDoedsbo i adressefelt")
                return preutfyllDoedsboAdresse(pdlperson)
            }
        }

        loggErrorVedFlereGyldigeAdresser(pdlperson)

        val kontaktAdresse = pdlperson.kontaktadresse
        val bostedAdresse = pdlperson.bostedsadresse
        val oppholdsAdresse = pdlperson.oppholdsadresse

        return when {
            // En Kontaktadresse kan nå være av typen Postboksadresse, Vegadresse, UtenlandskAdresse, PostadresseIFrittFormat, UtenlandskAdresseIFrittFormat og bare en av disse vil være utfylt
            kanNorskVegadresseBenyttes(kontaktAdresse?.vegadresse) -> preutfullNorskBostedVegadresse(kontaktAdresse?.vegadresse)
            kanUtlandsadresseBenyttes(kontaktAdresse?.utenlandskAdresse) -> preutfyllUtlandsAdresse(kontaktAdresse?.utenlandskAdresse)

            kanUtlandsadresseIFrittFormatBenyttes(kontaktAdresse?.utenlandskAdresseIFrittFormat) -> preutfyllUtenlandskAdresseIFrittFormat(kontaktAdresse?.utenlandskAdresseIFrittFormat)
            kanNorskPostadresseIFrittFormatBenyttes(kontaktAdresse?.postadresseIFrittFormat) -> preutfyllNorskPostadresseIFrittFormat(kontaktAdresse?.postadresseIFrittFormat)

            kanNorskVegadresseBenyttes(bostedAdresse?.vegadresse) -> preutfullNorskBostedVegadresse(bostedAdresse?.vegadresse)
            kanUtlandsadresseBenyttes(bostedAdresse?.utenlandskAdresse) -> preutfyllUtlandsAdresse(bostedAdresse?.utenlandskAdresse)

            kanNorskVegadresseBenyttes(oppholdsAdresse?.vegadresse) -> preutfullNorskBostedVegadresse(oppholdsAdresse?.vegadresse)
            kanUtlandsadresseBenyttes(oppholdsAdresse?.utenlandskAdresse) -> preutfyllUtlandsAdresse(oppholdsAdresse?.utenlandskAdresse)

            else -> tomAdresse()
        }
    }

    private fun preutfyllDoedsboAdresse(pdlperson: no.nav.eessi.pensjon.personoppslag.pdl.model.PdlPerson): Adresse {
        val kontaktinformasjonForDoedsbo = pdlperson.kontaktinformasjonForDoedsbo
        val landkode = kontaktinformasjonForDoedsbo!!.adresse.landkode
        val landKode2Tegn = if (landkode == null || landkode.length == 2) landkode else hentLandkode(landkode)

        return PrefillDodsboAdresse().preutfyllDodsboAdresse(
            kontaktinformasjonForDoedsbo,
            landKode2Tegn
        ) { idenfikasjonsnummer: String ->
            personService.hentPersonnavn(bestemIdent(idenfikasjonsnummer))
                ?: throw NullPointerException("Uventet nullverdi etter oppslag mot PDL på personnavn for $idenfikasjonsnummer")
        }
    }

    fun loggErrorVedFlereGyldigeAdresser(pdlperson: PdlPerson) {
        val kontaktadresse = pdlperson.kontaktadresse
        val bostedsadresse = pdlperson.bostedsadresse
        val oppholdsadresse = pdlperson.oppholdsadresse

        val adresseListe = listOf(
            Pair("kontaktadresse.vegadresse", kontaktadresse?.vegadresse),
            Pair("kontaktadresse.utenlandskAdresse", kontaktadresse?.utenlandskAdresse),
            Pair("kontaktadresse.utenlandskAdresseIFrittFormat", kontaktadresse?.utenlandskAdresseIFrittFormat),
            Pair("kontaktadresse.postadresseIFrittFormat", kontaktadresse?.postadresseIFrittFormat),

            Pair("bostedsadresse.vegadresse", bostedsadresse?.vegadresse),
            Pair("bostedsadresse.utenlandskAdresse", bostedsadresse?.utenlandskAdresse),

            Pair("oppholdsadresse.vegadresse", oppholdsadresse?.vegadresse),
            Pair("oppholdsadresse.utenlandskAdresse", oppholdsadresse?.utenlandskAdresse),
        ).mapNotNull {
            if (it.second != null) it else null
        }.toMap()
        if(adresseListe.size > 1){
            logger.warn("Fant flere gyldig adresser: ${adresseListe.entries.map { "\n" + it.key }}")
        }
    }

    fun kanNorskVegadresseBenyttes(vegadresse: Vegadresse?): Boolean {
        if (vegadresse == null) return false
        return !vegadresse.adressenavn.isNullOrEmpty() and
                !vegadresse.postnummer.isNullOrEmpty()
    }

    fun kanNorskPostadresseIFrittFormatBenyttes(postadresseIFrittFormat: PostadresseIFrittFormat?): Boolean {
        if (postadresseIFrittFormat == null) return false
        return  !postadresseIFrittFormat.adresselinje1.isNullOrEmpty() and
                !postadresseIFrittFormat.adresselinje2.isNullOrEmpty() and
                !postadresseIFrittFormat.adresselinje3.isNullOrEmpty()
    }

    fun kanUtlandsadresseBenyttes(utlandsAdresse: UtenlandskAdresse?): Boolean {
        if (utlandsAdresse == null) return false
        return  !(utlandsAdresse.adressenavnNummer.isNullOrEmpty() and
                utlandsAdresse.bySted.isNullOrEmpty() and
                utlandsAdresse.postkode.isNullOrEmpty() and
                (utlandsAdresse.postkode?.let {
                    it.length <= 25
                } == true))
    }

    fun kanUtlandsadresseIFrittFormatBenyttes(utenlandskAdresseIFrittFormat: UtenlandskAdresseIFrittFormat?): Boolean {
        if (utenlandskAdresseIFrittFormat == null) return false
        return !(utenlandskAdresseIFrittFormat.adresselinje1.isNullOrEmpty() and
                utenlandskAdresseIFrittFormat.adresselinje2.isNullOrEmpty() and
                utenlandskAdresseIFrittFormat.adresselinje3.isNullOrEmpty() and
                utenlandskAdresseIFrittFormat.byEllerStedsnavn.isNullOrEmpty() and
                utenlandskAdresseIFrittFormat.landkode.isNullOrEmpty() and
                utenlandskAdresseIFrittFormat.postkode.isNullOrEmpty())
    }

    private fun preutfyllUtlandsAdresse(utlandsAdresse: UtenlandskAdresse?): Adresse {
        logger.info("              preutfyller strukturert utlandsAdresse")
        if (utlandsAdresse == null) return tomAdresse()
        return Adresse(
            postnummer = utlandsAdresse.postkode,
            gate = utlandsAdresse.adressenavnNummer,
            by = utlandsAdresse.bySted,
            land = hentLandkode(utlandsAdresse.landkode),
            region = utlandsAdresse.regionDistriktOmraade,
            bygning = utlandsAdresse.bygningEtasjeLeilighet
        ).also { logger.debug("*** Utenlandskadresse inn: ${utlandsAdresse.toJson()} \n preutfyltadresse: ${it.toJson()}*****")}

    }

    private fun preutfyllUtenlandskAdresseIFrittFormat(utenlandskAdresseIFrittFormat: UtenlandskAdresseIFrittFormat?) : Adresse {
        logger.info("              preutfyller utenlandskAdresseIFrittFormat")
        return Adresse(
            gate = utenlandskAdresseIFrittFormat?.adresselinje1,
            bygning = utenlandskAdresseIFrittFormat?.adresselinje2,
            by = utenlandskAdresseIFrittFormat?.adresselinje3,
            land = hentLandkode(utenlandskAdresseIFrittFormat?.landkode)
        )

    }

    protected fun sjekkForDiskresjonKodeAdresse(pdlperson: PdlPerson): Boolean {
        logger.debug("diskresjonskode:  ${pdlperson.adressebeskyttelse}")
        logger.debug("2.2.2         Adresse, diskresjon ingen adresse?")
        return pdlperson.adressebeskyttelse.any {
            it == AdressebeskyttelseGradering.FORTROLIG || it == AdressebeskyttelseGradering.STRENGT_FORTROLIG
        }
    }

    /**
     *  2.2.2 tom
     *
     *  Returnerer en blank adresse
     *  Dette må så endres/rettes av saksbehendlaer i rina?
     */
    private fun tomAdresse(): Adresse {
        logger.info("             Tom adresse")
        return Adresse(
            gate = "",
            bygning = "",
            by = "",
            postnummer = "",
            land = ""
        )
    }

    fun hentLandkode(landkode: String?): String? {
        return hentLandkodeMetric.measure {
            landkode?.let { kodeverkClient.finnLandkode(it) }
        }
    }

}

