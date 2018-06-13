package no.nav.eessi.eessifagmodul.controllers

import no.nav.eessi.eessifagmodul.security.jaxws.client.AktoerIdClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import java.net.URI

@CrossOrigin
@RestController
@RequestMapping("/experiments")
class ExperimentController {

    @Autowired
    lateinit var aktoerIdClient: AktoerIdClient

    @Autowired
    lateinit var restTemplate: RestTemplate

    @Value("\${eessibasis.url}")
    lateinit var eessiBasisUrl: String

    @GetMapping("/testEuxOidc")
    fun testEuxOidc(authentication: OAuth2AuthenticationToken): ResponseEntity<String> {
        val oidcUser = authentication.principal as OidcUser
        val httpHeaders = HttpHeaders()
        httpHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer ${oidcUser.idToken.tokenValue}")
        val requestEntity = RequestEntity<String>(httpHeaders, HttpMethod.GET, URI("$eessiBasisUrl/sample"))

        try {
            return restTemplate.exchange(requestEntity, String::class.java)
        } catch(ex: Exception) {
            ex.printStackTrace()
            println("message: ${ex.message}")
            throw ex
        }
    }

    @GetMapping("/testAktoer/{ident}")
    fun testAktoer(@PathVariable("ident") ident: String, authentication: OAuth2AuthenticationToken): String? {
        val oidcUser = authentication.principal as OidcUser
        return aktoerIdClient.hentAktoerIdForIdent(ident, oidcUser.idToken.tokenValue)?.aktoerId
    }

}