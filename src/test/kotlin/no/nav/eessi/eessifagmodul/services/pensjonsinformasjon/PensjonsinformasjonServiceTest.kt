package no.nav.eessi.eessifagmodul.services.pensjonsinformasjon

import no.nav.eessi.eessifagmodul.services.pensjonsinformasjon.requesttransformer.RequestBuilder
import org.junit.Test

import org.junit.Before
import org.springframework.web.client.RestTemplate

class PensjonsinformasjonServiceTest {

    lateinit var pensjonsinformasjonService: PensjonsinformasjonService

    @Before
    fun setup() {
        pensjonsinformasjonService = PensjonsinformasjonService(RestTemplate(), RequestBuilder())
    }

    @Test
    fun hentSak() {
        pensjonsinformasjonService.hentAlt("1243")
    }
}