package no.nav.eessi.pensjon.services.geo

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class LandkoderTest {

    private val logger: Logger by lazy { LoggerFactory.getLogger(LandkoderTest::class.java) }

    private lateinit var service: LandkodeService

    @Before
    fun setup() {
        logger.debug("Starting tests.... ...")
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
