package no.nav.eessi.eessifagmodul.domian

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason ="Invalid Request")
class RequestException(melding : String) : RuntimeException(melding)
