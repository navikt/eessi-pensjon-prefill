package no.nav.eessi.pensjon.fagmodul.services.eux.bucmodel

data class Sbdh(
        val attachments: List<Any?>? = null,
        val sender: Sender? = null,
        val receivers: List<ReceiversItem?>? = null,
        val headerVersion: String? = null,
        val documentIdentification: DocumentIdentification? = null,
        val caseIdentification: CaseIdentification? = null
)