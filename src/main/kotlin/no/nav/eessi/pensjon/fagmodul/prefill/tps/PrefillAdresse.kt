package no.nav.eessi.pensjon.fagmodul.prefill.tps

import no.nav.eessi.pensjon.fagmodul.sedmodel.Adresse
import no.nav.eessi.pensjon.services.geo.PostnummerService
import no.nav.eessi.pensjon.services.kodeverk.KodeverkClient
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bostedsadresse
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Landkoder
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PrefillAdresse ( private val postnummerService: PostnummerService,
                       private val kodeverkClient: KodeverkClient) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillAdresse::class.java) }

    /**
     *  2.2.2 adresse informasjon
     */
    fun createPersonAdresse(personTPS: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person): Adresse? {
        logger.debug("2.2.2         Adresse")

        if (sjekkForDiskresjonKodeAdresse(personTPS)) {
            return null
        }

        //Gateadresse eller UstrukturertAdresse
        val bostedsadresse: Bostedsadresse = personTPS.bostedsadresse ?: return hentPersonAdresseUstrukturert()

        val gateAdresse = bostedsadresse.strukturertAdresse as Gateadresse
        val gate = gateAdresse.gatenavn
        val husnr = gateAdresse.husnummer
        return Adresse(
                postnummer = gateAdresse.poststed.value,
                gate = "$gate $husnr",
                land = hentLandkode(gateAdresse.landkode),
                by = postnummerService.finnPoststed(gateAdresse.poststed.value)
        )
    }

    protected fun sjekkForDiskresjonKodeAdresse(personTPS: Person): Boolean {
        logger.debug("diskresjonskode:  ${personTPS.diskresjonskode}")

        val diskresjons = personTPS.diskresjonskode
        if (diskresjons != null && (diskresjons.value == "SPFO" || diskresjons.value == "SPSF")) {
            logger.debug("2.2.2         Adresse, diskresjon ingen adresse")
            return true
        }
        return false
    }

    //TODO: Denne metoden gjør ikke det den sier at den skal gjøre
    /**
     *  2.2.2 ustrukturert
     *
     *  Returnerer en bank adresse dersom det finnes en ustrukturertAdresse hos borger.
     *  Dette må så endres/rettes av saksbehendlaer i rina?
     */
    private fun hentPersonAdresseUstrukturert(): Adresse {
        logger.debug("             UstrukturertAdresse (utland)")
        return Adresse(
                gate = "",
                bygning = "",
                by = "",
                postnummer = "",
                land = ""
        )
    }

    fun hentLandkode(landkodertps: Landkoder): String? {
        return kodeverkClient.finnLandkode2(landkodertps.value)
    }
}