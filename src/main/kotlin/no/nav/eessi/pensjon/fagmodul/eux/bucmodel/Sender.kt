package no.nav.eessi.pensjon.fagmodul.eux.bucmodel

class Sender (
        address: Address? = null,
        activeSince: Any? = null,
        registryNumber: Any? = null,
        acronym: String? = null,
        countryCode: String? = null,
        contactMethods: Any? = null,
        name: String? = null,
        location: Any? = null,
        assignedBUCs: Any? = null,
        id: String? = null,
        accessPoint: Any? = null,
        val identifier: String? = null,
        val contactTypeIdentifier: String? = null,
        val authority: String? = null
): Organisation(
        address,
        activeSince,
        registryNumber,
        acronym,
        countryCode,
        contactMethods,
        name,
        location,
        assignedBUCs,
        id,
        accessPoint
)
