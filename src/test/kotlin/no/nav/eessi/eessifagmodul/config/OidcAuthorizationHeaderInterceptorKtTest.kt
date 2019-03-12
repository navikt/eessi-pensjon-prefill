package no.nav.eessi.eessifagmodul.config

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.PlainJWT
import no.nav.security.oidc.context.OIDCClaims
import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.security.oidc.context.OIDCValidationContext
import no.nav.security.oidc.context.TokenContext
import org.junit.Test
import org.apache.commons.io.FileUtils
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.io.File
import java.nio.charset.Charset

@RunWith(MockitoJUnitRunner::class)
class OidcAuthorizationHeaderInterceptorKtTest {

    private val pesysRequestContextHolder = generateMockContextHolder(listOf("pesys", "oidc"))
    private val oidcRequestContextHolder = generateMockContextHolder(listOf("pesys"))

    @Test fun `gitt innlogget med issuer oidc når leter etter token så returner gyldig pesys token`() {
        assertEquals("oidcIdToken", getIdTokenFromIssuer(pesysRequestContextHolder))
    }

    @Test fun `gitt liste med issuers uten oidc så returner pesys`() {
        assertEquals("pesysIdToken", getIdTokenFromIssuer(oidcRequestContextHolder))
    }

    fun generateMockContextHolder(issuer: List<String>): OIDCRequestContextHolder {
        val oidcContext = OIDCValidationContext()
        val oidcContextHolder = MockOIDCRequestContextHolder()

        issuer.iterator().forEach {
            val issuer = it
            val idToken = issuer + "IdToken"
            val tokenContext = TokenContext(issuer, idToken)
            val claimSet = JWTClaimsSet.parse(FileUtils.readFileToString(File("src/test/resources/json/jwtExample.json"), Charset.forName("UTF-8")))
            val jwt = PlainJWT(claimSet)
            oidcContext.addValidatedToken(issuer, tokenContext, OIDCClaims(jwt))
        }
        oidcContextHolder.setOIDCValidationContext(oidcContext)
        return oidcContextHolder
    }
}