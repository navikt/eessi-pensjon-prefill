package no.nav.eessi.pensjon.logging

import no.nav.eessi.pensjon.logging.RequestIdOnMDCFilter.Companion.NAV_CALL_ID_HEADER
import no.nav.eessi.pensjon.logging.RequestIdOnMDCFilter.Companion.REQUEST_ID_HEADER
import no.nav.eessi.pensjon.logging.RequestIdOnMDCFilter.Companion.REQUEST_ID_MDC_KEY
import org.slf4j.MDC
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import java.util.*

class RequestIdHeaderInterceptor : ClientHttpRequestInterceptor {

    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        val requestId = MDC.get(REQUEST_ID_MDC_KEY) ?: UUID.randomUUID().toString()
        request.headers.add(REQUEST_ID_HEADER, requestId)
        request.headers.add(NAV_CALL_ID_HEADER, requestId)
        return execution.execute(request, body)
    }

}
