package no.nav.eessi.pensjon.fagmodul.metrics

import io.micrometer.core.instrument.Metrics
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


class MetricsTest {
    @Test
    fun `testing getCounter for right key in euxservice`() {
        val value = getCounter("SENDSEDOK")
        val tst = Metrics.counter("eessipensjon_fagmodul.sendsed", "type", "vellykkede")
        assertEquals(tst::class.java, value::class.java)
        assertEquals(tst, value)
    }

    @Test
    fun `testing getCounter for key not found in euxservice`() {
        assertThrows<NoSuchElementException> {
            getCounter("NOKEYISINMAP")
        }
    }
}
