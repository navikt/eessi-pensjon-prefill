package no.nav.eessi.pensjon.fagmodul.prefill.tps

import no.nav.eessi.pensjon.fagmodul.sedmodel.*
import no.nav.eessi.pensjon.services.personv3.PersonV3Service
import no.nav.eessi.pensjon.utils.simpleFormat
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PrefillPersonDataFromTPS(private val personV3Service: PersonV3Service) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillPersonDataFromTPS::class.java) }

    fun hentBrukerFraTPS(ident: String): no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker? {
        logger.debug("              Bruker")
        val brukerTPS = try {
            hentBrukerTPS(ident)
        } catch (ex: Exception) {
            logger.error("Feil ved henting av Bruker fra TPS, sjekk ident?")
            null
        }
        return brukerTPS
    }


    //bruker fra TPS
    private fun hentBrukerTPS(ident: String): no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker {
        val response = personV3Service.hentPerson(ident)
        return response.person as no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker
    }

    // FIXME TODO Ser ikke ut til å være i bruk??
    //Sivilstand ENKE, PENS, SINGLE Familiestatus
    //Dette feilter tilsvarer Familie statius i Rina kap. 2.2.2 eller 5.2.2
    fun createSivilstand(brukerTps: no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker): List<SivilstandItem> {
        logger.debug("2.2.2           Sivilstand / Familiestatus (01 Enslig, 02 Gift, 03 Samboer, 04 Partnerskal, 05 Skilt, 06 Skilt partner, 07 Separert, 08 Enke)")
        val sivilstand = brukerTps.sivilstand as Sivilstand
        val status = mapOf("GIFT" to "02", "REPA" to "04", "ENKE" to "08", "SAMB" to "03", "SEPA" to "07", "UGIF" to "01", "SKIL" to "05", "SKPA" to "06")
        return listOf(SivilstandItem(
                fradato = sivilstand.fomGyldighetsperiode.simpleFormat(),
                status = status[sivilstand.sivilstand.value]
        ))
    }

}