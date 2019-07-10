package no.nav.eessi.pensjon.fagmodul.services

import no.nav.eessi.pensjon.fagmodul.models.SED
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.lang.IllegalArgumentException

class SedValidator {

    fun validateP2000(sed: SED) {
        when {
            sed.nav?.bruker?.person?.etternavn == null -> throw SedValidatorException("Etternavn mangler")
            sed.nav?.bruker?.person?.fornavn == null -> throw SedValidatorException("Fornavn mangler")
            sed.nav?.bruker?.person?.foedselsdato == null -> throw SedValidatorException("Fødseldsdato mangler")
            sed.nav?.bruker?.person?.kjoenn == null -> throw SedValidatorException("Kjønn mangler")
            sed.nav?.krav?.dato == null -> throw SedValidatorException("Kravdato mangler")
        }
    }

}

@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY)
class SedValidatorException(message: String) : IllegalArgumentException(message)