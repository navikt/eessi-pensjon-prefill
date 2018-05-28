package no.nav.eessi.eessifagmodul.controllers

import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.services.SEDKompnentService
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class SEDControllerTest {

    @InjectMocks
    lateinit var controller: SEDController

    @InjectMocks
    lateinit var sedService : SEDKompnentService

    @BeforeTest
    fun runOnStart() {
        controller.sedService = sedService;
    }

    @Test
    fun testOpprettSEDmSAK() {
        val result = controller.opprettSEDmedSak("123456", "123-456")

        assertNotNull(result)
        assertEquals(SED::class.java , result::class.java)
        assertEquals("SAKnr: 123-456", result.NAVSaksnummer)
    }


    @Test
    fun checkOpprettSED() {
        val result = controller.opprettSED("123457")

        assertNotNull(result)
        assertEquals(SED::class.java , result::class.java)
        assertEquals("SAKnr: 123456", result.NAVSaksnummer)
        assertEquals("FNR=123457", result.ForsikretPerson.fnr)
    }


    @Test
    fun checkSedsForBuc() {
        val result = controller.getSedsForBuc("BUCK123")

        assertNotNull(result)
        assertEquals(3, result.size)
    }

}