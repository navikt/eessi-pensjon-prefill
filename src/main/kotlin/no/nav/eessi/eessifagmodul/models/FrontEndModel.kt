package no.nav.eessi.eessifagmodul.models

//{"institution":[{"NO:"DUMMY"}],"buc":"P_BUC_06","sed":"P6000","caseId":"caseId","subjectArea":"pensjon"}
data class FrontendRequest(
        val subjectAera: String? = null,
        val caseId: String? = null,
        val buc: String? = null,
        val sed : String? = null,
        val institution: List<Institusjon>? = null
)

data class Institusjon(
        val country: String? = null,
        val institution: String? =null
)
