package no.nav.eessi.eessifagmodul

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso
import org.springframework.boot.runApplication

@SpringBootApplication
class EessiFagmodulApplication

/**
 * under development (Intellij) må hva med under Vm option:
 * -Dspring.profiles.active=local
 *
 */
fun main(args: Array<String>) {
    runApplication<EessiFagmodulApplication>(*args)
}
