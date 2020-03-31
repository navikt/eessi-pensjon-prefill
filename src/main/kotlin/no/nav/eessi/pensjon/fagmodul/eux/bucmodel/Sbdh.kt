package no.nav.eessi.pensjon.fagmodul.eux.bucmodel

class Sbdh(
        val attachments: List<Any?>? = null,
        val sender: Sender? = null,
        val receivers: List<ReceiversItem?>? = null,
        val headerVersion: String? = null,
        val documentIdentification: DocumentIdentification? = null,
        val caseIdentification: CaseIdentification? = null
)