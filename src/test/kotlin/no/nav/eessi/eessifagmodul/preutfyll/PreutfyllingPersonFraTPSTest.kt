package no.nav.eessi.eessifagmodul.preutfyll

import no.nav.eessi.eessifagmodul.clients.personv3.PersonV3Client
import no.nav.eessi.eessifagmodul.component.LandkodeService
import no.nav.eessi.eessifagmodul.component.PostnummerService
import no.nav.eessi.eessifagmodul.utils.mapJsonToAny
import no.nav.eessi.eessifagmodul.utils.typeRefs
import no.nav.eessi.eessifagmodul.utils.validateJson
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class PreutfyllingPersonFraTPSTest{

    @Mock
    lateinit var mockPersonClient: PersonV3Client

    lateinit var preutfyllingTPS: PreutfyllingPersonFraTPS


    @Before
    fun setup() {
        preutfyllingTPS = PreutfyllingPersonFraTPS(mockPersonClient , PostnummerService(), LandkodeService())
    }


    @Test
    fun `create fodested som ukjent`() {
        val bruker = Bruker()
        bruker.foedested = null

        val result = preutfyllingTPS.hentFodested(bruker)

        assertNotNull(result)
        assertEquals("Unknown", result.land)

    }
    @Test
    fun `create correct fodested kjent`() {
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
        assertNotNull(result)
        assertEquals("NO", result.land)
        assertEquals("12", result.bygning)
        assertEquals("OSLO", result.by)
    }

}