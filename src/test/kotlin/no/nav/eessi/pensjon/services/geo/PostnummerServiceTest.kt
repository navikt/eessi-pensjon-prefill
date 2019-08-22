package no.nav.eessi.pensjon.services.geo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class PostnummerServiceTest {

    private lateinit var service: PostnummerService

    @BeforeEach
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
