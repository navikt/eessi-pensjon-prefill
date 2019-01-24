package no.nav.eessi.eessifagmodul.services.eux.bucmodel

data class Sender(
        val address: Address? = null,
        val activeSince: Long? = null,
        val registryNumber: Any? = null,
        val acronym: String? = null,
        val countryCode: String? = null,
        val contactMethods: Any? = null,
        val name: String? = null,
        val location: Any? = null,
        val assignedBUCs: Any? = null,
        val id: String? = null,
        val accessPoint: Any? = null,
        val identifier: String? = null,
        val contactTypeIdentifier: String? = null,
        val authority: String? = null
)