package no.nav.eessi.pensjon.api.person

import io.swagger.annotations.ApiOperation
import no.nav.eessi.pensjon.services.aktoerregister.AktoerregisterService
import no.nav.eessi.pensjon.services.personv3.PersonV3Service
import no.nav.eessi.pensjon.fagmodul.metrics.getCounter
import no.nav.eessi.pensjon.metrics.counter
import no.nav.eessi.pensjon.services.personv3.PersonV3IkkeFunnetException
import no.nav.eessi.pensjon.services.personv3.PersonV3SikkerhetsbegrensningException
import no.nav.eessi.pensjon.services.aktoerregister.AktoerregisterException
import no.nav.eessi.pensjon.services.aktoerregister.AktoerregisterIkkeFunnetException
import no.nav.security.oidc.api.Protected
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller for å kalle NAV interne registre
 *
 * @property aktoerregisterService
 * @property personService
 */
@Protected
@RestController
class PersonController(private val aktoerregisterService: AktoerregisterService,
                                private val personService: PersonV3Service) {

    private val logger = LoggerFactory.getLogger(PersonController::class.java)

    /**
     * Kaller AktørRegisteret , bytter aktørId mot Fnr/Dnr ,
     * deretter kalles PersonV3 hvor personinformasjon hentes
     *
     * @param aktoerid
     */
    @ApiOperation("henter ut personinformasjon for en aktørId")
    @GetMapping("/person/{aktoerid}")
    fun getDocument(@PathVariable("aktoerid", required = true) aktoerid: String): ResponseEntity<HentPersonResponse> {
        logger.info("Henter personinformasjon for aktørId")

        val norskIdent: String
        var personresp = HentPersonResponse()

        try {
            norskIdent = aktoerregisterService.hentGjeldendeNorskIdentForAktorId(aktoerid)
            personresp = personService.hentPerson(norskIdent)

        } catch (are: AktoerregisterException) {
            logger.error("Kall til Akørregisteret feilet på grunn av: " + are.message)
            counter("eessipensjon_fagmodul.personinfo", "feilede").increment()
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(AktoerregisterException::class.simpleName)
        } catch (arife: AktoerregisterIkkeFunnetException) {
            logger.error("Kall til Akørregisteret feilet på grunn av: " + arife.message)
            counter("eessipensjon_fagmodul.personinfo", "feilede").increment()
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(AktoerregisterException::class.simpleName)
        } catch (sbe: PersonV3SikkerhetsbegrensningException) {
            logger.error("Kall til PersonV3 med feilet på grunn av sikkerhetsbegrensning")
            counter("eessipensjon_fagmodul.personinfo", "feilede").increment()
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(PersonV3SikkerhetsbegrensningException::class.simpleName)
        } catch (ife: PersonV3IkkeFunnetException) {
            logger.error("Kall til PersonV3 feilet på grunn av person ikke funnet")
            counter("eessipensjon_fagmodul.personinfo", "feilede").increment()
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(PersonV3IkkeFunnetException::class.simpleName)
        }

        counter("eessipensjon_fagmodul.personinfo", "vellykkede").increment()
        return ResponseEntity.ok(personresp)
    }
}