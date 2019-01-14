package no.nav.eessi.eessifagmodul.services.bucbucket

enum class QueryParameters {
    CORRELATION_ID {
        override fun paramName() = "correlationId"
    },
    SED_ID {
        override fun paramName() = "sedId"
    },
    SED_TYPE {
        override fun paramName() = "sedType"
    },
    BUC_TYPE {
        override fun paramName() = "bucType"
    },
    AKTOER_ID {
        override fun paramName() = "aktoerId"
    },
    PIN {
        override fun paramName() = "pin"
    },
    NAV_CASE_ID {
        override fun paramName() = "navCaseId"
    },
    RINA_CASE_ID {
        override fun paramName() = "rinaCaseId"
    },
    JOURNALPOST_ID {
        override fun paramName() = "journalId"
    },
    TEMA {
        override fun paramName() = "tema"
    },
    NATION_CODE {
        override fun paramName() = "nationCode"
    },
    MAX_RESULTS {
        override fun paramName() = "maxResults"
    },
    RESULT_START {
        override fun paramName() = "resultStart"
    };

    abstract fun paramName(): String
}

@Deprecated(replaceWith = ReplaceWith("Nothing"), level = DeprecationLevel.WARNING, message = "Utgår")
data class QueryResult(
        val navCaseId: String?,
        val lastUpdate: String?,
        val journalId: String?,
        val receiverNations: List<String>?,
        val bucBucketId: Int?,
        val senderNation: String?,
        val correlationId: String?,
        val sedId: String?,
        val bucType: String?
)