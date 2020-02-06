package no.nav.eessi.pensjon.fagmodul.models

data class InstitusjonDetalj (
        val landkode: String? = null,
        val akronym: String? = null,
        val navn: String? = null,
        val id: String? = null,
        val tilegnetBucs: List<TilegnetBucsItem?>? = null
)

data class TilegnetBucsItem(
        val eessiklar: Boolean? = null,
        val gyldigStartDato: String? = null,
        val institusjonsrolle: String? = null,
        val bucType: String? = null
)
