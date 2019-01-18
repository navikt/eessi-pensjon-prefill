package no.nav.eessi.eessifagmodul.controllers

import no.nav.security.oidc.test.support.spring.TokenGeneratorController
import org.springframework.web.bind.annotation.RestController

@RestController
class UnprotectedController : TokenGeneratorController()