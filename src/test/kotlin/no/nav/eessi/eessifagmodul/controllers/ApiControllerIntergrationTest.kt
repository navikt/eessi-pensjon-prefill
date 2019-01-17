package no.nav.eessi.eessifagmodul.controllers

import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.test.context.junit4.SpringRunner


@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiControllerIntergrationTest {

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate


    @Ignore
    @Test
    fun testLandkoder() {
    }
//        val jwtResponse = testRestTemplate.getForEntity("/local/jwt", String::class.java)
//        val cookie = jwtResponse.body!!
//        println("cookie!!!!")
//        println(cookie)
//        val httpHeaders = HttpHeaders()
//        httpHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer $cookie")
//        val httpEntity = HttpEntity("", httpHeaders)
//
//        val result = testRestTemplate.exchange("/api/landkoder", HttpMethod.GET, httpEntity, typeRef<String>())
//
//        assertNotNull(result)
//        assertEquals(HttpStatus.OK, result.statusCode)
//
//        val list = mapJsonToAny(result.body!!, typeRefs<List<String>>())
//        assertEquals(10, list.size)
//
//        }

}