package no.nav.eessi.pensjon.fagmodul.prefill

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.lang.IllegalArgumentException

interface Prefill<T> {

    fun prefill(prefillData: PrefillDataModel): T

    @Throws(ValidationException::class)
    fun validate(data: T) {}
}

@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY)
class ValidationException(message: String) : IllegalArgumentException(message)