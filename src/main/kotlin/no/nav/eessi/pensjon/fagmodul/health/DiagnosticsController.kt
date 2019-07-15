package no.nav.eessi.pensjon.fagmodul.health

import no.nav.eessi.pensjon.fagmodul.metrics.getCounter
import no.nav.eessi.pensjon.fagmodul.services.eux.EuxService
import no.nav.eessi.pensjon.security.sts.STSService
import no.nav.eessi.pensjon.services.pensjonsinformasjon.PensjonsinformasjonService
import no.nav.eessi.pensjon.services.personv3.PersonV3Service
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.typeRefs
import no.nav.security.oidc.api.Protected
import no.nav.security.oidc.api.Unprotected
import org.apache.http.HttpHeaders
import org.apache.http.client.fluent.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Instant
import javax.servlet.http.HttpServletRequest

@CrossOrigin
@RestController
@Unprotected
class DiagnosticsController(private val stsService: STSService) {

    private val logger = LoggerFactory.getLogger(DiagnosticsController::class.java)

    @Value("\${app.name}")
    lateinit var appName: String

    @Value("\${app.version}")
    private lateinit var appVersion: String


    @GetMapping("/ping")
    fun ping(): ResponseEntity<Unit> {
        return ResponseEntity.ok().build()
    }

    @GetMapping("/internal/selftest")
    fun selftest(): SelftestResult {
        val requestAttrib =  RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
        val sessionId = requestAttrib.sessionId
        val request = requestAttrib.request

        try {
            val fagurl = getLocalProtectedAddr(request)
            logger.debug("Kall til selftest fra ip: ${fagurl} på sessionid: $sessionId")

            val localProtectedUrl = "${fagurl}/internal/protected/selftest"
            logger.debug("Prøver å kontakte Url: $localProtectedUrl")

            val token = stsService.getSystemOidcToken()
            val response = Request
                    .Get(localProtectedUrl)
                    .setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .execute()
                    .returnContent()
                    .asString()

            logger.debug("Selftest passed response: $response")
            val selfTestlist = mapJsonToAny(response, typeRefs<List<Check>>())
            return SelftestResult(name = appName, version = appVersion, aggregateResult = selfTestlist.size,checks = selfTestlist)

        } catch (ex: Exception) {
            logger.debug("Selftest failed")
            logger.error("Feil ved Selftest", ex)
            throw Exception("Feiler ved selftest, ${ex.message}")
        }
    }

    private fun getLocalProtectedAddr(request: HttpServletRequest): String {
        logger.debug("Request URL : HTTP://${appName}")
        if (request.localPort == 8081) {
            return "http://localhost:8081"
        }
        return "http://$appName"
    }

    @GetMapping("/internal/isalive")
    fun isalive(): ResponseEntity<String> {
        return ResponseEntity.ok("Is alive")
    }

    @GetMapping("/internal/isready")
    fun isready(): ResponseEntity<String> {
        return ResponseEntity.ok("Is ready")
    }

}

@RestController
class DiagnosticsControllerProtected(private val personService: PersonV3Service, private val euxService: EuxService, private val penService: PensjonsinformasjonService) {

    private val logger = LoggerFactory.getLogger(DiagnosticsController::class.java)

    @Value("\${app.name}")
    lateinit var appName: String

    private val CORRECT_RESULTCOUNTER = 3

    @Protected
    @GetMapping("/internal/protected/selftest")
    fun selftestProtected(): List<Check> {
        val requestAttrib =  RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
        val sessionId = requestAttrib.sessionId
        val request = requestAttrib.request

        logger.debug("Kall til selftest fra ip: ${request.remoteAddr} på sessionid: $sessionId")

        val selfTestChecklist = mutableListOf<Check>()
        logger.debug("Selftest/ping TPS")
        selfTestChecklist.add(selfTestTps())
        logger.debug("Selftest/ping PESYS")
        selfTestChecklist.add(selfTestPesys())
        logger.debug("Selftest/ping EUX")
        selfTestChecklist.add(selfTestEux())

        var counter = 0
        for (item in selfTestChecklist)  {
            counter += item.result
        }
        logger.debug("Selftest/ping SELF")
        if (counter == CORRECT_RESULTCOUNTER) {
            selfTestChecklist.add(selfTestSelfOk())
        } else {
            selfTestChecklist.add(selfTestSelfFail())
        }

        logger.debug("Selftest passed")
        return selfTestChecklist.toList()
    }

    private fun selfTestSelfOk() = Check(appName,"Eessi-Pensjon-Fagmodul OK","",1)
    private fun selfTestSelfFail() = Check(appName,"Eessi-Pensjon-Fagmodul OK","selftest feilet",0)

    private fun selfTestEux(): Check {
        return try {
            euxService.pingEux()
            getCounter("SELFTESTEUXOK").increment()
            Check("Eux-Rina-Api","Eux Rina OK","",1)
        } catch (ex: Exception) {
            getCounter("SELFTESTEUXFEIL").increment()
            Check("Eux-Rina-Api","Eux Rina FAIL", ex.message!!,0)
        }
    }

    private fun selfTestPesys(): Check {
        return try {
            penService.doPing()
            getCounter("SELFTESTPESYSOK").increment()
            Check("Pesys Personinformasjon","Personinformasjon OK", "",1)
        } catch (ex: Exception) {
            getCounter("SELFTESTPESYSFEIL").increment()
            Check("Pesys Personinformasjon","Personinformasjon FAIL",ex.message!!,0)
        }
    }

    private fun selfTestTps(): Check {
        return try {
            personService.hentPersonPing()
            getCounter("SELFTESTTPSOK").increment()
            Check("PersonV3","TPS PersonV3 OK","",1)
        } catch (ex: Exception) {
            getCounter("SELFTESTTPSFEIL").increment()
            Check("PersonV3","TPS PersonV3 FAIL", ex.message!! , 0)
        }
    }
}

data class SelftestResult(
        val name: String,
        val version: String,
        val timestamp: Instant = Instant.now(),
        val aggregateResult: Int,
        val checks: List<Check>?
)

data class Check(
        val endpoint: String,
        val description: String,
        val errorMessage: String,
        val result: Int
)

