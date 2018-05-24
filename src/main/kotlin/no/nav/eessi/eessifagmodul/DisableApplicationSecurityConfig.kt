package no.nav.eessi.eessifagmodul

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

/**
 * Configures spring-security to not automatically set up basic-auth login when freg-security is disabled
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = ["freg.security.oidc.enabled"], havingValue = "false")
class DisableApplicationSecurityConfig : WebSecurityConfigurerAdapter() {

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http.httpBasic().disable()
    }
}