package no.nav.eessi.pensjon.api.person

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.swagger.annotations.ApiOperation
import no.nav.eessi.pensjon.logging.AuditLogger
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.services.aktoerregister.AktoerregisterException
import no.nav.eessi.pensjon.services.aktoerregister.AktoerregisterIkkeFunnetException
import no.nav.eessi.pensjon.services.aktoerregister.AktoerregisterService
import no.nav.eessi.pensjon.services.personv3.PersonV3IkkeFunnetException
import no.nav.eessi.pensjon.services.personv3.PersonV3Service
import no.nav.eessi.pensjon.services.personv3.PersonV3SikkerhetsbegrensningException
import no.nav.security.oidc.api.Protected
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

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
                       private val auditLogger: AuditLogger,
                       @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper(SimpleMeterRegistry())) {

    private val logger = LoggerFactory.getLogger(PersonController::class.java)

    @ApiOperation("henter ut personinformasjon for en aktørId")
    @GetMapping("/person/{aktoerid}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getPerson(@PathVariable("aktoerid", required = true) aktoerid: String): ResponseEntity<Any> {

        auditLogger.log("/person/{$aktoerid}", "getPerson")
        return metricsHelper.measure(MetricsHelper.MeterName.PersonControllerHentPerson) {
            hentPerson(aktoerid)
        }
    }

    @ApiOperation("henter ut navn for en aktørId")
    @GetMapping("/personinfo/{aktoerid}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getNameOnly(@PathVariable("aktoerid", required = true) aktoerid: String): ResponseEntity<Any> {

        auditLogger.log("/personinfo/{$aktoerid}", "getNameOnly")
        return metricsHelper.measure(MetricsHelper.MeterName.PersonControllerHentPersonNavn) {
            hentPerson(aktoerid) {
                Personinformasjon(it.person.personnavn.sammensattNavn,
                        it.person.personnavn.fornavn,
                        it.person.personnavn.mellomnavn,
                        it.person.personnavn.etternavn)
            }
        }
    }

    private fun hentPerson(aktoerid: String, transform: (HentPersonResponse) -> Any = { it }): ResponseEntity<Any> {
        return try {
            logger.info("Henter personinformasjon for aktørId")
            val norskIdent: String = aktoerregisterService.hentGjeldendeNorskIdentForAktorId(aktoerid)
            val result = personService.hentPerson(norskIdent)
            val payload = transform(result)
            ResponseEntity.ok(payload)
        } catch (are: AktoerregisterException) {
            logger.error("Kall til Aktørregisteret feilet på grunn av: " + are.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(AktoerregisterException::class.simpleName)
        } catch (arife: AktoerregisterIkkeFunnetException) {
            logger.error("Kall til Aktørregisteret feilet på grunn av: " + arife.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(AktoerregisterIkkeFunnetException::class.simpleName)
        } catch (sbe: PersonV3SikkerhetsbegrensningException) {
            logger.error("Kall til PersonV3 feilet på grunn av sikkerhetsbegrensning")
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(PersonV3SikkerhetsbegrensningException::class.simpleName)
        } catch (ife: PersonV3IkkeFunnetException) {
            logger.error("Kall til PersonV3 feilet siden personen ikke ble funnet")
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
