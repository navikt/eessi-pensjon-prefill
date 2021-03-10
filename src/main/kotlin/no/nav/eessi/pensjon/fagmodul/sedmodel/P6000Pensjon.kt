package no.nav.eessi.pensjon.fagmodul.sedmodel

class P6000Pensjon(
    val gjenlevende: Bruker? = null,
    val reduksjon: List<ReduksjonItem>? = null, // P6000
    val vedtak: List<VedtakItem>? = null, // P6000
    val sak: Sak? = null, // P6000
    val tilleggsinformasjon: Tilleggsinformasjon? = null, //P6000
    val ytterligeinformasjon: String? = null,
    val kravDato: Krav? = null
)