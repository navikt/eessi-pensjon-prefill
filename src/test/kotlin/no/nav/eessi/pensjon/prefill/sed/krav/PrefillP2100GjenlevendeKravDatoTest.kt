package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.sed.KravType
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiKravGjelder
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PrefillP2100GjenlevendeKravDatoTest {

    @Test
    fun `En ferdig utfylt p2100 skal inkluderer kravdato`() {
//        val sak = mockk<V1Sak>(relaxed = true).apply {
//            every { forsteVirkningstidspunkt } returns createXMLCalendarFromString("2020-05-20")
//            every { kravHistorikkListe } returns mockkKravListe(listOf(KravArsak.GJNL_SKAL_VURD))
//        }
        val prefillData = mockk<PrefillDataModel>(relaxed = true).apply {
            every { kravType } returns KravType.GJENLEV
            every { kravDato } returns LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        }

        justRun {
            mockk<PrefillP2xxxPensjon>(relaxed = true).avsluttHvisKunDenneKravTypeIHistorikk(
                any(),
                any(),
                EessiKravGjelder.FORSTEG_BH
            )
        }

        val prefillNav = PrefillPDLNav(mockk(relaxed = true), "inst1", "instnavn")

//        assertEquals(
//            Krav(prefillData.kravDato, prefillData.kravType),
//            PrefillP2100(prefillNav).prefillSed(prefillData, mockk<PersonDataCollection>(relaxed = true)
//                .apply {
//                    every { gjenlevendeEllerAvdod?.sivilstand } returns listOf(
//                        Sivilstand(
//                            type = Sivilstandstype.GIFT,
//                            metadata = Metadata(emptyList(), false, "DOLLY", "Doll")
//                        )
//                    )
//                }, sak
//            ).second.nav?.krav
//        )
    }

//    private fun mockkKravListe(listOf: List<KravArsak>): V1KravHistorikkListe =
//        V1KravHistorikkListe().apply {
//            kravHistorikkListe.addAll(listOf.map { V1KravHistorikk().apply { kravArsak = it.name } })
//        }
}