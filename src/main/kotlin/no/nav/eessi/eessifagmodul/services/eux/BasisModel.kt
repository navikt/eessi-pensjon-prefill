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

data class RINATraits(
        var birthday: String? = null,
        var localPin: String? = null,
        var surname: String? = null,
        var caseId: String? = null,
        var name: String? = null,
        var flowType: String? = null,
        var status: String? = null
)

data class RINAProperties(
        var importance: String? = null,
        var criticality: String? = null
)

data class RINAaksjoner(
        var dokumentType: String? = null,
        var navn: String? = null,
        var dokumentId: String? = null,
        var kategori: String? = null,
        var id: String? = null
)