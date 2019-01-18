package no.nav.eessi.eessifagmodul.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class EessiFagmodulApplicationConfig(private val controllerRequestInterceptor: ControllerRequestInterceptor) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        //super.addInterceptors(registry)
        registry.addInterceptor(controllerRequestInterceptor).addPathPatterns("/**/sed/**/")
    }

}