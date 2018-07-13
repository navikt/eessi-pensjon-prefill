package no.nav.eessi.eessifagmodul.services

import no.nav.eessi.eessifagmodul.clients.personv3.PersonV3Client
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import org.springframework.stereotype.Service

@Service
class TpsService(val personV3Client: PersonV3Client) {

    fun hentPerson(ident: String): HentPersonResponse  {
        if (ident.length != 11) {
            throw IllegalArgumentException("Feil format i ident")
        }
        return  personV3Client.hentPerson(ident)
    }

}