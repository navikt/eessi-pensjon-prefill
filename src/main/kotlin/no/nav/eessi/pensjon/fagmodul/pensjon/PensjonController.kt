package no.nav.eessi.pensjon.fagmodul.pensjon

import io.swagger.annotations.ApiOperation
import no.nav.eessi.pensjon.fagmodul.pensjon.pensjonsinformasjon.IkkeFunnetException
import no.nav.eessi.pensjon.fagmodul.pensjon.pensjonsinformasjon.PensjonsinformasjonService
import no.nav.eessi.pensjon.fagmodul.person.AktoerIdHelper
import no.nav.eessi.pensjon.utils.errorBody
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.security.oidc.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Protected
@RestController
@RequestMapping("/pensjon")
class PensjonController(private val pensjonsinformasjonService: PensjonsinformasjonService, private val aktoerIdHelper: AktoerIdHelper) {

    @ApiOperation("Henter ut saktype knyttet til den valgte sakId og aktoerId")
    @GetMapping("/saktype/{sakId}/{aktoerId}")
    fun hentPensjonSakType(@PathVariable("sakId", required = true) sakId: String, @PathVariable("aktoerId", required = true) aktoerId: String): ResponseEntity<String>? {
        // FIXME This is a hack because Pesys uses the wrong identifier in some cases
        val fnr = if (isProbablyAnFnrSentAsAktoerId(aktoerId)) aktoerId else aktoerIdHelper.hentAktoerIdPin(aktoerId)

        //Pensjontype //= Pensjontype(sakId = "", sakType = "")
        try {
            var hentKunSakType = pensjonsinformasjonService.hentKunSakType(sakId, fnr)
            return ResponseEntity.ok().body(mapAnyToJson(hentKunSakType))
        } catch (ife: IkkeFunnetException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(ife.message!!))
        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(e.message!!))
        }
    }

    private fun isProbablyAnFnrSentAsAktoerId(aktorid: String) = aktorid.length == 11
}
