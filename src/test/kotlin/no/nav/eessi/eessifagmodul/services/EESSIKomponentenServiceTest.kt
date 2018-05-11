package no.nav.eessi.eessifagmodul.services

import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.web.client.RestTemplate
import java.util.*
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockitoAnnotations
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import kotlin.test.assertEquals

@SpringBootTest
//@RunWith(MockitoJUnitRunner::class)
@RunWith(SpringJUnit4ClassRunner::class)
@ActiveProfiles("test")
class EESSIKomponentenServiceTest {

    private val logger: Logger by lazy { LoggerFactory.getLogger(EESSIKomponentenServiceTest::class.java)}

//    @Bean
//    fun restTemplageMock(templateBuilder: RestTemplateBuilder): RestTemplate {
//        println("Mock template")
//        println("Mock template")
//        println("Mock template")
//        return templateBuilder.rootUri("http://localhost:8080/").build()
//    }
    @Autowired
    lateinit var eessiRest : EESSIRest

    @InjectMocks
    lateinit var service : EESSIKomponentenService

    @Mock
    lateinit var mockrestTemp : RestTemplate

    @Before
    fun setup() {
        logger.debug("Starting tests.... ...")
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testRequestAndResponse() {
        //val resentity = ResponseEntity<OpprettBuCogSEDResponse>(response, HttpStatus.OK)
        //val request = rest.createPost("/")

        //requestData
        val data = PENBrukerData("12345678", "DummyTester", "12345678")
        val uuid = UUID.randomUUID()

        //mock response
        val response = OpprettBuCogSEDResponse(uuid, "RINA-12345678", "Statusen er nå")
        val responseEntity = ResponseEntity<OpprettBuCogSEDResponse>(response, HttpStatus.OK)

        //mock requestTemplate
        whenever(mockrestTemp.postForEntity(anyString(), any() , eq(response::class.java))).thenReturn(responseEntity)
        //whenever(rest.getRest().exchange(eq(rest.createPost("/")), eq(rest.typeRef<OpprettBuCogSEDResponse>()))).thenReturn(resentity)

        //mock tilbake til helperbean
        eessiRest.restTemplate = mockrestTemp
        //helperbean settes til service
        service.eessiRest = eessiRest

        assertEquals(mockrestTemp, service.eessiRest.restTemplate)

        val res = service.opprettBuCogSED(data)

        Assert.assertNotNull(res)

        val resp : OpprettBuCogSEDResponse = res!!
        Assert.assertEquals(response.korrelasjonsID, resp.korrelasjonsID)
    }

    fun getDummyData(data : PENBrukerData) : OpprettBuCogSEDRequest {
        val request = OpprettBuCogSEDRequest(
                KorrelasjonsID = UUID.randomUUID(),
                BUC = BUC(
                        flytType = "P_BUC_01",
                        saksnummerPensjon = data.saksnummer,
                        saksbehandler = data.saksbehandler,
                        Parter = SenderReceiver(
                                sender = Institusjon(landkode = "NO", navn = "NAV"),
                                receiver = listOf(Institusjon(landkode = "DK", navn = "ATP"))
                        ),
                        NAVSaksnummer =  "nav_saksnummer",
                        SEDType = "SED_type",
                        notat_tmp = "Temp fil for å se hva som skjer"
                ),
                SED = SED(
                        SEDType = "P6000",
                        NAVSaksnummer = data.saksnummer,
                        ForsikretPerson = NavPerson(data.forsikretPerson),
                        Barn = listOf(NavPerson("123"), NavPerson("234")),
                        Samboer = NavPerson("345")
                )
        )
        return request
    }

}


