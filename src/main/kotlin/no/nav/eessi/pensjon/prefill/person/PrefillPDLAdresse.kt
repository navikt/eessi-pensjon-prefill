package no.nav.eessi.pensjon.prefill.person

import no.nav.eessi.pensjon.eux.model.sed.Adresse
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.model.AdressebeskyttelseGradering
import no.nav.eessi.pensjon.personoppslag.pdl.model.NorskIdent
import no.nav.eessi.pensjon.personoppslag.pdl.model.PostadresseIFrittFormat
import no.nav.eessi.pensjon.personoppslag.pdl.model.UtenlandskAdresse
import no.nav.eessi.pensjon.personoppslag.pdl.model.UtenlandskAdresseIFrittFormat
import no.nav.eessi.pensjon.personoppslag.pdl.model.Vegadresse
import no.nav.eessi.pensjon.kodeverk.PostnummerService
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import no.nav.eessi.pensjon.personoppslag.pdl.model.Person as PDLPerson

@Component
class PrefillPDLAdresse (private val postnummerService: PostnummerService,
                         private val kodeverkClient: KodeverkClient,
                         private val personService: PersonService) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillPDLAdresse::class.java) }

    /**
     *  2.2.2 adresse informasjon
     */
    fun createPersonAdresse(pdlperson: PDLPerson): Adresse? {
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
            by = postnummerService.finnPoststed(vegadresse.postnummer),
            land = "NO"
        )
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


    private fun sjekkOgPreutfyllAdresse(pdlperson: PDLPerson): Adresse {

        if (pdlperson.erDoed()) {
            logger.info("              person er død. sjekker kontaktinformasjonForDoedsbo")
            if (pdlperson.kontaktinformasjonForDoedsbo != null) {
                logger.info("              preutfyller kontaktinformasjonForDoedsbo i adressefelt")
                return preutfyllDoedsboAdresse(pdlperson)
            }
        }

        loggErrorVedFlereGyldigeAdresser(pdlperson)

        return when {
            // En Kontaktadresse kan nå være av typen Postboksadresse, Vegadresse, UtenlandskAdresse, PostadresseIFrittFormat, UtenlandskAdresseIFrittFormat og bare en av disse vil være utfylt
            kanNorskVegadresseBenyttes(pdlperson.kontaktadresse?.vegadresse) -> preutfullNorskBostedVegadresse(pdlperson.kontaktadresse?.vegadresse)
            kanUtlandsadresseBenyttes(pdlperson.kontaktadresse?.utenlandskAdresse) -> preutfyllUtlandsAdresse(pdlperson.kontaktadresse?.utenlandskAdresse)
            kanUtlandsadresseIFrittFormatBenyttes(pdlperson.kontaktadresse?.utenlandskAdresseIFrittFormat) -> preutfyllUtenlandskAdresseIFrittFormat(pdlperson.kontaktadresse?.utenlandskAdresseIFrittFormat)
            kanNorskPostadresseIFrittFormatBenyttes(pdlperson.kontaktadresse?.postadresseIFrittFormat) -> preutfyllNorskPostadresseIFrittFormat(pdlperson.kontaktadresse?.postadresseIFrittFormat)

            kanNorskVegadresseBenyttes(pdlperson.bostedsadresse?.vegadresse) -> preutfullNorskBostedVegadresse(pdlperson.bostedsadresse?.vegadresse)
            kanUtlandsadresseBenyttes(pdlperson.bostedsadresse?.utenlandskAdresse) -> preutfyllUtlandsAdresse(pdlperson.bostedsadresse?.utenlandskAdresse)

            kanNorskVegadresseBenyttes(pdlperson.oppholdsadresse?.vegadresse) -> preutfullNorskBostedVegadresse(pdlperson.oppholdsadresse?.vegadresse)
            kanUtlandsadresseBenyttes(pdlperson.oppholdsadresse?.utenlandskAdresse) -> preutfyllUtlandsAdresse(pdlperson.oppholdsadresse?.utenlandskAdresse)

            else -> tomAdresse()
        }
    }

    private fun preutfyllDoedsboAdresse(pdlperson: no.nav.eessi.pensjon.personoppslag.pdl.model.Person): Adresse {
        val landkode = pdlperson.kontaktinformasjonForDoedsbo!!.adresse.landkode
        val landKode2Tegn = if (landkode == null || landkode.length == 2) landkode else hentLandkode(landkode)

        return PrefillDodsboAdresse().preutfyllDodsboAdresse(
            pdlperson.kontaktinformasjonForDoedsbo!!,
            landKode2Tegn
        ) { idenfikasjonsnummer: String ->
            personService.hentPersonnavn(NorskIdent(idenfikasjonsnummer))
                ?: throw NullPointerException("Uventet nullverdi etter oppslag mot PDL på personnavn for $idenfikasjonsnummer")
        }
    }


    fun loggErrorVedFlereGyldigeAdresser(pdlperson: PDLPerson) {
        val adresseListe = listOf(
            Pair("kontaktadresse.vegadresse", pdlperson.kontaktadresse?.vegadresse),
            Pair("kontaktadresse.utenlandskAdresse", pdlperson.kontaktadresse?.utenlandskAdresse),
            Pair("kontaktadresse.utenlandskAdresseIFrittFormat", pdlperson.kontaktadresse?.utenlandskAdresseIFrittFormat),
            Pair("kontaktadresse.postadresseIFrittFormat", pdlperson.kontaktadresse?.postadresseIFrittFormat),

            Pair("bostedsadresse.vegadresse", pdlperson.bostedsadresse?.vegadresse),
            Pair("bostedsadresse.utenlandskAdresse", pdlperson.bostedsadresse?.utenlandskAdresse),

            Pair("oppholdsadresse.vegadresse", pdlperson.oppholdsadresse?.vegadresse),
            Pair("oppholdsadresse.utenlandskAdresse", pdlperson.oppholdsadresse?.utenlandskAdresse),
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
        )

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

    protected fun sjekkForDiskresjonKodeAdresse(pdlperson: PDLPerson): Boolean {
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
        return landkode?.let { kodeverkClient.finnLandkode(it) }
    }

}

