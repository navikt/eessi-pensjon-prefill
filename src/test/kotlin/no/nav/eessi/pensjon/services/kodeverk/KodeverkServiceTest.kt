package no.nav.eessi.pensjon.services.kodeverk

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.nio.file.Files
import java.nio.file.Paths

@ExtendWith(MockitoExtension::class)
class KodeverkServiceTest {

    @Mock
    private lateinit var mockrestTemplate: RestTemplate

    lateinit var kodeverkService: KodeverkService

    @BeforeEach
    fun setup() {


        kodeverkService = KodeverkService(mockrestTemplate, "eessi-fagmodul")
    }

    @Test
    fun hentingavIso2landkodevedbrukAvlandkode2() {
        val expected = "SE"
        val landkode3 = "SWE"

        val mockResponseEntityISO3 = createResponseEntityFromJsonFile("src/test/resources/json/kodeverk/landkoderSammensattIso2.json")
        doReturn(mockResponseEntityISO3)
                .whenever(mockrestTemplate)
                .exchange(
                        eq("api/v1/hierarki/LandkoderSammensattISO2/noder"),
                        any<HttpMethod>(),
                        any<HttpEntity<Unit>>(),
                        eq(String::class.java)
                )
        val actual = kodeverkService.finnLandkode2(landkode3)
        Assertions.assertEquals(expected, actual)

    }

    @Test
    fun hentingavIso2landkodevedbrukAvlandkode3() {
        val expected = "BMU"
        val landkode2 = "BM"

        val mockResponseEntityISO3 = createResponseEntityFromJsonFile("src/test/resources/json/kodeverk/landkoderSammensattIso2.json")
        doReturn(mockResponseEntityISO3)
                .whenever(mockrestTemplate)
                .exchange(
                        eq("api/v1/hierarki/LandkoderSammensattISO2/noder"),
                        any<HttpMethod>(),
                        any<HttpEntity<Unit>>(),
                        eq(String::class.java)
                )

        val actual = kodeverkService.finnLandkode3(landkode2)

        Assertions.assertEquals(expected, actual)

    }

    @Test
    fun hentingavIso2landkodevedbrukAvlandkode3part2() {
        val expected = "ALB"
        val landkode2 = "AL"

        val mockResponseEntityISO3 = createResponseEntityFromJsonFile("src/test/resources/json/kodeverk/landkoderSammensattIso2.json")
        doReturn(mockResponseEntityISO3)
                .whenever(mockrestTemplate)
                .exchange(
                        eq("api/v1/hierarki/LandkoderSammensattISO2/noder"),
                        any<HttpMethod>(),
                        any<HttpEntity<Unit>>(),
                        eq(String::class.java)
                )

        val actual = kodeverkService.finnLandkode3(landkode2)

        Assertions.assertEquals(expected, actual)

    }


    @Test
    fun hentingavIso2landkodevedbrukAvlandkode3FeilerMedNull() {
        val expected = null
        val landkode2 = "BMUL"

        val mockResponseEntityISO3 = createResponseEntityFromJsonFile("src/test/resources/json/kodeverk/landkoderSammensattIso2.json")
        doReturn(mockResponseEntityISO3)
                .whenever(mockrestTemplate)
                .exchange(
                        eq("api/v1/hierarki/LandkoderSammensattISO2/noder"),
                        any<HttpMethod>(),
                        any<HttpEntity<Unit>>(),
                        eq(String::class.java)
                )

        val actual = kodeverkService.finnLandkode3(landkode2)

        Assertions.assertEquals(expected, actual)

    }


    private fun createResponseEntityFromJsonFile(filePath: String, httpStatus: HttpStatus = HttpStatus.OK): ResponseEntity<String> {
        val mockResponseString = String(Files.readAllBytes(Paths.get(filePath)))
        return ResponseEntity(mockResponseString, httpStatus)
    }


}