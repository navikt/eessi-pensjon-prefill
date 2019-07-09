package no.nav.eessi.pensjon.services.arkiv

import org.springframework.http.HttpStatus

class SafException(message: String, var httpStatus: HttpStatus) : RuntimeException(message)