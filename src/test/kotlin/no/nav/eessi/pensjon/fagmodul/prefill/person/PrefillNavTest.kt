package no.nav.eessi.pensjon.fagmodul.prefill.person

import com.nhaarman.mockitokotlin2.mock
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.tps.PrefillPersonDataFromTPS
import no.nav.eessi.pensjon.fagmodul.sedmodel.EessisakItem
import no.nav.eessi.pensjon.fagmodul.sedmodel.Krav
import no.nav.eessi.pensjon.fagmodul.sedmodel.Nav
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate

internal class PrefillNavTest {

    @Test
    fun `prefill med tom respons fra TPS`() {
        val mockPrefillPersonDataFromTPS = mock<PrefillPersonDataFromTPS>()
        val someInstitutionId = "enInstId"
        val someIntitutionNavn = "instNavn"
        val prefillNav = PrefillNav(mockPrefillPersonDataFromTPS, someInstitutionId, someIntitutionNavn)
        val somePenSaksnr = "somePenSaksnr"
        val somePersonNr = "somePersonNr"
        val prefillData = PrefillDataModel().apply {
            penSaksnummer = somePenSaksnr
            personNr = somePersonNr

        }
        val actual = prefillNav.prefill(prefillData, true)
        val expected = Nav(
                eessisak = listOf(EessisakItem(someInstitutionId, someIntitutionNavn, somePenSaksnr, "NO")),
                krav = Krav(LocalDate.now().toString()))
        assertEquals(expected, actual)
    }
}