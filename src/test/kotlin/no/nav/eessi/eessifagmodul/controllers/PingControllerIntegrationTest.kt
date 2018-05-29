package no.nav.eessi.eessifagmodul.controllers

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import kotlin.test.assertEquals

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = ["freg.security.oidc.enabled=false"])
class PingControllerIntegrationTest {

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate

    @Test
    fun testLocalPing() {
        val result = testRestTemplate.getForEntity("/ping/", String::class.java)
        Assert.assertNotNull(result)
        Assert.assertEquals(ResponseEntity::class.java, result.javaClass)
        Assert.assertNull(result.body)
        assertEquals(HttpStatus.OK, result.statusCode)
    }
}