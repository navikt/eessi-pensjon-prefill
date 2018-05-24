package no.nav.eessi.eessifagmodul.models

import org.junit.Assert.*
import org.junit.Test

class SenderReceiverTest {

    @Test
    fun standarSenderReceiver() {
        val ins = Institusjon("NO", "Norge")

        val list : MutableList<Institusjon> = mutableListOf(Institusjon("SE", "Sverige"),Institusjon("DK", "Danmark"))

        val sr = SenderReceiver(ins, list)


        assertNotNull(sr)
        assertEquals(ins, sr.sender)
        assertEquals("NO", sr.sender.landkode)
        assertEquals("NO", sr.sender.landkode)
        assertNotNull(sr.receiver)
        assertEquals(2, sr.receiver.size)
        assertEquals("DK", sr.receiver.get(1).landkode)
    }

}