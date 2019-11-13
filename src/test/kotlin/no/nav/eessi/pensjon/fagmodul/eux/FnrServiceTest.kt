package no.nav.eessi.pensjon.fagmodul.eux

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import no.nav.eessi.pensjon.utils.validateJson
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
class FnrServiceTest {

    private lateinit var mockEuxService: EuxService

    @Mock
    private lateinit var mockEuxrestTemplate: RestTemplate

    private lateinit var service:  FnrService


    @BeforeEach
    fun setup() {
        mockEuxService = EuxService(mockEuxrestTemplate)
        service = FnrService(mockEuxService)
    }

    fun mockSedResponse(sedJson: String): ResponseEntity<String> {
        return ResponseEntity(sedJson, HttpStatus.OK)
    }

    fun getTestJsonFile(filename: String): String {
        val filepath = "src/test/resources/json/nav/${filename}"
        val json = String(Files.readAllBytes(Paths.get(filepath)))
        Assertions.assertTrue(validateJson(json))
        return json
    }

    @Test
    fun `filtrer norsk pin annenperson med rolle 01`()   {
        val mapper = jacksonObjectMapper()
        val p2000json = getTestJsonFile("P2000-NAV.json")
        Assertions.assertEquals(null, service.filterAnnenpersonPinNode(mapper.readTree(p2000json)))

        val p10000json = getTestJsonFile("P10000-01Gjenlevende-NAV.json")
        val expected = "287654321"
        val actual  = service.filterAnnenpersonPinNode(mapper.readTree(p10000json))
        Assertions.assertEquals(expected, actual)

    }

    @Test
    fun `letter igjennom beste Sed på valgt buc P2100 også P2000 etter norsk personnr`() {
        val mockEuxCaseID = "123123"
        val mock = listOf(Pair("04117b9f8374420e82a4d980a48df6b3","P2100"), Pair("04117b9f8374420e82a4d980a48df6b3","P2000"))

                doReturn(mockSedResponse(getTestJsonFile("P2100-PinDK-NAV.json")))
                .doReturn(mockSedResponse(getTestJsonFile("P2000-NAV.json")))
                .whenever(mockEuxrestTemplate)
                .exchange(ArgumentMatchers.contains(
                        "buc/$mockEuxCaseID/sed/")
                        , eq(HttpMethod.GET)
                        , eq(null)
                        , eq(String::class.java)
                )

        val actual = service.getFodselsnrFraSed(mockEuxCaseID, mock)
        val expected = "970970970"
        assertEquals(expected, actual)
    }

    @Test
    fun `letter igjennom beste Sed på valgt buc etter norsk personnr`() {
        val mockEuxCaseID = "123123"
        val mock = listOf(Pair("04117b9f8374420e82a4d980a48df6b3","P2100"),Pair("04117b9f8374420e82a4d980a48df6b3","P2100"),
                Pair("04117b9f8374420e82a4d980a48df6b3","P2100"), Pair("04117b9f8374420e82a4d980a48df6b3","P2100"),
                Pair("04117b9f8374420e82a4d980a48df6b3","P2000"),Pair("04117b9f8374420e82a4d980a48df6b3","P15000"))

        doReturn(mockSedResponse(getTestJsonFile("P2100-PinDK-NAV.json")))
                .doReturn(mockSedResponse(getTestJsonFile("P2100-PinDK-NAV.json")))
                .doReturn(mockSedResponse(getTestJsonFile("P2100-PinDK-NAV.json")))
                .doReturn(mockSedResponse(getTestJsonFile("P2100-PinDK-NAV.json")))
                .doReturn(mockSedResponse(getTestJsonFile("P2000-NAV.json")))
                .doReturn(mockSedResponse(getTestJsonFile("P15000-NAV.json")))
                .whenever(mockEuxrestTemplate)
                .exchange(ArgumentMatchers.contains(
                        "buc/$mockEuxCaseID/sed/")
                        , eq(HttpMethod.GET)
                        , eq(null)
                        , eq(String::class.java)
                )

        val actual = service.getFodselsnrFraSed(mockEuxCaseID, mock)
        val expected = "970970970"
        assertEquals(expected, actual)
    }

