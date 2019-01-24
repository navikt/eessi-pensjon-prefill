package no.nav.eessi.eessifagmodul.services.eux.bucmodel

import com.fasterxml.jackson.annotation.JsonIgnore

data class Tags(
        val DMProcessId: String? = null,
        val operation: Any? = null,
        val category: String? = null,
        val type: String? = null,
        @JsonIgnore
        val dmprocessId: Any? = null
)