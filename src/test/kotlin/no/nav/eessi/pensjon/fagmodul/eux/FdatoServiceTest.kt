package no.nav.eessi.pensjon.fagmodul.eux

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import no.nav.eessi.pensjon.fagmodul.sedmodel.*
import no.nav.eessi.pensjon.utils.toJson
import no.nav.eessi.pensjon.utils.validateJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.nio.file.Files
import java.nio.file.Paths

@ExtendWith(MockitoExtension::class)
class FdatoServiceTest {

    private lateinit var mockEuxService: EuxService

    @Mock
    private lateinit var mockEuxrestTemplate: RestTemplate

    private lateinit var service:  FdatoService

    @BeforeEach
    fun setup() {
        mockEuxService = EuxService(mockEuxrestTemplate)
        service = FdatoService(mockEuxService)

    }

    fun mockSedResponse(sedJson: String): ResponseEntity<String> {
        return ResponseEntity(sedJson, HttpStatus.OK)
    }

    @Test
    fun `Calling getFDatoFromSed returns exception when foedselsdato is not found` () {
        val euxCaseId = "123456"
        val bucPath = "src/test/resources/json/buc/buc-158123_v4.1.json"
        val bucJson = String(Files.readAllBytes(Paths.get(bucPath)))
        assertTrue(validateJson(bucJson))

        val sed = SED("P2000")
        sed.nav = Nav(bruker = Bruker(person = Person(fornavn = "Dummy")))
        val mockSedJson = sed.toJson()

        val bucResponse: ResponseEntity<String> = ResponseEntity(bucJson, HttpStatus.OK)
        doReturn(bucResponse)
                .whenever(mockEuxrestTemplate)
                .exchange(eq("/buc/$euxCaseId"), eq(HttpMethod.GET), eq(null), eq(String::class.java))
        doReturn(mockSedResponse(mockSedJson))
                .whenever(mockEuxrestTemplate)
                .exchange(ArgumentMatchers.contains("buc/$euxCaseId/sed/"), eq(HttpMethod.GET), eq(null), eq(String::class.java))

        org.junit.jupiter.api.assertThrows<IkkeFunnetException> {
            service.getFDatoFromSed(euxCaseId, "P_BUC_01")
        }
    }

    @Test
    fun `Calling getFDatoFromSed   returns valid resultset on BUC_01` () {
        val euxCaseId = "123456"
        val bucPath = "src/test/resources/json/buc/buc-158123_2_v4.1.json"
        val bucJson = String(Files.readAllBytes(Paths.get(bucPath)))
        assertTrue(validateJson(bucJson))
        val bucResponse: ResponseEntity<String> = ResponseEntity(bucJson, HttpStatus.OK)
        doReturn(bucResponse)
                .whenever(mockEuxrestTemplate)
                .exchange(eq("/buc/$euxCaseId"), eq(HttpMethod.GET), eq(null), eq(String::class.java))
        doReturn(mockSedResponse(getTestJsonFile("P2000-NAV.json")))
                .whenever(mockEuxrestTemplate)
                .exchange(ArgumentMatchers.contains("buc/$euxCaseId/sed/"), eq(HttpMethod.GET), eq(null), eq(String::class.java))

        assertEquals("1980-01-01", service.getFDatoFromSed(euxCaseId,"P_BUC_01"))
    }

    @Test
    fun `Calling getFDatoFromSed returns valid resultset on BUC_06` () {
        val euxCaseId = "123456"
        val bucPath = "src/test/resources/json/buc/buc-175254_noX005_v4.1.json"
        val bucJson = String(Files.readAllBytes(Paths.get(bucPath)))
        assertTrue(validateJson(bucJson))
        val bucResponse: ResponseEntity<String> = ResponseEntity(bucJson, HttpStatus.OK)
        doReturn(bucResponse)
                .whenever(mockEuxrestTemplate)
                .exchange(eq("/buc/$euxCaseId"), eq(HttpMethod.GET), eq(null), eq(String::class.java))
        doReturn(mockSedResponse(getTestJsonFile("P10000-03Barn-NAV.json")))
                .whenever(mockEuxrestTemplate)
                .exchange(ArgumentMatchers.contains("buc/$euxCaseId/sed/"), eq(HttpMethod.GET), eq(null), eq(String::class.java))

        assertEquals("1948-06-28", service.getFDatoFromSed(euxCaseId,"P_BUC_06"))
    }

    @Test
    fun `Calling getFDatoFromSed returns exception when seddocumentId is not found` () {
        val euxCaseId = "123456"
        val bucPath = "src/test/resources/json/buc/buc-fb-152955.json"
        val bucJson = String(Files.readAllBytes(Paths.get(bucPath)))
        assertTrue(validateJson(bucJson))
        val bucResponse: ResponseEntity<String> = ResponseEntity(bucJson, HttpStatus.OK)
        doReturn(bucResponse)
                .whenever(mockEuxrestTemplate)
                .exchange(eq("/buc/$euxCaseId"), eq(HttpMethod.GET), eq(null), eq(String::class.java))

        org.junit.jupiter.api.assertThrows<IkkeFunnetException> {
            service.getFDatoFromSed(euxCaseId, "P_BUC_03")
        }
    }

}