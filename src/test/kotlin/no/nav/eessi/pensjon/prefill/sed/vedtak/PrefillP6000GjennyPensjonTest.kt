package no.nav.eessi.pensjon.prefill.sed.vedtak

import no.nav.eessi.pensjon.eux.model.sed.Bruker
import no.nav.eessi.pensjon.eux.model.sed.Person
import no.nav.eessi.pensjon.eux.model.sed.PinItem
import no.nav.eessi.pensjon.prefill.etterlatte.EtterlatteVedtakResponseData
import no.nav.eessi.pensjon.prefill.etterlatte.GjennyUtbetaling
import no.nav.eessi.pensjon.prefill.etterlatte.GjennyVedtak
import no.nav.eessi.pensjon.prefill.etterlatte.VedtakStatus
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime


class PrefillP6000GjennyPensjonTest {

    private val prefillP6000GjennyPensjon: PrefillP6000GjennyPensjon = PrefillP6000GjennyPensjon()

    @Test
    fun prefillP6000GjennyPensjonTest() {
        val etterlatteResData = EtterlatteVedtakResponseData(
            vedtak = listOf(
                GjennyVedtak(
                    sakId = 123456,
                    sakType = "Omstilling",
                    virkningstidspunkt = LocalDate.parse("2025-07-23"),
                    type = VedtakStatus.INNVILGELSE,
                    utbetaling = listOf(
                        GjennyUtbetaling(
                            fraOgMed = LocalDate.parse("2025-07-23"),
                            tilOgMed = null,
                            beloep = "2358"
                        )
                    ),
                    iverksettelsesTidspunkt = LocalDateTime.now(),
                )
            )
        )

        val eessiInformasjon = EessiInformasjon(
            institutionid = "321",
            institutionnavn = "",
            institutionGate = "",
            institutionPostnr = "",
            institutionLand = "",
            institutionBy = "",
        )

        val gjenlevende = Bruker(
            person = Person(
                fornavn = "Gjenlevende",
                etternavn = "Etternavn",
                pin = listOf(PinItem(identifikator = "12345678901")),
                statsborgerskap = null
            )
        )
        val result = prefillP6000GjennyPensjon.prefillP6000GjennyPensjon(gjenlevende, etterlatteResData, eessiInformasjon)

        assertEquals("03", result?.vedtak?.firstOrNull()?.type)
        assertEquals("01", result?.vedtak?.firstOrNull()?.resultat)
        assertEquals("2025-07-23", result?.vedtak?.firstOrNull()?.virkningsdato)
        assertEquals("", result?.vedtak?.firstOrNull()?.beregning?.firstOrNull()?.periode?.tom)
        assertEquals("2025-07-23", result?.vedtak?.firstOrNull()?.beregning?.firstOrNull()?.periode?.fom)

    }

}
