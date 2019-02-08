package no.nav.eessi.eessifagmodul.services.eux

/**
 * data class model from EUX Basis
 */

data class RinaAksjon(
        val dokumentType: String? = null,
        val navn: String? = null,
        val dokumentId: String? = null,
        val kategori: String? = null,
        val id: String? = null
)

data class BucSedResponse(
        val caseId: String,
        val documentId: String
)