package no.nav.eessi.eessifagmodul.models

import com.fasterxml.jackson.annotation.JsonAlias
import java.util.*


data class OpprettBuCogSEDResponse(
        @JsonAlias("korrelasjonsID")
        val korrelasjonsID: UUID,
        @JsonAlias("rinasaksnummer")
        val rinaSaksnummer: String?,
        val status: String?
        //val nyere_parameter_versjon2: String?
)

data class OpprettBuCogSEDRequest(
        val KorrelasjonsID: UUID,
        val BUC: BUC?,
        val SED: SED?,
        val Vedlegg: List<Any>? = null
)
