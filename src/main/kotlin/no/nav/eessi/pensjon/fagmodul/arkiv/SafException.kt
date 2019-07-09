package no.nav.eessi.pensjon.fagmodul.arkiv

import org.springframework.http.HttpStatus

class SafException(message: String, var httpStatus: HttpStatus) : RuntimeException(message)