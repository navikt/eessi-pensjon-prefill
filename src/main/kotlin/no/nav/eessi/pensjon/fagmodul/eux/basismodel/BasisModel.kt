package no.nav.eessi.pensjon.fagmodul.eux.basismodel

/**
 * data class model from EUX Basis
 */



data class BucSedResponse(
        val caseId: String,
        val documentId: String
)

class Rinasak(
        val id: String? = null,
        val processDefinitionId: String? = null,
        val traits: Traits? = null,
        val applicationRoleId: String? = null,
        val properties: Properties? = null,
        val status: String? = null
)

class Properties(
        val importance: String? = null,
        val criticality: String? = null
)

class Traits(
        val birthday: String? = null,
        val localPin: String? = null,
        val surname: String? = null,
        val caseId: String? = null,
        val name: String? = null,
        val flowType: String? = null,
        val status: String? = null
)
