package no.nav.eessi.eessifagmodul.config

import no.nav.eessi.eessifagmodul.controllers.SedController
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import org.springframework.web.util.ContentCachingRequestWrapper
import java.util.stream.Collectors
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Component
class ControllerRequestInterceptor : HandlerInterceptorAdapter() {

    private val logger = LoggerFactory.getLogger(SedController::class.java)

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        //logger.info("Oppretter tidlig preHandler")
        val requestCacheWrapperObject = ContentCachingRequestWrapper(request)
        requestCacheWrapperObject.parameterMap
//        val ba = requestCacheWrapperObject.contentAsByteArray
//        val value = String(ba)
//
//        logger.info("Value : " + value)
//
        logRequest(request)
        return true
    }

    override fun afterCompletion(request: HttpServletRequest, response: HttpServletResponse, handler: Any, ex: Exception?) {
        //super.afterCompletion(request, response, handler, ex)
    }

    private fun logRequest(request: HttpServletRequest) {
        logger.info("\n===========================remote request begin==========================================\n"
                + "Method      : " + request.method + "\n"
                + "path        : " + request.pathTranslated + "\n"
                + "queryString : " + request.queryString + "\n"
                + "remoteHost  : " + request.remoteHost + "\n"
                + "remoteAddr  : " + request.remoteAddr + "\n"
                //+ "headers   : " + logHeaders(request) + "\n"
                //+ "postBody    : " + postBody(request) + "\n"
                + "==========================request end================================================")
    }

    private fun logHeaders(request: HttpServletRequest): String {
        val names = request.headerNames.toList()
        var header = ""
        names.forEach {
            header += request.getHeader(it) + "\n"
        }
        return header
    }

    private fun postBody(request: HttpServletRequest): String {
        if (request.method == "POST") {
            return request.reader.lines().collect(Collectors.joining(System.lineSeparator()))
        }
        return ""
    }


}