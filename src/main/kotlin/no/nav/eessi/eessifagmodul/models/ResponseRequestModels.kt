package no.nav.eessi.eessifagmodul.models

import java.util.*

data class OpprettBuCogSEDResponse(
        val korrelasjonsID: UUID,
        val rinasaksnummer: String?,
        val status: String?
        //val nyere_parameter_versjon2: String?
)

data class OpprettBuCogSEDRequest(
        val KorrelasjonsID: UUID,
        val BUC: BUC?,
        val SED: SED?,
        val Vedlegg: List<Any>? = null
)
