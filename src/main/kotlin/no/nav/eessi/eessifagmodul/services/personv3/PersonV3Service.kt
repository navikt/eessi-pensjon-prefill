package no.nav.eessi.eessifagmodul.services.personv3

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.eessi.eessifagmodul.config.sts.configureRequestSamlTokenOnBehalfOfOidc
import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning
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

//    val HENTPERSON_TELLER_NAVN = "eessipensjon_fagmodul.hentperson"
//    val HENTPERSON_TELLER_TYPE_VELLYKKEDE = counter(HENTPERSON_TELLER_NAVN, "vellykkede")
//    val HENTPERSON_TELLER_TYPE_FEILEDE = counter(HENTPERSON_TELLER_NAVN, "feilede")

    fun counter(name: String, type: String): Counter {
        return Metrics.counter(name, "type", type)
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
        try {
            val resp = service.hentPerson(request)
//            HENTPERSON_TELLER_TYPE_VELLYKKEDE.increment()
            return resp
        } catch (personIkkefunnet : HentPersonPersonIkkeFunnet) {
//            HENTPERSON_TELLER_TYPE_FEILEDE.increment()
            throw personIkkefunnet
        } catch (personSikkerhetsbegrensning: HentPersonSikkerhetsbegrensning) {
//            HENTPERSON_TELLER_TYPE_FEILEDE.increment()
            throw personSikkerhetsbegrensning
        }    }

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