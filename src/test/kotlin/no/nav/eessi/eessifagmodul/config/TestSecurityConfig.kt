package no.nav.eessi.eessifagmodul.config

import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter


@Configuration
@Order(1)
@EnableWebSecurity
class TestSecurityConfig : WebSecurityConfigurerAdapter() {

//    @Throws(Exception::class)
//    override fun configure(auth: AuthenticationManagerBuilder) {
//        println("TestSecurityConfig.configure(auth) called")
//        auth.inMemoryAuthentication()
//                .withUser("user")
//                .password("password")
//                .roles("USER")
//                .and()
//                .withUser("admin")
//                .password("admin")
//                .roles("USER", "ADMIN")
//    }

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        println("TestSecurityConfig.configure(http) called")
        http.httpBasic().disable()
    }
}