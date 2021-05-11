package no.nav.eessi.pensjon.services.kodeverk

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.typeRefs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.nio.file.Files
import java.nio.file.Paths

class KodeverkClientTest {

    var mockrestTemplate: RestTemplate = mockk()

    var kodeverkClient: KodeverkClient = mockk()

    @BeforeEach
    fun setup() {
        kodeverkClient = KodeverkClient(mockrestTemplate, "eessi-fagmodul")
        kodeverkClient.initMetrics()
    }

    @Test
    fun hentingavIso2landkodevedbrukAvlandkode2() {
        val expected = "SE"
        val landkode3 = "SWE"

        val mockResponseEntityISO3 = createResponseEntityFromJsonFile("src/test/resources/json/kodeverk/landkoderSammensattIso2.json")
        every { mockrestTemplate.exchange( eq("/api/v1/hierarki/LandkoderSammensattISO2/noder"), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns mockResponseEntityISO3

        val actual = kodeverkClient.finnLandkode(landkode3)
        Assertions.assertEquals(expected, actual)
    }

    @Test
    fun hentingavIso2landkodevedbrukAvlandkode3() {
        val expected = "BMU"
        val landkode2 = "BM"

        val mockResponseEntityISO3 = createResponseEntityFromJsonFile("src/test/resources/json/kodeverk/landkoderSammensattIso2.json")

        every { mockrestTemplate.exchange( eq("/api/v1/hierarki/LandkoderSammensattISO2/noder"), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns mockResponseEntityISO3

        val actual = kodeverkClient.finnLandkode(landkode2)

        Assertions.assertEquals(expected, actual)
    }

    @Test
    fun hentingavIso2landkodevedbrukAvlandkode3part2() {
        val expected = "ALB"
        val landkode2 = "AL"

        val mockResponseEntityISO3 = createResponseEntityFromJsonFile("src/test/resources/json/kodeverk/landkoderSammensattIso2.json")
        every { mockrestTemplate.exchange( eq("/api/v1/hierarki/LandkoderSammensattISO2/noder"), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns mockResponseEntityISO3
        val actual = kodeverkClient.finnLandkode(landkode2)

        Assertions.assertEquals(expected, actual)
    }


    @Test
    fun testerLankodeMed2Siffer() {
        val mockResponseEntityISO3 = createResponseEntityFromJsonFile("src/test/resources/json/kodeverk/landkoderSammensattIso2.json")

        every { mockrestTemplate.exchange( eq("/api/v1/hierarki/LandkoderSammensattISO2/noder"), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns mockResponseEntityISO3

        val actual = kodeverkClient.hentLandkoderAlpha2()

        Assertions.assertEquals("ZW", actual.last())
        Assertions.assertEquals(249, actual.size)
    }

    @Test
    fun henteAlleLandkoderReturnererAlleLandkoder() {
        val mockResponseEntityISO3 = createResponseEntityFromJsonFile("src/test/resources/json/kodeverk/landkoderSammensattIso2.json")
        every { mockrestTemplate.exchange( eq("/api/v1/hierarki/LandkoderSammensattISO2/noder"), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns mockResponseEntityISO3

        val json = kodeverkClient.hentAlleLandkoder()

        val list = mapJsonToAny(json, typeRefs<List<Landkode>>())

        Assertions.assertEquals(249, list.size)

        Assertions.assertEquals("AD", list.first().landkode2)
        Assertions.assertEquals("AND", list.first().landkode3)

    }

    @Test
    fun hentingavIso2landkodevedbrukAvlandkode3FeilerMedNull() {
        val landkode2 = "BMUL"

        val mockResponseEntityISO3 = createResponseEntityFromJsonFile("src/test/resources/json/kodeverk/landkoderSammensattIso2.json")
        every { mockrestTemplate.exchange( eq("/api/v1/hierarki/LandkoderSammensattISO2/noder"), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns mockResponseEntityISO3

        val exception  = assertThrows<IllegalArgumentException> {
            kodeverkClient.finnLandkode(landkode2)
        }
        Assertions.assertEquals("Ugyldig landkode: BMUL", exception.message)
    }

    private fun createResponseEntityFromJsonFile(filePath: String, httpStatus: HttpStatus = HttpStatus.OK): ResponseEntity<String> {
        val mockResponseString = String(Files.readAllBytes(Paths.get(filePath)))
        return ResponseEntity(mockResponseString, httpStatus)
    }
}
