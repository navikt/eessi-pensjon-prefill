package no.nav.eessi.pensjon.logging

import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

class InternalOperationIdOnMDCFilterTest {

    @Test
    fun `we have an app-internal MDC-identifier for each request`() {
        val mockFilterChain = MDCCapturingMockFilterChain()
        InternalOperationIdOnMDCFilter().doFilter(MockHttpServletRequest(), MockHttpServletResponse(), mockFilterChain)

        assertTrue(mockFilterChain.capturedMDCKey(INTERNAL_CORRELATION_ID_KEY))
    }

    @Test
    fun `the app-internal MDC-identifier for is removed afterwards`() {
        InternalOperationIdOnMDCFilter().doFilter(MockHttpServletRequest(), MockHttpServletResponse(), MockFilterChain())

        assertNull(MDC.get(INTERNAL_CORRELATION_ID_KEY))
    }
}

class MDCCapturingMockFilterChain : FilterChain {
    private var contextMap: MutableMap<String, String>? = null

    override fun doFilter(request: ServletRequest?, response: ServletResponse?) {
        contextMap = MDC.getCopyOfContextMap()
    }

    fun capturedMDCKey(key: String ) = contextMap!!.containsKey(key)
}
