package no.nav.eessi.eessifagmodul.controllers

import no.nav.eessi.eessifagmodul.domian.RequestException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import javax.servlet.http.HttpServletResponse

@ControllerAdvice
class ControllerExceptionHandler {

    @ExceptionHandler(RuntimeException::class)
    fun handleBadRequest(ex: Throwable, response: HttpServletResponse) {
        print("ControllerExceptionHandler handleBadRequest : $ex")
        response.sendError(HttpStatus.BAD_REQUEST.value(), ex.message)
    }

    @ExceptionHandler(RequestException::class)
    fun serverBadRequest(ex: Throwable, response: HttpServletResponse) {
        print("ControllerExceptionHandler serverBadRequest : $ex")
        response.sendError(HttpStatus.UNAUTHORIZED.value(), ex.message)
    }
}