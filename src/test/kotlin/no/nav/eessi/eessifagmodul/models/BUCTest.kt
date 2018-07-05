package no.nav.eessi.eessifagmodul.models

import no.nav.eessi.eessifagmodul.utils.createListOfSED
import no.nav.eessi.eessifagmodul.utils.createListOfSEDOnBUC
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class BUCTest {

    val logger: Logger by lazy { LoggerFactory.getLogger(BUCTest::class.java) }

    @Before
    fun setup() {
        logger.debug("Starting tests.... ...")
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun `parse BUCs til json`() {
        val data = createPensjonBucList()
        val json = mapAnyToJson(data)
        logger.debug(json)
    }

    @Test
    fun `liste over seds fra alle bucs`() {
        val seds = createListOfSED()
        assertNotNull(seds)
        assertEquals(13, seds.size)
    }

    @Test
    fun `liste over seds fra bestemt bucs`() {
        val buc3 = BUC(bucType = "P_BUC_03")
        val seds = createListOfSEDOnBUC(buc3)
        assertNotNull(seds)
        assertEquals(1, seds.size)

        val buc6 = BUC(bucType = "P_BUC_06")
        val seds2 = createListOfSEDOnBUC(buc6)

        assertNotNull(seds2)
        assertEquals(4, seds2.size)

    }


}