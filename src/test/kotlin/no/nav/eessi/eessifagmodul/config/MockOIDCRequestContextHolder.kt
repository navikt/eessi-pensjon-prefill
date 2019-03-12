package no.nav.eessi.eessifagmodul.config

import no.nav.security.oidc.context.OIDCRequestContextHolder
import no.nav.security.oidc.context.OIDCValidationContext

class MockOIDCRequestContextHolder : OIDCRequestContextHolder {

    private lateinit var oidcValidationContext : OIDCValidationContext

    override fun setOIDCValidationContext(oidcValidationContext: OIDCValidationContext?) {
        this.oidcValidationContext = oidcValidationContext!!
    }

    override fun getRequestAttribute(name: String?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setRequestAttribute(name: String?, value: Any?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOIDCValidationContext(): OIDCValidationContext {
        return this.oidcValidationContext
    }
}