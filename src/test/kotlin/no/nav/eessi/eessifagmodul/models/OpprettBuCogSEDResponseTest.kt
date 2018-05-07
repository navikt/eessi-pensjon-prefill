package no.nav.eessi.eessifagmodul.models

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class OpprettBuCogSEDResponseTest {

    @Test
    fun normalBuCogSEDResponseObj() {
        val uuid : UUID = UUID.randomUUID()
        val response = OpprettBuCogSEDResponse(uuid, "RINA-SAK-12345", "Status")
        Assert.assertNotNull(response)
        Assert.assertEquals(uuid, response.korrelasjonsID)
        Assert.assertEquals("RINA-SAK-12345", response.rinaSaksnummer)
        Assert.assertEquals("Status", response.status)
    }

}