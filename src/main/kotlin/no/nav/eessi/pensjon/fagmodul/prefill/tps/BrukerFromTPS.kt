package no.nav.eessi.pensjon.fagmodul.prefill.tps

import no.nav.eessi.pensjon.services.personv3.PersonV3Service
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BrukerFromTPS(private val personV3Service: PersonV3Service) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(BrukerFromTPS::class.java) }

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
}
