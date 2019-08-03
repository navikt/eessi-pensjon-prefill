package no.nav.eessi.pensjon.fagmodul.prefill.model

import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import java.time.LocalDate

object PrefillDataModelMother {
    fun initialPrefillDataModel(sedType: String, personAge: Int) =
            PrefillDataModel()
                    .apply {
                        rinaSubject = "Pensjon"
                        sed = SED(sedType)
                        penSaksnummer = "12345"
                        vedtakId = "12312312"
                        buc = "P_BUC_99"
                        aktoerID = "123456789"
                        personNr = generateRandomFnr(personAge)
                        institution = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
            }

    private fun generateRandomFnr(yearsToSubtract: Int): String {
        val fnrdate = LocalDate.now().minusYears(yearsToSubtract.toLong())
        val y = fnrdate.year.toString()
        val day = fixDigits(fnrdate.dayOfMonth.toString())
        val month = fixDigits(fnrdate.month.value.toString())
        val fixedyear = y.substring(2, y.length)
        val fnr = day + month + fixedyear + 43352
        return fnr
    }

    private fun fixDigits(str: String): String {
        if (str.length == 1) {
            return "0$str"
        }
        return str
    }

}