    @Test
    fun `letter igjennom beste Sed på valgt buc P15000 alder eller ufor etter norsk personnr`() {
        val mockEuxCaseID = "123123"
        val mock = listOf(Pair("04117b9f8374420e82a4d980a48df6b3","P2100"),Pair("04117b9f8374420e82a4d980a48df6b3","P15000"))

        doReturn(mockSedResponse(getTestJsonFile("P2100-PinDK-NAV.json")))
                .doReturn(mockSedResponse(getTestJsonFile("P15000-NAV.json")))
                .whenever(mockEuxrestTemplate)
                .exchange(ArgumentMatchers.contains(
                        "buc/$mockEuxCaseID/sed/")
                        , eq(HttpMethod.GET)
                        , eq(null)
                        , eq(String::class.java)
                )

        val actual = service.getFodselsnrFraSed(mockEuxCaseID, mock)
        val expected = "21712"
        assertEquals(expected, actual)
    }

    @Test
    fun `leter igjennom beste Sed paa valgt buc P15000 gjenlevende etter norsk personnr`() {
        val mockEuxCaseID = "123123"
        val mock = listOf(Pair("04117b9f8374420e82a4d980a48df6b3","P2100"),Pair("04117b9f8374420e82a4d980a48df6b3","P15000"))

        doReturn(mockSedResponse(getTestJsonFile("P2100-PinDK-NAV.json")))
                .doReturn(mockSedResponse(getTestJsonFile("P15000Gjennlevende-NAV.json")))
                .whenever(mockEuxrestTemplate)
                .exchange(ArgumentMatchers.contains(
                        "buc/$mockEuxCaseID/sed/")
                        , eq(HttpMethod.GET)
                        , eq(null)
                        , eq(String::class.java)
                )

        val actual = service.getFodselsnrFraSed(mockEuxCaseID, mock)
        val expected = "21712"
        assertEquals(expected, actual)
    }


    @Test
    fun `letter igjennom beste Sed på valgt buc P10000 annenperson etter norsk personnr`() {
        val mockEuxCaseID = "123123"
        val mock = listOf(Pair("04117b9f8374420e82a4d980a48df6b3","P2100"),Pair("04117b9f8374420e82a4d980a48df6b3","P10000"))

        doReturn(mockSedResponse(getTestJsonFile("P2100-PinDK-NAV.json")))
        .doReturn(mockSedResponse(getTestJsonFile("P10000-01Gjenlevende-NAV.json")))
        .whenever(mockEuxrestTemplate)
        .exchange(ArgumentMatchers.contains(
                "buc/$mockEuxCaseID/sed/")
                , eq(HttpMethod.GET)
                , eq(null)
                , eq(String::class.java)
        )

        val actual = service.getFodselsnrFraSed(mockEuxCaseID, mock)
        val expected = "287654321"
        assertEquals(expected, actual)
    }



    @Test
    fun `letter igjennom beste Sed på valgt buc P2100 etter norsk personnr feiler kaster exception`() {
        val mockEuxCaseID = "123123"
        val mock = listOf(Pair("04117b9f8374420e82a4d980a48df6b3","P2100"))

            doReturn(mockSedResponse(getTestJsonFile("P2100-PinDK-NAV.json")))
            .whenever(mockEuxrestTemplate)
            .exchange(ArgumentMatchers.contains(
                    "buc/$mockEuxCaseID/sed/")
                    , eq(HttpMethod.GET)
                    , eq(null)
                    , eq(String::class.java)
            )

            assertThrows<IkkeFunnetException> {
            service.getFodselsnrFraSed(mockEuxCaseID, mock)
        }
    }



}