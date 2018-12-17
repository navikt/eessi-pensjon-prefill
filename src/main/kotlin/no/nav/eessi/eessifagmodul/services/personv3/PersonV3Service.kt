package no.nav.eessi.eessifagmodul.services.personv3

import no.nav.eessi.eessifagmodul.config.sts.configureRequestSamlTokenOnBehalfOfOidc
import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningRequest
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import org.springframework.stereotype.Component

@Component
class PersonV3Service(val service: PersonV3, val oidcRequestContextHolder: OIDCRequestContextHolder) {

    fun ping(): Boolean {
//        var response: Boolean
//        try {
//            service.ping()
//            print("PersonV3: PING SERVICE OK")
//            response = true
//        } catch (ex: Exception) {
//            print("PersonV3: PING SERVICE FAIL")
//            response = false
//        }
//        return response
        return true
    }

    fun hentPerson(fnr: String): HentPersonResponse {
        val token = oidcRequestContextHolder.oidcValidationContext.getToken("oidc")

        configureRequestSamlTokenOnBehalfOfOidc(service, token.idToken)

        val request = HentPersonRequest().apply {
            withAktoer(PersonIdent().withIdent(
                    NorskIdent().withIdent(fnr)))

            withInformasjonsbehov(listOf(
                    Informasjonsbehov.ADRESSE,
                    Informasjonsbehov.FAMILIERELASJONER
            ))
        }
        return service.hentPerson(request)
    }

    //Experimental only
    fun hentGeografi(fnr: String): HentGeografiskTilknytningResponse {

        val token = oidcRequestContextHolder.oidcValidationContext.getToken("oidc")
        configureRequestSamlTokenOnBehalfOfOidc(service, token.idToken)

        val request = HentGeografiskTilknytningRequest().apply {
            withAktoer(PersonIdent().withIdent(
                    NorskIdent().withIdent(fnr))
            )
        }
        return service.hentGeografiskTilknytning(request)
    }

}