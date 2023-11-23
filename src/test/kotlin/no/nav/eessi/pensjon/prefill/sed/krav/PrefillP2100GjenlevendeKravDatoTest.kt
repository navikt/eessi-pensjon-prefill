package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.sed.AndreinstitusjonerItem
import no.nav.eessi.pensjon.eux.model.sed.Bruker
import no.nav.eessi.pensjon.eux.model.sed.Krav
import no.nav.eessi.pensjon.eux.model.sed.KravType
import no.nav.eessi.pensjon.pensjonsinformasjon.models.KravArsak
import no.nav.eessi.pensjon.pensjonsinformasjon.models.PenKravtype
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.utils.createXMLCalendarFromString
import no.nav.pensjon.v1.kravhistorikk.V1KravHistorikk
import no.nav.pensjon.v1.kravhistorikkliste.V1KravHistorikkListe
import no.nav.pensjon.v1.sak.V1Sak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PrefillP2100GjenlevendeKravDatoTest {

    @Test
    fun midlertidigTestForKrav() {
        val prefillData = mockk<PrefillDataModel>(relaxed = true)
        val sak = mockk<V1Sak>(relaxed = true)
        val andreInstitusjondetaljer = mockk<AndreinstitusjonerItem>()
        val gjenlev = mockk<Bruker>()
        val prefillNav = PrefillPDLNav(mockk(relaxed = true), "inst1", "instnavn")
        val personData = mockk<PersonDataCollection>(relaxed = true)

        every { sak.forsteVirkningstidspunkt } returns createXMLCalendarFromString("2020-05-20")
        every { prefillData.kravDato } returns "2020-01-01"
        every { prefillData.kravType } returns KravType.GJENLEV

        val kravHistorikkListe = mockkKravListe()

        every { sak.kravHistorikkListe } returns kravHistorikkListe
        val prefilledPensjon = mockk<PrefillP2xxxPensjon>(relaxed = true)
        justRun { prefilledPensjon.avsluttHvisKunDenneKravTypeIHistorikk(any(), any(), PenKravtype.FORSTEG_BH) }
        every {
            prefilledPensjon.populerMeldinOmPensjon(
                prefillData.bruker.norskIdent,
                prefillData.penSaksnummer,
                sak,
                andreInstitusjondetaljer,
                gjenlev,
                prefillData.kravId
            )
        } returns mockk()

        val result = PrefillP2100(prefillNav).prefillSed(prefillData, personData, sak)
        assertEquals(Krav("2020-01-01", "02"), result.second.nav?.krav)
        assertEquals(Krav("2020-01-01", "02"), result.second.pensjon?.kravDato)
    }

    private fun mockkKravListe(): V1KravHistorikkListe {
        val gjenlevendHistorikk = V1KravHistorikk()
        gjenlevendHistorikk.kravArsak = KravArsak.GJNL_SKAL_VURD.name

        val kravListe = mutableListOf(gjenlevendHistorikk)
        val kravHistorikkListe = V1KravHistorikkListe()
        kravHistorikkListe.kravHistorikkListe.addAll(kravListe)
        return kravHistorikkListe
    }
}