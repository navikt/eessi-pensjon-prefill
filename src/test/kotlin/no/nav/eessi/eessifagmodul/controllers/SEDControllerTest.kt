package no.nav.eessi.eessifagmodul.controllers

import no.nav.eessi.eessifagmodul.models.SED
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class SEDControllerTest {

    @InjectMocks
    lateinit var controller: SEDController

    @Test
    fun checkForOpprettSED() {
        val result = controller.opprettSED("123456", "123-456")

        assertNotNull(result)
        assertEquals(SED::class.java , result::class.java)
        assertEquals("SAKnr: 123-456", result.NAVSaksnummer)


    }




}