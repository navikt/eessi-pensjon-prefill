package no.nav.eessi.eessifagmodul.arkiv

import org.springframework.http.HttpStatus

class SafException(message: String, var httpStatus: HttpStatus) : RuntimeException(message)