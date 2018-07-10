package no.nav.eessi.eessifagmodul.models

/**
 * data class model from EUX Basis
 */
data class RINASaker(
    val id: String? = null,
    val applicationRoleId: String? = null,
    val status: String? = null,
    val processDefinitionId: String? = null,
    val traits: RINATraits? = null,
    val properties: RINAProperties? = null
)

data class RINATraits(
    val birthday: String? =null,
    val localPin: String? = null,
    val surname: String? = null,
    val caseId: String? = null,
    val name: String? = null,
    val flowType: String? = null,
    val status: String? = null
)

data class RINAProperties(
    val importance: String? = null,
    val criticality: String? = null
)