package no.nav.eessi.eessifagmodul.models

import com.fasterxml.jackson.annotation.JsonProperty

//Request from frontend
//{"institutions":[{"NO:"DUMMY"}],"buc":"P_BUC_06","sed":"P6000","caseId":"caseId","subjectArea":"pensjon","actorId":"2323123"}
data class FrontendRequest(
        //sector
        val subjectArea: String? = null,
        //PEN-saksnummer
        val caseId: String? = null,
        val buc: String? = null,
        val sed : String? = null,
        //mottakere
        val institutions: List<Institusjon>? = null,
        @JsonProperty("actorId")
        //aktoerid
        var pinid: String? = null
)

data class Institusjon(
        val country: String? = null,
        val institution: String? = null
)

