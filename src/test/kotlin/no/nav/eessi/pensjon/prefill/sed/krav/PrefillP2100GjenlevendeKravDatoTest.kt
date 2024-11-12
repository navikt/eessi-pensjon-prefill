package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.sed.Krav
import no.nav.eessi.pensjon.eux.model.sed.KravType
import no.nav.eessi.pensjon.pensjonsinformasjon.models.KravArsak
import no.nav.eessi.pensjon.pensjonsinformasjon.models.PenKravtype
import no.nav.eessi.pensjon.personoppslag.pdl.model.Metadata
import no.nav.eessi.pensjon.personoppslag.pdl.model.Sivilstand
import no.nav.eessi.pensjon.personoppslag.pdl.model.Sivilstandstype
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.utils.createXMLCalendarFromString
import no.nav.eessi.pensjon.utils.toJson
import no.nav.pensjon.v1.kravhistorikk.V1KravHistorikk
import no.nav.pensjon.v1.kravhistorikkliste.V1KravHistorikkListe
import no.nav.pensjon.v1.sak.V1Sak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PrefillP2100GjenlevendeKravDatoTest {

    @Disabled
    @Test
    fun `En ferdig utfylt p2100 skal inkluderer kravdato`() {
        val sak = mockk<V1Sak>(relaxed = true).apply {
            every { forsteVirkningstidspunkt } returns createXMLCalendarFromString("2020-05-20")
            every { kravHistorikkListe } returns mockkKravListe(listOf(KravArsak.GJNL_SKAL_VURD))
        }
        val prefillData = mockk<PrefillDataModel>(relaxed = true).apply {
            every { kravType } returns KravType.GJENLEV
            every { kravDato } returns LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        }

        justRun {
            mockk<PrefillP2xxxPensjon>(relaxed = true).avsluttHvisKunDenneKravTypeIHistorikk(
                any(),
                any(),
                PenKravtype.FORSTEG_BH
            )
        }

        val prefillNav = PrefillPDLNav(mockk(relaxed = true), "inst1", "instnavn")

        println("** ${prefillNav.toJson()}")
        assertEquals(
            Krav(prefillData.kravDato, prefillData.kravType),
            PrefillP2100(prefillNav).prefillSed(prefillData, mockk<PersonDataCollection>(relaxed = true)
                .apply {
                    every { gjenlevendeEllerAvdod?.sivilstand } returns listOf(
                        Sivilstand(
                            type = Sivilstandstype.GIFT,
                            metadata = Metadata(emptyList(), false, "DOLLY", "Doll")
                        )
                    )
                }, sak
            ).second.nav?.krav
        )
    }

    private fun mockkKravListe(listOf: List<KravArsak>): V1KravHistorikkListe =
        V1KravHistorikkListe().apply {
            kravHistorikkListe.addAll(listOf.map { V1KravHistorikk().apply { kravArsak = it.name } })
        }
}