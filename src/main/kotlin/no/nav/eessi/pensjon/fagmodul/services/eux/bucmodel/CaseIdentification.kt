package no.nav.eessi.pensjon.fagmodul.services.eux.bucmodel

data class CaseIdentification(
        val identifier: String? = null,
        val isProtectedPerson: Boolean? = null,
        val protectedPerson: Boolean? = null,
        val type: String? = null,
        val version: String? = null
)