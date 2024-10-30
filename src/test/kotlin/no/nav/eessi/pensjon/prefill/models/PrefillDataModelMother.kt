package no.nav.eessi.pensjon.prefill.models

import no.nav.eessi.pensjon.eux.model.BucType
import no.nav.eessi.pensjon.eux.model.BucType.P_BUC_09
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.shared.api.InstitusjonItem
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.api.ReferanseTilPerson

object PrefillDataModelMother {

    fun initialPrefillDataModel(
        sedType: SedType,
        pinId: String,
        vedtakId: String? = null,
        penSaksnummer: String = "12345",
        avdod: PersonInfo? = null,
        kravDato: String? = null,
        kravId: String? = null,
        refTilPerson: ReferanseTilPerson? = null,
        euxCaseId: String = "123456",
        bucType: BucType = P_BUC_09,
        institution: List<InstitusjonItem> = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))) =
            PrefillDataModel(
                    penSaksnummer,
                    bruker = PersonInfo(pinId, "123456789", false, "ola@nav.no", "11223344"),
                    avdod = avdod,
                    sedType = sedType,
                    vedtakId = vedtakId ?: "",
                    buc = bucType,
                    institution = institution,
                    kravDato = kravDato,
                    kravId = kravId,
                    refTilPerson = refTilPerson,
                    euxCaseID = euxCaseId
            )
}

