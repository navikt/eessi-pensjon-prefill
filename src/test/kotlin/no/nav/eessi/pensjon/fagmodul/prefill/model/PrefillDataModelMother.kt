package no.nav.eessi.pensjon.fagmodul.prefill.model

import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED

object PrefillDataModelMother {
    fun initialPrefillDataModel(sedType: String, pinId: String, vedtakId: String = "12312312") =
            PrefillDataModel().apply {
                rinaSubject = "Pensjon"
                sed = SED(sedType)
                penSaksnummer = "12345"
                this.vedtakId = vedtakId
                buc = "P_BUC_99"
                aktoerID = "123456789"
                personNr = pinId
                institution = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
            }
}

