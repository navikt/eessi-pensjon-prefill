package no.nav.eessi.eessifagmodul.models

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class IkkeGyldigKallException(message: String) : IllegalArgumentException(message)

