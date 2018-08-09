package no.nav.eessi.eessifagmodul.models

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

//kaster når peson ikke finnes (aktorid til pinnr)
@ResponseStatus(value = HttpStatus.NOT_FOUND)
class PersonIkkeFunnetException(message: String, exception: Exception): Exception(message, exception)

//kastes dersom muligeaksjoner ikke finner 'update' på valgt sed i rinanr
@ResponseStatus(value = HttpStatus.NOT_FOUND)
class SedDokumentIkkeOpprettetException(message: String): Exception(message)

//kastes dersom muligeaksjoner ikke finner 'create' på valgt sed i rnanr
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class SedDokumentIkkeGyldigException(message: String): Exception(message)

//kastes når 'opprettbucogsed' ikke returnerer rinanr
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
class RinaCasenrIkkeMottattException(message: String): Exception(message)

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class IkkeGyldigKallException(message: String) : IllegalArgumentException(message)