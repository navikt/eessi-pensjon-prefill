package no.nav.eessi.eessifagmodul.controllers

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.http.ResponseEntity


@RunWith(MockitoJUnitRunner::class)
class PingControllerTest {

    @InjectMocks
    //lateinit var pingController: PingController
    lateinit var pingController: InternalController

    @Test
    fun checkForPingIsOK() {
        val result = pingController.getPing()
        Assert.assertNotNull(result)
        Assert.assertEquals(ResponseEntity.ok(""), result)
        Assert.assertEquals(200, result.statusCode.value())
    }

}