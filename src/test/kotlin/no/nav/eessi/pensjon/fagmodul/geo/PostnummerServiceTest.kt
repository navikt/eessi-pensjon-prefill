package no.nav.eessi.pensjon.fagmodul.geo

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(MockitoJUnitRunner::class)
class PostnummerServiceTest {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PostnummerServiceTest::class.java) }

    private lateinit var service: PostnummerService

    @Before
    fun setup() {
        logger.debug("Starting tests.... ...")
        service = PostnummerService()
    }

    @Test
    fun `hente postnr med gyldig sted`() {

        val sted = service.finnPoststed("1430")
        assertNotNull(sted)
        assertEquals("ÅS", sted)
    }

    @Test
    fun `hente postnr med ugyldig sted`() {
        val sted = service.finnPoststed("1439")
        assertNull(sted)
    }


}
