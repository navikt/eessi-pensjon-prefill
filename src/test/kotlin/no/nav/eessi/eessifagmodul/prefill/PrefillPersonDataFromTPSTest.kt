package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.clients.personv3.PersonV3Client
import no.nav.eessi.eessifagmodul.services.LandkodeService
import no.nav.eessi.eessifagmodul.services.PostnummerService
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class PrefillPersonDataFromTPSTest{

    @Mock
    lateinit var mockPersonClient: PersonV3Client

    private lateinit var preutfyllingTPS: PrefillPersonDataFromTPS


    @Before
    fun setup() {
        preutfyllingTPS = PrefillPersonDataFromTPS(mockPersonClient , PostnummerService(), LandkodeService())
    }


    @Test
    fun `create birthplace as unknown`() {
        val bruker = Bruker()
        bruker.foedested = null

        val result = preutfyllingTPS.hentFodested(bruker)

        assertNotNull(result)
        println(result)
        assertEquals(null, result.land)

    }
    @Test
    fun `create correct birthplace known`() {
        val bruker = Bruker()
        bruker.foedested = "NOR"

        val result = preutfyllingTPS.hentFodested(bruker)

        assertNotNull(result)
        assertEquals("NOR", result.land)

    }

    @Test
    fun `create personAdresse`() {
        val landkode = Landkoder()
        landkode.value = "NOR"
        val postnr = Postnummer()
        postnr.value = "0123"
        val gateadresse = Gateadresse()
        gateadresse.husnummer = 12
        gateadresse.kommunenummer = "120"
        gateadresse.gatenavn = "Kirkeveien"
        gateadresse.landkode = landkode
        gateadresse.poststed = postnr

        //val struktadr = StrukturertAdresse()
        //struktadr.
        val bostedadr = Bostedsadresse()
        bostedadr.strukturertAdresse = gateadresse

        val person = Person()
        person.bostedsadresse = bostedadr

        val result = preutfyllingTPS.personAdresse(person)
        println(result)

        assertNotNull(result)

        assertEquals("NO", result.land)
        assertEquals( "Kirkeveien 12", result.gate)
        assertEquals("OSLO", result.by)
    }

}