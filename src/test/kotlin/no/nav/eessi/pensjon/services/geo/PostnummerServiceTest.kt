package no.nav.eessi.pensjon.services.geo

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull

@RunWith(MockitoJUnitRunner::class)
class PostnummerServiceTest {

    private lateinit var service: PostnummerService

    @Before
    fun setup() {
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
