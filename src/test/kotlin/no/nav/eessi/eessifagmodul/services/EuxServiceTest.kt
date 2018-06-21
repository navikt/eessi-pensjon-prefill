package no.nav.eessi.eessifagmodul.services

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.utils.typeRef
import no.nav.eessi.eessifagmodul.utils.typeRefs
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.client.getForEntity
import java.time.LocalDate
import java.util.*


@RunWith(MockitoJUnitRunner::class)
class EuxServiceTest {

    private val logger: Logger by lazy { LoggerFactory.getLogger(EuxServiceTest::class.java) }

    lateinit var service: EuxService

    private val objMapper = jacksonObjectMapper()

    @Mock
    private lateinit var mockrestTemplate: RestTemplate

    @Before
    fun setup() {
        logger.debug("Starting tests.... ...")
        objMapper.enable(SerializationFeature.WRAP_ROOT_VALUE)
        service = EuxService(mockrestTemplate)
    }

    @Ignore("Not yet implemented")
    @Test(expected = Exception::class)
    fun getBuCNoEUSaknr() {
        service.getBuC("")
    }

    @Ignore("Not yet implemented")
    @Test
    fun getBuC() {
    }

    @Ignore("Not yet implemented")
    @Test(expected = RestClientException::class)
    fun sendSedForbidden403() {
        val data = "{\"timestamp\":\"2018-06-05T11:29:31.394+0000\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Forbidden\",\"path\":\"/cpi/SendSED\"}"
        createSendSEDmedFeilResponse(data, HttpStatus.FORBIDDEN)
    }

    @Ignore("Not yet implemented")
    @Test(expected = RestClientException::class)
    fun sendSedNoRINAFound() {
        val data = "{\"timestamp\":\"2018-06-05T11:29:31.394+0000\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Not Found\",\"path\":\"/cpi/SendSED\"}"
        createSendSEDmedFeilResponse(data, HttpStatus.NOT_FOUND)
    }

    private fun createSendSEDmedFeilResponse(data: String, status: HttpStatus) {
        val response: ResponseEntity<String> = ResponseEntity(data, status)
        whenever(mockrestTemplate.getForEntity<String>(anyString(), any())).thenReturn(response)

        val rinaSaksnr = "12132123"
        service.getMuligeAksjoner(rinaSaksnr)
    }

    @Ignore("Not yet implemented")
    @Test
    fun sendSed() {
        val response: ResponseEntity<String> = ResponseEntity("", HttpStatus.OK)
        whenever(mockrestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(typeRef<String>()))).thenReturn(response)

        val rinaSaksnr = "12132123"
        val korrelasjonID = "123123123-12312312-12312312"

