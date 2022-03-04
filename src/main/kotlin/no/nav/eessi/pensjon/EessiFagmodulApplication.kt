package no.nav.eessi.pensjon

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Profile

@EnableJwtTokenValidation(ignore = ["org.springframework", "springfox.documentation", "no.nav.eessi.pensjon.fagmodul.health.DiagnosticsController"])
@SpringBootApplication
@EnableCaching
@Profile("!unsecured-webmvctest")
class EessiFagmodulApplication

/**
 * under development (Intellij) må hva med under Vm option:
 * -Dspring.profiles.active=local  local run T environment
 *
 */
fun main(args: Array<String>) {
    runApplication<EessiFagmodulApplication>(*args)
}
