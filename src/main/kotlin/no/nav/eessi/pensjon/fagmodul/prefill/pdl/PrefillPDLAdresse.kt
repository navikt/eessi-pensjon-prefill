package no.nav.eessi.pensjon.fagmodul.prefill.pdl

import no.nav.eessi.pensjon.fagmodul.sedmodel.Adresse
import no.nav.eessi.pensjon.personoppslag.pdl.model.AdressebeskyttelseGradering
import no.nav.eessi.pensjon.personoppslag.pdl.model.UtenlandskAdresse
import no.nav.eessi.pensjon.services.geo.PostnummerService
import no.nav.eessi.pensjon.services.kodeverk.KodeverkClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import no.nav.eessi.pensjon.personoppslag.pdl.model.Person as PDLPerson

@Component
class PrefillPDLAdresse (private val postnummerService: PostnummerService,
                         private val kodeverkClient: KodeverkClient) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillPDLAdresse::class.java) }

    /**
     *  2.2.2 adresse informasjon
     */
    fun createPersonAdresse(pdlperson: PDLPerson): Adresse? {
        logger.debug("2.2.2         Adresse")

        if (sjekkForDiskresjonKodeAdresse(pdlperson)) {
            return null
        }
        val bostedsadresse = pdlperson.bostedsadresse ?: return sjekkForUtlandsadresse(pdlperson)
        val vegadresse = bostedsadresse.vegadresse ?: return tomAdresse()

        val husnr = listOfNotNull(vegadresse.husnummer, vegadresse.husbokstav)
            .joinToString(separator = " ")
        logger.debug("preutfyller bostedadresse")
        return Adresse(
                postnummer = vegadresse.postnummer,
                gate = "${vegadresse.adressenavn} $husnr",
                by = postnummerService.finnPoststed(vegadresse.postnummer),
                land = "NO"
        )
    }

    private fun sjekkForUtlandsadresse(pdlperson: PDLPerson): Adresse {
        val opphold = pdlperson.oppholdsadresse ?: return kontaktUtlandsadresse(pdlperson)
        val utlandsAdresse = opphold.utenlandskAdresse ?: return kontaktUtlandsadresse(pdlperson)

        if(sjekkForGydligUtlandAdresse(utlandsAdresse)) {
            return kontaktUtlandsadresse(pdlperson)
        }

        logger.debug("preutfyller strukturert utlandsadresse")
        return Adresse(
            postnummer = utlandsAdresse.postkode,
            gate = utlandsAdresse.adressenavnNummer,
            by = utlandsAdresse.bySted,
            land = hentLandkode(utlandsAdresse.landkode)
        )

    }

    private fun sjekkForGydligUtlandAdresse(utlandsAdresse: UtenlandskAdresse): Boolean {
        var antallAdrlinjer = 0
        if (utlandsAdresse.adressenavnNummer.isNullOrEmpty())  antallAdrlinjer ++
        if (utlandsAdresse.bySted.isNullOrEmpty()) antallAdrlinjer ++
        if (utlandsAdresse.postkode.isNullOrEmpty()) antallAdrlinjer ++
        if (antallAdrlinjer >= 2) {
            return true
        }
        return false
    }

    private fun kontaktUtlandsadresse(pdlperson: PDLPerson) : Adresse {
        val kontaktadresse = pdlperson.kontaktadresse ?: return tomAdresse()
        val utenlandskAdresseIFrittFormat = kontaktadresse.utenlandskAdresseIFrittFormat ?: return tomAdresse()

        logger.debug("preutfyller ustrukturert utlandsadresse")
        return Adresse(
            gate = utenlandskAdresseIFrittFormat.adresselinje1,
            bygning = utenlandskAdresseIFrittFormat.adresselinje2,
            by = utenlandskAdresseIFrittFormat.adresselinje3,
            land = hentLandkode(utenlandskAdresseIFrittFormat.landkode)
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
        logger.debug("             Tom adresse")
        return Adresse(
                gate = "",
                bygning = "",
                by = "",
                postnummer = "",
                land = ""
        )
    }

    fun hentLandkode(landkode: String?): String? {
        return landkode?.let { kodeverkClient.finnLandkode2(it) }
    }
}
