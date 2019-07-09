package no.nav.eessi.pensjon.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit


@Service
class TimingService(private val registry: MeterRegistry) {

    fun timedStart(tag: String): TimingObject = TimingObject(tag, registry)
    fun timesStop(timerObject: TimingObject) { timerObject.stop() }
}

class TimingObject(tag: String, registry: MeterRegistry) {
    private val timer: Timer = Timer.builder("eessipensjon_fagmodul").tag("tag", tag).register(registry)
    private val start = System.nanoTime()

    fun stop() {
        timer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS)
    }
}
