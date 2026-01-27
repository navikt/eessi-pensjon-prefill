package no.nav.eessi.pensjon.prefill.sed

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import no.nav.eessi.pensjon.eux.model.sed.KravType
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate


@Daniel

internal class PrefillP15000Test {

    private val prfillP15000 = PrefillP15000(mockk())

    @Test
    fun `prefill P15000 uten sakstype skal gi en feilmelding som sier at sakType mangler`() {
        val mockedPrefill = mockkClass(PrefillDataModel::class)
        every { mockedPrefill.penSaksnummer } returns "12345"
        every { mockedPrefill.kravType } returns KravType.ALDER
        every { mockedPrefill.avdod } returns PersonInfo("1234", "5678")

        val exception = assertThrows<ResponseStatusException> {
//            prfillP15000.prefill(
//                mockedPrefill, mockk(), mockk<Pensjonsinformasjon>{
//                    every { avdod } returns mockk()
//                }
//            )
        }
        assertEquals(exception.reason,"Ved opprettelse av krav SED må saksbehandling være fullført i Pesys ( vilkårsprøving o.l ) og jordklode i brukerkontekst kan ikke benyttes")
    }

    @Test
    fun `Validerer av gyldig KravDato`() {
        assertEquals(
            LocalDate.of(2020,3, 29),
            prfillP15000.validateFrontEndKravDato("2020-03-29"))

        assertEquals(
            LocalDate.of(2021,9, 21),
            prfillP15000.validateFrontEndKravDato("2021-09-21T11:22:30.877+00:00"))

        assertEquals(
            LocalDate.of(2019,8, 30),
            prfillP15000.validateFrontEndKravDato("2019-08-30T09:37:37.318+0100"))
    }

    @Test
    fun `Validerer av ugyldig KravDato`() {
        assertThrows<ResponseStatusException> {
            prfillP15000.validateFrontEndKravDato("2020 -03-29")
        }
    }
}
