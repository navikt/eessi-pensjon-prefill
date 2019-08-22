package no.nav.eessi.pensjon.services.geo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class LandkoderTest {

    private lateinit var service: LandkodeService

    @BeforeEach
    fun setup() {
        service = LandkodeService()
    }

    @Test
    fun `hente landkode2 med gyldig verdi`() {

        val sted = service.finnLandkode2("NOR")
        assertNotNull(sted)
        assertEquals("NO", sted)
    }

    @Test
    fun `hente landkode3 med gyldig verdi`() {
        val sted = service.finnLandkode3("NO")
        assertNotNull(sted)
        assertEquals("NOR", sted)
    }

    @Test
    fun `hente liste over land`() {
        val list = service.hentLandkode2()
        assertNotNull(list)
        assertEquals(31, list.size)
    }

}