        val status = service.sendSED(rinaSaksnr, korrelasjonID, "documentId")
        assertEquals(true, status)
        assertTrue("Skal komme hit", true)
    }

    @Ignore("Not yet implemented")
    @Test
    fun getBuCtypePerSektor() {
        val mockData = listOf(
                "P_BUC_01",
                "P_BUC_07",
                "P_BUC_02",
                "P_BUC_05",
                "P_BUC_06",
                "P_BUC_09")
        val respData = ResponseEntity(mockData, HttpStatus.OK)
        whenever(mockrestTemplate.exchange<List<String>>(anyString(), eq(HttpMethod.GET), any(), eq(typeRefs<List<String>>()))).thenReturn(respData)

        val data: List<String> = service.getBuCtypePerSektor()
        assertTrue(data.containsAll(mockData))
    }

    @Ignore("Not yet implemented")
    @Test
    fun getInstitusjoner() {
        val expected = listOf("NO:NAV02", "NO:DUMMY")
        val mockResponse = ResponseEntity(expected, HttpStatus.OK)
        whenever(mockrestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(typeRef<List<String>>()))).thenReturn(mockResponse)

        val data = service.getInstitusjoner("P_BUC_01", "NO")
        assertEquals(expected.size, data.size)
        assertTrue(expected.containsAll(data))
    }

    @Ignore("Not yet implemented")
    @Test
    fun getMuligeAksjoner() {
        val bucType = "FB_BUC_01"

        val data = "[\"FB_BUC_01\",\"FB_BUC_01\"]"
        val response: ResponseEntity<String> = ResponseEntity(data, HttpStatus.OK)
        whenever(mockrestTemplate.getForEntity<String>(anyString(), eq(typeRef<String>()))).thenReturn(response)

        val resultat = service.getMuligeAksjoner(bucType)

        assertEquals(2, resultat.size)
        assertEquals("FB_BUC_01", resultat.get(0))
        assertTrue("Skal komme hit", true)
    }

    @Ignore("Not yet implemented")
    @Test
    fun getAvailableSEDTypes() {
        val bucType = "P_BUC_01"
        val mockData = listOf("P2000", "P6000")
        val mockResponse = ResponseEntity(mockData, HttpStatus.OK)
        whenever(mockrestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(typeRef<List<String>>()))).thenReturn(mockResponse)

        val resultat = service.getAvailableSEDTypes(bucType)

        assertTrue(resultat.containsAll(mockData))
    }

    @Test
    fun createCaseAndDocument() {

        val fagSaknr = "EESSI-PEN-123"
        val mottaker = "DUMMY"
        val pensjon = genererMockData()
        val pensjonAsJson = objMapper.writeValueAsString(pensjon)
        val bucType = "P_BUC_01"
        val korrid = UUID.randomUUID()
        val vedleggType = ""

        //mock response and mock restTemplate
        val response: ResponseEntity<String> = ResponseEntity("Rina:12345", HttpStatus.OK)
        whenever(mockrestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String::class.java))).thenReturn(response)

        try {
            val data = service.createCaseAndDocument(pensjonAsJson, bucType, fagSaknr, mottaker, vedleggType, korrid.toString())
            assertTrue("Skal komme hit!", true)
            logger.info("Response: $data")
            assertNotNull(data)
            println("RINA NR?       : $data")
            println("KorrelasjonsID : $korrid")
            println("BuCType        : $bucType")
        } catch (ex: Exception) {
            fail("Skal ikke komme hit")
            logger.error(ex.message)
        }
    }

    private fun genererMockData(): Pensjon {
        return Pensjon(
                kjoeringsdato = LocalDate.now().toString(),
                sak = Sak(
                        type = "AP",
                        artikkel44 = "Artikkel44",
                        kravtyper = listOf(
                                Kravtype(datoFirst = LocalDate.now().toString()),
                                Kravtype(datoFirst = LocalDate.now().toString())
                        )
                ),
                vedtak = Vedtak(
                        basertPaa = "A",
                        avslag = Avslag(begrunnelse = "Ikke godkjent opphold", begrunnelseAnnen = "Ingen kake"),
                        beregninger = listOf(
                                Beregning(
                                        artikkel = "Beregning artikkel abcdØåæ",
                                        virkningsdato = LocalDate.now().toString(),
                                        periode = Periode(LocalDate.now().toString(), LocalDate.now().toString()),
                                        beloepNetto = BeloepNetto(beloep = "5432.50"),
                                        beloepBrutto = BeloepBrutto(beloep = "9542.50", ytelseskomponentGrunnpensjon = "1240.50", ytelseskomponentTilleggspensjon = "3292.20", ytelseskomponentAnnen = "194.50"),
                                        valuta = "SE",
                                        utbetalingshyppighet = "1",
                                        utbetalingshyppighetAnnen = "Annet")
                                ,
                                Beregning(
                                        artikkel = "Beregning artikkel efghÆåø",
                                        virkningsdato = LocalDate.now().toString(),
                                        periode = Periode(LocalDate.now().toString(), LocalDate.now().toString()),
                                        beloepNetto = BeloepNetto(beloep = "3432.50"),
                                        beloepBrutto = BeloepBrutto(beloep = "9542.50", ytelseskomponentGrunnpensjon = "1240.50", ytelseskomponentTilleggspensjon = "3292.20", ytelseskomponentAnnen = "194.50"),
                                        valuta = "EU",
                                        utbetalingshyppighet = "3",
                                        utbetalingshyppighetAnnen = "Skuddår")
                        ),
                        grunnlag = Grunnlag(meldlemskap = "Norge", opptjening = Opptjening(forsikredeAnnen = "Anders And"), framtidigtrygdetid = "Nåtid"),
                        opphor = Opphor(
                                verdi = "1000.50",
                                begrunnelse = "Begrunnelse",
                                annulleringdato = LocalDate.now().toString(),
                                dato = LocalDate.now().toString(),
                                utbetaling = Utbetaling(
                                        beloepBrutto = "10000",
                                        valuta = "NOK"
                                )
                        ),
                        reduksjoner = listOf(
                                Reduksjon(
                                        type = "1",
                                        arsak = Arsak(inntekt = "212323.50", inntektAnnen = "23223.50"),
                                        artikkeltype = "2")
                        ),
                        dato = LocalDate.now().toString(),
                        artikkel48 = "Vedtakk artikkel48"
                )
        )
    }
}

