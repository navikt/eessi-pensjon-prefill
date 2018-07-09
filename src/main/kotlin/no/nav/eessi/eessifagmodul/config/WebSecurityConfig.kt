package no.nav.eessi.eessifagmodul.config

import no.nav.freg.security.oidc.common.HttpSecurityConfigurer
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@Order(10001)
@Configuration
class WebSecurityConfig: HttpSecurityConfigurer {

    override fun configure(http: HttpSecurity) {
        http.csrf().disable()
    }

}