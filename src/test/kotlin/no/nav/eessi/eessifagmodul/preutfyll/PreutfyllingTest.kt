package no.nav.eessi.eessifagmodul.preutfyll

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.models.createSED
import no.nav.eessi.eessifagmodul.services.EuxService
import no.nav.eessi.eessifagmodul.services.EuxServiceTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestTemplate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class PreutfyllingTest {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PreutfyllingTest::class.java) }

    @Mock
    private lateinit var sed: SED

    @Before
    fun setup() {
        logger.debug("Starting tests.... ...")
    }


    @Test
    fun `create mock on preutfyll P6000`() {

        val sed = createSED("P6000")

        val resultat = Preutfylling().preutfylling(sed)

        assertNotNull(resultat)

        assertEquals(2, resultat.grad!!.size)

        val list = resultat.grad!!

        assertEquals(100, list[0].grad)
        assertEquals("Bruker", list[0].felt)
        assertEquals(100, list[1].grad)
        assertEquals("Pensjon", list[1].felt)



    }






}