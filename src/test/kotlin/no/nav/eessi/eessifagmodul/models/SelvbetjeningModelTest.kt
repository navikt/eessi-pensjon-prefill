package no.nav.eessi.eessifagmodul.models

import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


@RunWith(MockitoJUnitRunner::class)
class SelvbetjeningModelTest {

    @Test
    fun `create mock structure trygdehistorikk`() {

        val result = createTrygdehistorikkMock()
        assertNotNull(result)
        assertEquals(3, result?.periode?.size)
        assertEquals(true, result.godkjent)

        val json = mapAnyToJson(result)

        println(json)

    }



}