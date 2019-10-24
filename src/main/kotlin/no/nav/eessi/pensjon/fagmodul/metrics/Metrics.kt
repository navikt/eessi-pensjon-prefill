package no.nav.eessi.pensjon.fagmodul.metrics

import io.micrometer.core.instrument.Counter
import no.nav.eessi.pensjon.metrics.counter

fun getCounter(key: String): Counter {
    val countermap = mapOf(
            "AKSJONOK" to counter("eessipensjon_fagmodul.euxmuligeaksjoner", "vellykkede"),
            "SENDSEDOK" to counter("eessipensjon_fagmodul.sendsed", "vellykkede"),
            "SENDSEDFEIL" to counter("eessipensjon_fagmodul.sendsed", "feilede"),
            "HENTSEDOK" to counter("eessipensjon_fagmodul.hentsed", "vellykkede"),
            "HENTSEDFEIL" to counter("eessipensjon_fagmodul.hentsed", "feilede"),
            "SLETTSEDOK" to counter("eessipensjon_fagmodul.slettsed", "vellykkede"),
            "SLETTSEDFEIL" to counter("eessipensjon_fagmodul.slettsed", "feilede"),
            "OPPRETTBUCOGSEDOK" to counter("eessipensjon_fagmodul.opprettbucogsed", "vellykkede"),
            "OPPRETTBUCOGSEDFEIL" to counter("eessipensjon_fagmodul.opprettbucogsed", "feilede"),
            "HENTBUCOK" to counter("eessipensjon_fagmodul.hentbuc", "vellykkede"),
            "HENTBUCFEIL" to counter("eessipensjon_fagmodul.hentbuc", "feilede"),
            "HENTKRAVUTLANDOK" to counter("eessipensjon_fagmodul.hentKravUtland", "vellykkede"),
            "HENTKRAVUTLANDFEIL" to counter("eessipensjon_fagmodul.hentKravUtland", "feilede"),
            "SELFTESTEUXOK" to counter("eessipensjon_fagmodul.selftestEUX", "vellykkede"),
            "SELFTESTTPSOK" to counter("eessipensjon_fagmodul.selftestTPS", "vellykkede"),
            "SELFTESTPESYSOK" to counter("eessipensjon_fagmodul.selftestPESYS", "vellykkede"),
            "SELFTESTEUXFEIL" to counter("eessipensjon_fagmodul.selftestEUX", "feilede"),
            "SELFTESTTPSFEIL" to counter("eessipensjon_fagmodul.selftestTPS", "feilede"),
            "SELFTESTPESYSFEIL" to counter("eessipensjon_fagmodul.selftestPESYS", "feilede")

    )
    return countermap.getValue(key)
}
