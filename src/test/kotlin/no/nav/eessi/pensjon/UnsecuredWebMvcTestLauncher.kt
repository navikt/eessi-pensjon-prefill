package no.nav.eessi.pensjon

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Profile
import org.springframework.test.annotation.DirtiesContext

@SpringBootApplication
@Profile("unsecured-webmvctest")
@DirtiesContext
class UnsecuredWebMvcTestLauncher

fun main(args: Array<String>) {
    runApplication<UnsecuredWebMvcTestLauncher>(*args)
}
