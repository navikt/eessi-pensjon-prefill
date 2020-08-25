package no.nav.eessi.pensjon.fagmodul.api

import no.nav.eessi.pensjon.personoppslag.aktoerregister.AktoerId
import no.nav.eessi.pensjon.personoppslag.aktoerregister.AktoerregisterService
import no.nav.eessi.pensjon.personoppslag.aktoerregister.IdentGruppe
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

    fun hentFnrfraAktoerService(aktoerid: String?, aktoerService: AktoerregisterService): String {
        if (aktoerid.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Tom input-verdi")
        }
        return aktoerService.hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId(aktoerid))?.id
           ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "NorskIdent for aktoerId $aktoerid ikke funnet.")

    }
