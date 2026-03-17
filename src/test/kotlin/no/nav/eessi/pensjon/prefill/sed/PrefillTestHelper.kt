package no.nav.eessi.pensjon.prefill.sed


import no.nav.eessi.pensjon.eux.model.BucType
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.shared.api.ApiRequest
import no.nav.eessi.pensjon.shared.api.InstitusjonItem


fun createMockApiRequest(sed: SedType, buc: BucType, payload: String, sakNr: String): ApiRequest {
    val items = listOf(InstitusjonItem(country = "NO", institution = "NAVT003"))
    return ApiRequest(
        institutions = items,
        sed = sed,
        sakId = sakNr,
        euxCaseId = null,
        aktoerId = "1000060964183",
        buc = buc,
        subjectArea = "Pensjon",
        payload = payload,
        processDefinitionVersion = "4.2"
    )
}
