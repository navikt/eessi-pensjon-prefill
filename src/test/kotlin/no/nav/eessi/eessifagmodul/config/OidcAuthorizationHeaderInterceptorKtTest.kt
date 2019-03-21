package no.nav.eessi.eessifagmodul.config

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.PlainJWT
import no.nav.security.oidc.context.OIDCClaims
import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.security.oidc.context.OIDCValidationContext
import no.nav.security.oidc.context.TokenContext
import org.apache.commons.io.FileUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.io.File
import java.nio.charset.Charset
import kotlin.test.assertFailsWith

@RunWith(MockitoJUnitRunner::class)
class OidcAuthorizationHeaderInterceptorKtTest {

    private val oidcRequestContextHolder = generateMockContextHolder(listOf("oidc"))
    private val pesysRequestContextHolder = generateMockContextHolder(listOf("pesys"))
    private val mulitpleRequestContextHolders = generateMockContextHolder(listOf("pesys", "oidc"))

    private lateinit var authInterceptor: OidcAuthorizationHeaderInterceptor
    private lateinit var authInterceptorIssuer: OidcAuthorizationHeaderInterceptorSelectIssuer

    @Test fun `gitt issuer oidc returner gyldig oidcIdToken`() {
        authInterceptor = OidcAuthorizationHeaderInterceptor(oidcRequestContextHolder)
        assertEquals("oidcIdToken", authInterceptor.getIdTokenFromIssuer(oidcRequestContextHolder))
    }

    @Test fun `gitt issuer pesys returner gyldig pesysIdToken`() {
        authInterceptor = OidcAuthorizationHeaderInterceptor(pesysRequestContextHolder)
        assertEquals("pesysIdToken", authInterceptor.getIdTokenFromIssuer(pesysRequestContextHolder))
    }

    @Test fun `gitt mulitple issuers returner RuntimeException`() {
        authInterceptor = OidcAuthorizationHeaderInterceptor(mulitpleRequestContextHolders)
        assertFailsWith(RuntimeException::class) { authInterceptor.getIdTokenFromIssuer(mulitpleRequestContextHolders) }
    }

    @Test
    fun `gitt mulitple issuers returner selected ussuer pesys`() {
        authInterceptorIssuer = OidcAuthorizationHeaderInterceptorSelectIssuer(mulitpleRequestContextHolders, "pesys")
        assertEquals("pesysIdToken", authInterceptorIssuer.getIdTokenFromSelectedIssuer(mulitpleRequestContextHolders, "pesys"))
    }

    @Test
    fun `gitt mulitple issuers returner selected ussuer oidc`() {
        authInterceptorIssuer = OidcAuthorizationHeaderInterceptorSelectIssuer(mulitpleRequestContextHolders, "oidc")
        assertEquals("oidcIdToken", authInterceptorIssuer.getIdTokenFromSelectedIssuer(mulitpleRequestContextHolders, "oidc"))
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