package no.nav.eessi.eessifagmodul.services

import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.clients.personv3.PersonV3Client
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class TpsServiceTest{

    private val logger: Logger by lazy { LoggerFactory.getLogger(TpsServiceTest::class.java) }

    lateinit var service: TpsService

    @Mock
    private lateinit var mockPersonV3Client: PersonV3Client

    @Before
    fun setup() {
        logger.debug("Starting tests.... ...")
        service = TpsService(mockPersonV3Client)
    }


    @Test
    fun `hente ned tpsPerson`() {
        val personnavn = Personnavn()
            personnavn.fornavn = "Dummy"
        val person = Person()
            person.personnavn = personnavn
        val response = HentPersonResponse()
            response.person = person

        whenever(mockPersonV3Client.hentPerson(anyString())).thenReturn(response)

        val data = service.hentPerson("12345678901")
        assertNotNull(data)
        val json = mapAnyToJson(data)
        assertNotNull(json)
        assertEquals("Dummy", data.person.personnavn.fornavn)

        println("Hva HentPersonRespone består av : $json")

    }

    @Test(expected = IllegalArgumentException::class)
    fun `hente ned tpsPerson med forlite ident`() {
        service.hentPerson("12678901")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `hente ned tpsPerson med formye ident`() {
        service.hentPerson("1267890342342342342341")
    }

}