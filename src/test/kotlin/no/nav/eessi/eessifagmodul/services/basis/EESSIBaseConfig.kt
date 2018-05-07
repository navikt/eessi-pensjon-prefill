package no.nav.eessi.eessifagmodul.services.basis

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EESSIBaseConfig {

    private val log: Logger = LoggerFactory.getLogger(EESSIBaseConfig::class.java)

    //@Value("\${eessibasis.mockit}")
    //lateinit var mockbase : String

    @Bean
    fun eessiBaseBygg() : EESSIBasis {
       // log.debug("****************************************")
        //log.debug("*   Mockbase: $mockbase                *")
        //log.debug("****************************************")

        //if ("true".equals(mockbase)) {
        //    return EESSIBasisMock()
        //}
        return EESSIBasisImp()
    }

}