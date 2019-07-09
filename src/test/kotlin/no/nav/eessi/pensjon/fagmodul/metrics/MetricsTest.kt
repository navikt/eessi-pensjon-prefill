package no.nav.eessi.pensjon.fagmodul.metrics

import io.micrometer.core.instrument.Metrics
import org.junit.Test
import kotlin.test.assertEquals


class MetricsTest {
    @Test
    fun `testing getCounter for right key in euxservice`() {
        val value = getCounter("SENDSEDOK")
        val tst = Metrics.counter("eessipensjon_fagmodul.sendsed", "type", "vellykkede")
        assertEquals(tst::class.java, value::class.java)
        assertEquals(tst, value)
    }

    @Test(expected = NoSuchElementException::class)
    fun `testing getCounter for key not found in euxservice`() {
        getCounter("NOKEYISINMAP")
    }
}