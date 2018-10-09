package no.nav.eessi.eessifagmodul.prefill.kravsak

data class KravSak(
        val kKravSakFull: String? = null,
        val kKravGjelder: String? = null,
        val kSakT: String? = null,
        val decode: String? = null
) {
    override fun toString(): String {
        return "KravSak: $kKravSakFull, $kKravGjelder, $kSakT, $decode"
    }
}
