package no.nav.eessi.eessifagmodul.services

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc

@SpringBootTest
@RunWith(SpringRunner::class)
@WebMvcTest(EESSIKomponentenService::class)
class EESSIKomponentenServiceTest {

    @Autowired
    lateinit var mvc : MockMvc

    @Test
    fun testRequest() {

    }

}


