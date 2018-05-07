package no.nav.eessi.eessifagmodul.models

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class OpprettBuCogSEDRequestTest {

    @Test
    fun normalBuCogSEDRequestObj() {

        val data = PENBrukerData("12345678", "DummyTester", "12345678")

        val uuid : UUID = UUID.randomUUID()

        val buc = BUC(
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
        )
        val sed = SED(
                SEDType = "P2000",
                NAVSaksnummer = data.saksnummer,
                ForsikretPerson = NavPerson(data.forsikretPerson),
                Barn = listOf(NavPerson("123"), NavPerson("234")),
                Samboer = NavPerson("345")
        )

        val request = OpprettBuCogSEDRequest(
                KorrelasjonsID = uuid,
                BUC = buc,
                SED = sed
        )

        assertEquals(uuid, request.KorrelasjonsID)
        assertEquals(buc , request.BUC)
        assertEquals(sed , request.SED)

        assertEquals("P2000", request.SED!!.SEDType)
        assertEquals("SED_type", request.BUC!!.SEDType)

    }
}