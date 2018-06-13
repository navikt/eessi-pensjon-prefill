package no.nav.eessi.eessifagmodul.security.jaxws.client

import no.nav.tjeneste.virksomhet.aktoer.v2.binding.AktoerV2
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AktoerIdClientConfig {

    @Value("\${aktoer.v2.endpointurl}")
    lateinit var endpointUrl: String

    @Bean
    fun setupAktoerV2(): AktoerV2 {
        val factory = JaxWsProxyFactoryBean()
        factory.serviceClass = AktoerV2::class.java
        factory.address = endpointUrl
        // Debug/logging av meldinger som sendes mellom app og tilbyder
        //factory.features.add(LoggingFeature()) // TODO: Add denne featureren bare dersom DEBUG er enabled
        return factory.create() as AktoerV2
    }
}
