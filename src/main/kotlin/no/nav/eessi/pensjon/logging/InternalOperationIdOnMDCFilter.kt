package no.nav.eessi.pensjon.logging

import org.slf4j.MDC
import org.springframework.stereotype.Component
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import java.util.UUID
import java.io.IOException
import javax.servlet.ServletException

const val INTERNAL_CORRELATION_ID_KEY = "x_operation_id"

/**
 * Adds an internal correlation id to the MDC (Mapped Diagnostic Context)
 */
@Component
class InternalOperationIdOnMDCFilter : Filter {

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        MDC.put(INTERNAL_CORRELATION_ID_KEY, UUID.randomUUID().toString())
        try {
            chain.doFilter(request, response)
        } finally {
            MDC.remove(INTERNAL_CORRELATION_ID_KEY)
        }
    }
}