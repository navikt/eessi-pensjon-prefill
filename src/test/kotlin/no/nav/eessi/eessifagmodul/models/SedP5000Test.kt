package no.nav.eessi.eessifagmodul.models

import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import no.nav.eessi.eessifagmodul.utils.mapJsonToAny
import no.nav.eessi.eessifagmodul.utils.typeRefs
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
class SedP5000Test {

    val logger: Logger by lazy { LoggerFactory.getLogger(SedP5000Test::class.java) }

    @Before
    fun setup() {
        logger.debug("Starting tests.... ...")
        MockitoAnnotations.initMocks(this)
    }


    @Test
    fun `validate P5000 to json and back`() {
        val navSedP5000 = SedMock().genererP5000Mock()
        assertNotNull(navSedP5000)

        val json = mapAnyToJson(navSedP5000, true)
        //map json back to P6000 obj
        val pensjondata = mapJsonToAny(json, typeRefs<SED>())
        assertNotNull(pensjondata)
        assertEquals(navSedP5000, pensjondata)


        logger.debug("\n\n $json \n\n")

    }



}