package no.nav.eessi.eessifagmodul.services

import no.nav.tjeneste.virksomhet.aktoer.v2.binding.AktoerV2
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor
import org.apache.wss4j.common.ext.WSPasswordCallback
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.security.auth.callback.Callback
import javax.security.auth.callback.CallbackHandler

@Configuration
class AktoerIdClientConfig {

    @Value("\${aktoer.v2.endpointurl}")
    lateinit var endpointUrl: String

    @Value("\${credentials.srvpensjon.username}")
    lateinit var user: String

    @Value("\${credentials.srvpensjon.password}")
    lateinit var password: String

    @Bean
    fun aktoerIdProxy(): AktoerV2 {
        val proxy = JaxWsProxyFactoryBean()
        proxy.serviceClass = AktoerV2::class.java
        proxy.address = endpointUrl
        proxy.features.add(WSAddressingFeature())
        proxy.outInterceptors.add(WSS4JOutInterceptor(mapOf(
                "action" to "UsernameToken",
                "user" to user,
                "passwordType" to "PasswordText",
                "passwordCallbackRef" to PasswordCallbackHandler(password)
        )))
        return proxy.create() as AktoerV2
    }

    class PasswordCallbackHandler(private val password: String) : CallbackHandler {
        override fun handle(callbacks: Array<out Callback>?) {
            val pb: WSPasswordCallback = callbacks?.get(0) as WSPasswordCallback
            pb.password = password
        }
    }
}
