package no.nav.eessi.eessifagmodul.services

import no.nav.eessi.eessifagmodul.clients.personv3.PersonV3Client
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import org.springframework.stereotype.Service

//Utgaar...
@Service
class TpsService(val personV3Client: PersonV3Client) {

    fun hentPerson(ident: String): Bruker  {
        if (ident.length != 11) {
            throw IllegalArgumentException("Feil format i ident")
        }
        try {
            val response: HentPersonResponse = personV3Client.hentPerson(ident)
            return response.person as Bruker
        } catch (he: HentPersonPersonIkkeFunnet) {
            throw IllegalArgumentException(he.message)
        } catch (hx: HentPersonSikkerhetsbegrensning) {
            throw IllegalArgumentException(hx.message)
        } catch (ex: Exception) {
            throw IllegalArgumentException("Feil ved hentPerson")
        }

    }

}