package no.nav.eessi.eessifagmodul.models

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.time.Instant

class PENBrukerDataTest {

    lateinit var data :  PENBrukerData
    lateinit var testime : Instant

    @Before
    fun setUp() {
        testime = Instant.ofEpochMilli(1525872472700)
        println("timeinMillSek : $testime")
        data = PENBrukerData("123456","DummySak", "Forsikret2345", testime )
    }

    @Test
    fun getSaksnummer() {
        assertEquals("123456", data.saksnummer)
    }

    @Test
    fun getDato() {
        assertEquals(testime, data.dato)
    }
}