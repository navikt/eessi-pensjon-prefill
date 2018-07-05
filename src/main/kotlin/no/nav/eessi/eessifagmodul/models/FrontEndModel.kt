package no.nav.eessi.eessifagmodul.models

//{"institution":"DUMMY","buc":"P_BUC_06","sed":"P6000","caseId":"caseId"}
data class FrontendRequest(
        val sed : String? = null,
        val institution: String? = null,
        val buc: String? = null,
        val caseId: String? = null
)
