package no.nav.eessi.eessifagmodul

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
@ActiveProfiles(value = ["develop"])
class EessiFagmodulApplicationTests {

    @Test
    fun contextLoads() {

    }
}
