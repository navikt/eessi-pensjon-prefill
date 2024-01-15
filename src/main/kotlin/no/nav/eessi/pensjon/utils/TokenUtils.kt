package no.nav.eessi.pensjon.utils

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken


fun getToken(tokenValidationContextHolder: TokenValidationContextHolder): JwtToken {
    val context = tokenValidationContextHolder.getTokenValidationContext()
    if(context.issuers.isEmpty())
        throw RuntimeException("No issuer found in context")
    val issuer = context.issuers.first()

    return context.getJwtToken(issuer)!!
}