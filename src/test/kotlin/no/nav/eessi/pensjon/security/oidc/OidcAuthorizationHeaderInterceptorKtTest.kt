package no.nav.eessi.pensjon.security.oidc

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

@RunWith(MockitoJUnitRunner::class)
class OidcAuthorizationHeaderInterceptorKtTest {

    private val allRequestContextHolders = generateMockContextHolder(listOf("isso","pesys","oidc"))
    private lateinit var authInterceptor: OidcAuthorizationHeaderInterceptor

    @Test
    fun `gitt magic mulitple issuers returner pesysIdToken token`() {
        authInterceptor = OidcAuthorizationHeaderInterceptor(allRequestContextHolders)

        val tokenfromIssuer = authInterceptor.getIdTokenFromIssuer(allRequestContextHolders)
        assertEquals("pesysIdToken", tokenfromIssuer)
    }

    @Test
    fun `gitt magic mulitple issuers returner issoIdToken token`() {
        val allMagicRequestContextHoldersIsso = generateMockContextHolder(listOf("isso","pesys","oidc"),true)

        authInterceptor = OidcAuthorizationHeaderInterceptor(allMagicRequestContextHoldersIsso)

        val tokenfromIssuer = authInterceptor.getIdTokenFromIssuer(allMagicRequestContextHoldersIsso)
        assertEquals("issoIdToken", tokenfromIssuer)
    }

    @Test
    fun `gitt magic mulitple issuers returner pesys`() {
        val allMagicRequestContextHoldersIsso = generateMockContextHolder(listOf("pesys"))
        authInterceptor = OidcAuthorizationHeaderInterceptor(allMagicRequestContextHoldersIsso)

        val tokenfromIssuer = authInterceptor.getIdTokenFromIssuer(allMagicRequestContextHoldersIsso)
        assertEquals("pesysIdToken", tokenfromIssuer)
    }

    @Test
    fun `gitt magic mulitple issuers returner oidc`() {
        val allMagicRequestContextHoldersIsso = generateMockContextHolder(listOf("oidc","isso"))
        authInterceptor = OidcAuthorizationHeaderInterceptor(allMagicRequestContextHoldersIsso)

        val tokenfromIssuer = authInterceptor.getIdTokenFromIssuer(allMagicRequestContextHoldersIsso)
        assertEquals("oidcIdToken", tokenfromIssuer)
    }

    @Test
    fun `gitt magic mulitple issuers returner isso had longest exp`() {
        val allMagicRequestContextHoldersIsso = generateMockContextHolder(listOf("oidc","isso"), true)
        authInterceptor = OidcAuthorizationHeaderInterceptor(allMagicRequestContextHoldersIsso)

        val tokenfromIssuer = authInterceptor.getIdTokenFromIssuer(allMagicRequestContextHoldersIsso)
        assertEquals("issoIdToken", tokenfromIssuer)
    }

    fun generateMockContextHolder(issuer: List<String>): OIDCRequestContextHolder {
        return generateMockContextHolder(issuer, false)
    }

    fun generateMockContextHolder(issuerList: List<String>, doIsso: Boolean): OIDCRequestContextHolder {
        val oidcContext = OIDCValidationContext()
        val oidcContextHolder = MockOIDCRequestContextHolder()

        val claimSetNormal: JWTClaimsSet = JWTClaimsSet.parse(FileUtils.readFileToString(File("src/test/resources/json/jwtExample.json"), Charset.forName("UTF-8")))
        val claimSetISSO: JWTClaimsSet = JWTClaimsSet.parse(FileUtils.readFileToString(File("src/test/resources/json/jwtISSOExample.json"), Charset.forName("UTF-8")))

        issuerList.iterator().forEach {
            val issuer = it
            val idToken = issuer + "IdToken"
            val tokenContext = TokenContext(issuer, idToken)
            val claimSet = generateMockClaimSet(it, doIsso, claimSetNormal, claimSetISSO)
            val jwt = PlainJWT(claimSet)
            oidcContext.addValidatedToken(issuer, tokenContext, OIDCClaims(jwt))
        }
        oidcContextHolder.setOIDCValidationContext(oidcContext)
        return oidcContextHolder
    }

    fun generateMockClaimSet(issuer: String, doIsso: Boolean, normalClaim: JWTClaimsSet, issoClaim: JWTClaimsSet): JWTClaimsSet {
        return if (doIsso && issuer == "isso") {
            issoClaim
        } else {
            normalClaim
        }
    }
}