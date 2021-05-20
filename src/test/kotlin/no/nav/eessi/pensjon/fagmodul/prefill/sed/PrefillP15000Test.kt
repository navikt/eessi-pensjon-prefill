package no.nav.eessi.pensjon.fagmodul.prefill.sed

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import no.nav.eessi.pensjon.fagmodul.models.KravType
import no.nav.eessi.pensjon.fagmodul.models.PersonId
import no.nav.eessi.pensjon.fagmodul.models.PrefillDataModel
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import kotlin.test.assertFailsWith


internal class PrefillP15000Test {

    private val prfillP15000 = PrefillP15000(mockk())

    @Test
    fun `prefill P15000 uten sakstype skal gi en feilmelding som sier at sakType mangler`() {
        val mockedPrefill = mockkClass(PrefillDataModel::class)
        every { mockedPrefill.penSaksnummer } returns "12345"
        every { mockedPrefill.kravType } returns KravType.ALDER
        every { mockedPrefill.avdod } returns PersonId("1234", "5678")

        val exception = assertFailsWith<ResponseStatusException> {
            prfillP15000.prefill(
                mockedPrefill, mockk(), mockk(){
                    every { sakType} returns null
                }, mockk<Pensjonsinformasjon>{
                    every { avdod } returns mockk()
                }
            )
        }
        assertEquals(exception.reason,"Ved opprettelse av krav SED må saksbehandling være fullført i Pesys ( vilkårsprøving o.l ) og jordklode i brukerkontekst kan ikke benyttes")
    }

    @Test
    fun `Validerer av gyldig KravDato`() {
        val result = prfillP15000.validateFrontEndKravDato("2020-03-29 ")
        assertEquals(LocalDate.of(2020,3, 29), result)
    }

    @Test
    fun `Validerer av ugyldig KravDato`() {
        assertThrows<ResponseStatusException> {
            prfillP15000.validateFrontEndKravDato("2020 -03-29")
        }
    }
}
