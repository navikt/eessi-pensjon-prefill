package no.nav.eessi.eessifagmodul.clients.personv3

import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PersonV3Config {

    @Value("\${virksomhet.person.v3.endpointurl}")
    lateinit var endpointUrl: String

    @Bean
    fun personV3(): PersonV3 {
        val factory = JaxWsProxyFactoryBean()
        factory.serviceClass = PersonV3::class.java
        factory.address = endpointUrl
        // Debug/logging av meldinger som sendes mellom app og tilbyder
        //factory.features.add(LoggingFeature()) // TODO: Add denne featureren bare dersom DEBUG er enabled
        return factory.create() as PersonV3
    }

}