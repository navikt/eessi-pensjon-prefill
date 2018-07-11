package no.nav.eessi.eessifagmodul.config

import no.nav.freg.security.oidc.common.HttpSecurityConfigurer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity

@Order(10001)
@Configuration
class WebSecurityConfig: HttpSecurityConfigurer {

    private val logger: Logger by lazy { LoggerFactory.getLogger(WebSecurityConfig::class.java) }

    override fun configure(http: HttpSecurity) {
        logger.info("WebSecurityConfig disable CSRF")
        http.csrf().disable()
    }

}
