package no.nav.eessi.eessifagmodul.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit


//@Configuration
//@EnableAspectJAutoProxy
//class TimingConfiguration {
//
//    @Bean
//    fun timedAspect(registry: MeterRegistry): TimedAspect {
//        return TimedAspect(registry)
//    }
//
//}

data class TimingObject(
        val timer: Timer,
        val start: Long
)

@Service
class TimingService(private val registry: MeterRegistry) {

    fun timedStart(tag: String): TimingObject {
        // you can keep a ref to this; ok to call multiple times, though
        val timer = Timer.builder("eessipensjon_fagmodul").tag("tag", tag).register(registry)
        // manually do the timing calculation
        return TimingObject(timer, System.nanoTime())
    }

    fun timesStop(timerObject: TimingObject) {
        //get timer and starttime from Objet
        val timer = timerObject.timer
        val start = timerObject.start
        // manually do the timing calculation
        timer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS)
    }

//    fun helloManual() {
//        // you can keep a ref to this; ok to call multiple times, though
//        val timer = Timer.builder("eessipensjon_fagmodul").tag("method", "manual").register(registry)
//
//        // manually do the timing calculation
//        val start = System.nanoTime()
//        doSomething()
//        timer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS)
//    }
//
//    fun helloSupplier() {
//        //eessipensjon_fagmodul.euxmuligeaksjoner
//        val timer = Timer.builder("eessipensjon_fagmodul").tag("method", "supplier").register(registry)
//
//        // execution of the method is timed internally
//        timer.record({ doSomething() })
//    }
//
//    fun helloSample() {
//        val timer = Timer.builder("eessipensjon_fagmodul").tag("method", "sample").register(registry)
//
//        // records time taken between Sample creation and registering the
//        // stop() with the given Timer
//        val sample = Timer.start(registry)
//        doSomething()
//        sample.stop(timer)
//    }

//    // TimedAspect adds "class" and "method" tags
//    @Timed(value = "eessipensjon_fagmodul.aspect")
//    fun helloAspect() {
//        doSomething()
//    }

//    private fun doSomething() {
//        try {
//            Thread.sleep(50)
//        } catch (e: InterruptedException) {
//            //
//        }
//
//    }

}
