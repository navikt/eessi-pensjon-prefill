package no.nav.eessi.pensjon.api.person

import io.swagger.annotations.ApiOperation
import no.nav.eessi.pensjon.logging.AuditLogger
import no.nav.eessi.pensjon.services.aktoerregister.AktoerregisterService
import no.nav.eessi.pensjon.services.personv3.PersonV3Service
import no.nav.eessi.pensjon.metrics.counter
import no.nav.eessi.pensjon.services.personv3.PersonV3IkkeFunnetException
import no.nav.eessi.pensjon.services.personv3.PersonV3SikkerhetsbegrensningException
import no.nav.eessi.pensjon.services.aktoerregister.AktoerregisterException
import no.nav.eessi.pensjon.services.aktoerregister.AktoerregisterIkkeFunnetException
import no.nav.security.oidc.api.Protected
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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
                       private val personService: PersonV3Service,
                       private val auditLogger: AuditLogger) {

    private val logger = LoggerFactory.getLogger(PersonController::class.java)

    @ApiOperation("henter ut personinformasjon for en aktørId")
    @GetMapping("/person/{aktoerid}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getPerson(@PathVariable("aktoerid", required = true) aktoerid: String): ResponseEntity<Any> {
            auditLogger.log("/person/{$aktoerid}", "getPerson")
            return hentPerson(aktoerid)
    }

    @ApiOperation("henter ut navn for en aktørId")
    @GetMapping("/personinfo/{aktoerid}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getNameOnly(@PathVariable("aktoerid", required = true) aktoerid: String): ResponseEntity<Any> {
        auditLogger.log("/personinfo/{$aktoerid}", "getNameOnly")
        return hentPerson(aktoerid) {
            Personinformasjon(it.person.personnavn.sammensattNavn,
            it.person.personnavn.fornavn,
            it.person.personnavn.mellomnavn,
            it.person.personnavn.etternavn)
        }
    }

    private fun hentPerson(aktoerid: String, transform: (HentPersonResponse) -> Any = { it }): ResponseEntity<Any> {
            return try {
                logger.info("Henter personinformasjon for aktørId")
                val norskIdent: String = aktoerregisterService.hentGjeldendeNorskIdentForAktorId(aktoerid)
                val result = personService.hentPerson(norskIdent)
                val payload = transform(result)
                counter("eessipensjon_fagmodul.personinfo", "vellykkede").increment()
                ResponseEntity.ok(payload)
            } catch (are: AktoerregisterException) {
                logger.error("Kall til Akørregisteret feilet på grunn av: " + are.message)
                counter("eessipensjon_fagmodul.personinfo", "feilede").increment()
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(AktoerregisterException::class.simpleName)
            } catch (arife: AktoerregisterIkkeFunnetException) {
                logger.error("Kall til Akørregisteret feilet på grunn av: " + arife.message)
                counter("eessipensjon_fagmodul.personinfo", "feilede").increment()
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(AktoerregisterIkkeFunnetException::class.simpleName)
            } catch (sbe: PersonV3SikkerhetsbegrensningException) {
                logger.error("Kall til PersonV3 med feilet på grunn av sikkerhetsbegrensning")
                counter("eessipensjon_fagmodul.personinfo", "feilede").increment()
                ResponseEntity.status(HttpStatus.FORBIDDEN).body(PersonV3SikkerhetsbegrensningException::class.simpleName)
            } catch (ife: PersonV3IkkeFunnetException) {
                logger.error("Kall til PersonV3 feilet på grunn av person ikke funnet")
                counter("eessipensjon_fagmodul.personinfo", "feilede").increment()
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(PersonV3IkkeFunnetException::class.simpleName)
            }
    }
    /**
     * Personinformasjon fra TPS ( PersonV3 )
     */
    data class Personinformasjon(var fulltNavn: String? = null,
                                 var fornavn: String? = null,
                                 var mellomnavn: String? = null,
                                 var etternavn: String? = null)
}
