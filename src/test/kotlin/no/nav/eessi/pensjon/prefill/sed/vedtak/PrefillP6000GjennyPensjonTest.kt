package no.nav.eessi.pensjon.prefill.sed.vedtak

import no.nav.eessi.pensjon.eux.model.sed.Bruker
import no.nav.eessi.pensjon.eux.model.sed.Person
import no.nav.eessi.pensjon.eux.model.sed.PinItem
import no.nav.eessi.pensjon.prefill.etterlatte.EtterlatteVedtakResponseData
import no.nav.eessi.pensjon.prefill.etterlatte.GjennyUtbetaling
import no.nav.eessi.pensjon.prefill.etterlatte.GjennyVedtak
import no.nav.eessi.pensjon.prefill.etterlatte.VedtakStatus
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.utils.toJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime


class PrefillP6000GjennyPensjonTest {

    private val prefillP6000GjennyPensjon: PrefillP6000GjennyPensjon = PrefillP6000GjennyPensjon()

    @Test
    fun prefillP6000GjennyPensjonTest() {
        val etterlatteResData = EtterlatteVedtakResponseData(listOf(gjennyVedtak(
            attDato = LocalDateTime.of(2025, 7, 23, 23, 59),
            status = VedtakStatus.INNVILGELSE
        )))
        val eessiInformasjon = mockEessiInfo()
        val gjenlevende = mockBruker()

        val result = prefillP6000GjennyPensjon.prefillP6000GjennyPensjon(gjenlevende, etterlatteResData, eessiInformasjon)
        println("RESULT: ${result?.toJson()}")

        assertEquals("03", result?.vedtak?.firstOrNull()?.type)
        assertEquals("01", result?.vedtak?.firstOrNull()?.resultat)
        assertEquals("2021-07-23", result?.vedtak?.firstOrNull()?.virkningsdato)
        assertEquals("", result?.vedtak?.firstOrNull()?.beregning?.firstOrNull()?.periode?.tom)
        assertEquals("2025-07-23", result?.vedtak?.firstOrNull()?.beregning?.firstOrNull()?.periode?.fom)
        assertEquals(LocalDate.of(2025,7,23), result?.vedtak?.firstOrNull()?.iverksettelsesTidspunkt)
        assertEquals("2025-07-23", result?.tilleggsinformasjon?.dato)
    }

    @Test
    fun `Prefill P6000 Gjenny Pensjon med null som iverksettelsesdato `() {
        val etterlatteResData = EtterlatteVedtakResponseData(listOf(gjennyVedtak(
            null,
            VedtakStatus.INNVILGELSE
        )))
        val eessiInformasjon = mockEessiInfo()
        val gjenlevende = mockBruker()

        val result = prefillP6000GjennyPensjon.prefillP6000GjennyPensjon(gjenlevende, etterlatteResData, eessiInformasjon)

        assertEquals(null, result?.tilleggsinformasjon?.dato)

    }

    @Test
    fun `Prefill P6000 Gjenny Pensjon med vedtaket som har den nyeste iverksettelsesdatoen `() {
        val etterlatteResData = EtterlatteVedtakResponseData(listOf(
            gjennyVedtak(attDato = LocalDateTime.of(2020, 7, 23, 23, 59), status = VedtakStatus.AVSLAG),
            gjennyVedtak(attDato = LocalDateTime.of(2025, 7, 23, 23, 59), status = VedtakStatus.AVSLAG)
        ))

        val eessiInformasjon = mockEessiInfo()
        val gjenlevende = mockBruker()

        val result = prefillP6000GjennyPensjon.prefillP6000GjennyPensjon(gjenlevende, etterlatteResData, eessiInformasjon)

        assertEquals("2025-07-23", result?.tilleggsinformasjon?.dato)

    }

    @Test
    fun `Prefill P6000 Gjenny Pensjon med vedtaket som har ddtgdfgen nyeste iverksettelsesdatoen `() {
        val etterlatteResData = EtterlatteVedtakResponseData(listOf(
            gjennyVedtak(attDato = LocalDateTime.of(2025, 7, 23, 23, 59), status = VedtakStatus.AVSLAG),
            gjennyVedtak(attDato = LocalDateTime.of(2020, 7, 23, 23, 59), status = VedtakStatus.AVSLAG),
            gjennyVedtak(attDato = LocalDateTime.of(2026, 7, 23, 23, 59), status = VedtakStatus.INNVILGELSE)
        ))

        val eessiInformasjon = mockEessiInfo()
        val gjenlevende = mockBruker()

        val result = prefillP6000GjennyPensjon.prefillP6000GjennyPensjon(gjenlevende, etterlatteResData, eessiInformasjon)

        assertEquals("2026-07-23", result?.tilleggsinformasjon?.dato)

    }

    @Test
    fun `Prefill Gjenny P6000 Pensjon med AVSLAG skal gi ut attestertTidspunkt på vedtaksdato pkt 6_4 `() {
        val etterlatteResData = EtterlatteVedtakResponseData(listOf(
            gjennyVedtak(null, VedtakStatus.AVSLAG, attDato = LocalDateTime.of(2000, 7, 23, 23, 59) )
        ))

        val eessiInformasjon = mockEessiInfo()
        val gjenlevende = mockBruker()

        val result = prefillP6000GjennyPensjon.prefillP6000GjennyPensjon(gjenlevende, etterlatteResData, eessiInformasjon)

        assertEquals("2000-07-23", result?.tilleggsinformasjon?.dato)
        assertEquals("2021-07-23", result?.vedtak?.firstOrNull()?.virkningsdato)

    }

    private fun gjennyVedtak(dato: LocalDateTime? = null, status: VedtakStatus, attDato: LocalDateTime? = null) = GjennyVedtak(
        sakId = 123456,
        sakType = "Omstilling",
        virkningstidspunkt = LocalDate.parse("2021-07-23"),
        type = status,
        utbetaling = listOf(
            GjennyUtbetaling(
                fraOgMed = LocalDate.parse("2025-07-23"),
                tilOgMed = null,
                beloep = "2358"
            )
        ),
        iverksettelsesTidspunkt = dato,
        attestertTidspunkt = attDato
    )

    private fun mockBruker(): Bruker = Bruker(
        person = Person(
            fornavn = "Gjenlevende",
            etternavn = "Etternavn",
            pin = listOf(PinItem(identifikator = "12345678901")),
            statsborgerskap = null
        )
    )

    private fun mockEessiInfo(): EessiInformasjon = EessiInformasjon(
        institutionid = "321",
        institutionnavn = "",
        institutionGate = "",
        institutionPostnr = "",
        institutionLand = "",
        institutionBy = "",
    )

}
