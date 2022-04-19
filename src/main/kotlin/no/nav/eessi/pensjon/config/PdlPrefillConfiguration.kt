package no.nav.eessi.pensjon.config

import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.eessi.pensjon.personoppslag.pdl.PdlToken
import no.nav.eessi.pensjon.personoppslag.pdl.PdlTokenCallBack
import no.nav.eessi.pensjon.personoppslag.pdl.PdlTokenImp
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component


@Component("PdlTokenComponent")
@Profile("prod", "test")
@Primary
@Order(Ordered.HIGHEST_PRECEDENCE)
class PdlPrefillTokenComponent(private val tokenValidationContextHolder: TokenValidationContextHolder): PdlTokenCallBack {

    private val logger = LoggerFactory.getLogger(PdlPrefillTokenComponent::class.java)


    @Value("\${AZURE_APP_PDL_CLIENT_ID}")
    private lateinit var pdlClientId: String

    override fun callBack(): PdlToken {
        val navidentTokenFromFagmodul = getToken(tokenValidationContextHolder).tokenAsString

        logger.info("NavIdent fra fagmodul: $navidentTokenFromFagmodul")
        val tokenClient: AzureAdOnBehalfOfTokenClient = AzureAdTokenClientBuilder.builder()
            .withNaisDefaults()
            .buildOnBehalfOfTokenClient()

        val accessToken: String = tokenClient.exchangeOnBehalfOfToken(
            "api://$pdlClientId/.default",
            navidentTokenFromFagmodul
        )
        logger.debug("PDL token on Behalf: $accessToken")

        return PdlTokenImp(accessToken)
    }

    fun getToken(tokenValidationContextHolder: TokenValidationContextHolder): JwtToken {
        val context = tokenValidationContextHolder.tokenValidationContext
        if(context.issuers.isEmpty())
            throw RuntimeException("No issuer found in context")
        val issuer = context.issuers.first()

        return context.getJwtToken(issuer)
    }
}

