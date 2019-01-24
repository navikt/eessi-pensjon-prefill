package no.nav.eessi.eessifagmodul.services.eux.bucmodel

data class ConversationsItem(
        val date: Any? = null,
        val versionId: Any? = null,
        val userMessages: List<UserMessagesItem>? = null, //Any?
        val id: String? = null,
        val participants: List<ParticipantsItem?>? = null
)