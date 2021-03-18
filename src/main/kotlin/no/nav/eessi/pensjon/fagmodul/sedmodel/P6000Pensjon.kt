package no.nav.eessi.pensjon.fagmodul.sedmodel

class P6000Pensjon(
    val gjenlevende: Bruker? = null,
    val reduksjon: List<ReduksjonItem>? = null,
    val vedtak: List<VedtakItem>? = null,
    val sak: Sak? = null,
    val tilleggsinformasjon: Tilleggsinformasjon? = null,
    val ytterligeinformasjon: String? = null,
    val kravDato: Krav? = null
)