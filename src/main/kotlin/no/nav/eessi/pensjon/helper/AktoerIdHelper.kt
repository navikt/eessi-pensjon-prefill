package no.nav.eessi.pensjon.helper

import no.nav.eessi.pensjon.services.aktoerregister.AktoerregisterException
import no.nav.eessi.pensjon.services.aktoerregister.AktoerregisterService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.ResponseStatus

@Component
class AktoerIdHelper(private val aktoerregisterService: AktoerregisterService) {

    @Throws(AktoerregisterException::class, ManglerAktoerIdException::class)
    fun hentAktoerIdPin(aktorid: String?): String {
        if (aktorid.isNullOrBlank()) throw ManglerAktoerIdException("Mangler AktorId")
        return aktoerregisterService.hentGjeldendeNorskIdentForAktorId(aktorid)
    }
}

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class ManglerAktoerIdException(message: String) : IllegalArgumentException(message)
