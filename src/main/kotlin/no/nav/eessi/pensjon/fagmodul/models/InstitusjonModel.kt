package no.nav.eessi.pensjon.fagmodul.models

class InstitusjonDetalj (
        val landkode: String,
        val akronym: String,
        val navn: String,
        val id: String,
        val tilegnetBucs: List<TilegnetBucsItem>
)

class TilegnetBucsItem(
        val eessiklar: Boolean,
        val gyldigStartDato: String,
        val institusjonsrolle: String,
        val bucType: String
)
