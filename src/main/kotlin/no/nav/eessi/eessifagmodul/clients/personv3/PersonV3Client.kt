package no.nav.eessi.eessifagmodul.clients.personv3

import no.nav.eessi.eessifagmodul.config.sts.configureRequestSamlTokenOnBehalfOfOidc
import no.nav.freg.security.oidc.common.OidcTokenAuthentication
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class PersonV3Client(val service: PersonV3) {

    fun hentPerson(fnr: String): HentPersonResponse {

        val auth = SecurityContextHolder.getContext().authentication as OidcTokenAuthentication
        configureRequestSamlTokenOnBehalfOfOidc(service, auth.idToken)

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
}