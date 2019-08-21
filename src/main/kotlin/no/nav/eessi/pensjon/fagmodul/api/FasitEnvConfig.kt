package no.nav.eessi.pensjon.fagmodul.api

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FasitEnvConfig {

    @Value("\${FASIT_ENVIRONMENT_NAME}")
    private lateinit var fasitEnv: String

    @Bean
    fun fasitEnvName(): String {
        return fasitEnv
    }

}
