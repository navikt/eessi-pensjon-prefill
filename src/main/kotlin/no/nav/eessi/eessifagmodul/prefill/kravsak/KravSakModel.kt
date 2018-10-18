package no.nav.eessi.eessifagmodul.prefill.kravsak

data class KravSak(
        val kravGjelder: String? = null,
        val kravSakFull: String? = null,
        val sak: String? = null,
        val decode: String? = null
) {
    override fun toString(): String {
        return "KravSak: $kravSakFull, $kravGjelder, $sak, $decode"
    }
}
