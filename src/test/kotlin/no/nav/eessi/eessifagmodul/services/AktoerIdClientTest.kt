package no.nav.eessi.eessifagmodul.services

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.whenever
import com.sun.org.apache.xerces.internal.jaxp.datatype.DatatypeFactoryImpl
import no.nav.eessi.eessifagmodul.jaxws.client.AktoerIdClient
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.AktoerV2
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentRequest
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentResponse
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.IdentDetaljer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.time.Instant
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@RunWith(SpringJUnit4ClassRunner::class)
//@ActiveProfiles("develop")
class AktoerIdClientTest {

    @InjectMocks
    lateinit var aktorclient : AktoerIdClient

    @Mock
    lateinit var service: AktoerV2

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    //@Test
    fun hentAktoerIdForIdent() {
        //mock ident
        val identid = "ident12"

        //mock request
        val request = HentAktoerIdForIdentRequest()
        request.ident = identid

        //mock response
        val response = HentAktoerIdForIdentResponse()
        response.aktoerId = "Aktor12345"

        val detalj : IdentDetaljer = IdentDetaljer()
        detalj.tpsId  = identid
        detalj.datoFom = DatatypeFactoryImpl.newInstance().newXMLGregorianCalendar()
        //val detaljer : MutableList<IdentDetaljer> = mutableListOf(detalj)
        response.identHistorikk.add(detalj)

        //mock service
        whenever(service.hentAktoerIdForIdent(any())).thenReturn(response)
        aktorclient.service = service

        //validate mock like client service
        assertEquals(service, aktorclient.service)

        //run test
        //val res = aktorclient.hentAktoerIdForIdent(identid)!!

        //validate test data
        /*
        assertNotNull(res)
        assertEquals(response.aktoerId, res.aktoerId)
        assertNotNull(res.identHistorikk)
        assertEquals(1, res.identHistorikk.size)
        */
    }

}

