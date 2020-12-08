package no.nav.eessi.pensjon.fagmodul.eux

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.fagmodul.eux.basismodel.BucSedResponse
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.security.sts.STSService
import org.aspectj.lang.JoinPoint
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestTemplate
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals


@SpringBootTest(classes = [UnsecuredWebMvcTestLauncher::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["unsecured-webmvctest"])
@AutoConfigureMockMvc
class EuxClientIntegrationTest {

    @MockBean
    lateinit var euxKlientInterceptorHandler: EuxKlientInterceptorHandler

    @MockBean
    lateinit var stsService: STSService

    @MockBean(name = "euxOidcRestTemplate")
    lateinit var restEuxTemplate: RestTemplate

    @Autowired
    lateinit var euxClient: EuxKlient

    private val jpCaptor = argumentCaptor<JoinPoint>()
    private val responseCaptor = argumentCaptor<BucSedResponse>()
    private val metricsHelper: MetricsHelper = MetricsHelper(SimpleMeterRegistry())

    @Test
    fun `Starter eux client og kaller opprettSed`() {
        val euxCaseId = "12345"
        val dokumentID = "d1782883dc424a10868c7878bd475a21"
        mockOpprettSed(dokumentID)
        euxClient.opprettSed(p2000Sed(), euxCaseId, metricsHelper.init("opprettSed"), "feil")
        Mockito.verify(euxKlientInterceptorHandler).logOpprettSed(jpCaptor.capture(), responseCaptor.capture())
        assertEquals(euxCaseId, (responseCaptor.firstValue).caseId)
        assertEquals("d1782883dc424a10868c7878bd475a21", (responseCaptor.firstValue).documentId)
    }

    @Test
    fun `Starter eux client og kaller opprettSvarSed`() {
        val euxCaseId = "12345"
        val parentId = "133243243242"
        mockOpprettSvarSed()
        euxClient.opprettSvarSed(p2000Sed(), euxCaseId,  "feil", parentId, metricsHelper.init("opprettSvarSed"))
        Mockito.verify(euxKlientInterceptorHandler).logOpprettSvarSed(jpCaptor.capture(), responseCaptor.capture())
        assertEquals(euxCaseId, (responseCaptor.firstValue).caseId)
    }

    private fun p2000Sed(): String {
        val filepath = "src/test/resources/json/nav/P2000-NAV.json"
        val json = String(Files.readAllBytes(Paths.get(filepath)))
        return json
    }

    private fun mockOpprettSvarSed() {
        doReturn(ResponseEntity.ok().body("bah")).whenever(restEuxTemplate).postForEntity(
            any<String>(),
            any(),
            eq(String::class.java)
        )
    }

    private fun mockOpprettSed(dokumentId  : String) {
        doReturn(ResponseEntity.ok().body(dokumentId)).whenever(restEuxTemplate).postForEntity(
            any<String>(),
            any(),
            eq(String::class.java),
            any<String>(),
            any<String>()
        )
    }
}