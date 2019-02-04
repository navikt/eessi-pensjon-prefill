package no.nav.eessi.eessifagmodul.services.eux

/**
 * data class model from EUX Basis
 */

//TODO: Er denne i bruk?
//data class RINASaker(
//        var id: String? = null,
//        var applicationRoleId: String? = null,
//        var status: String? = null,
//        var processDefinitionId: String? = null,
//        var traits: RINATraits? = null,
//        var properties: RINAProperties? = null
//)


data class BucSedResponse(
        val caseId: String,
        val documentId: String
)