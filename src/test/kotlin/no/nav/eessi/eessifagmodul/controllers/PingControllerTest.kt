package no.nav.eessi.eessifagmodul.controllers

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.junit.MockitoJUnitRunner


@RunWith(MockitoJUnitRunner::class)
class PingControllerTest {

    @InjectMocks
    lateinit var pingController: PingController

    @Test
    fun checkForPingIsOK() {
        val result = pingController.getPing()
        Assert.assertNotNull(result)
        Assert.assertEquals(200, result.statusCode.value())
    }
}