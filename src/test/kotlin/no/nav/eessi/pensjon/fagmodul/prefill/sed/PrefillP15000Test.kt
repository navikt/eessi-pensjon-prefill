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
import org.springframework.web.server.ResponseStatusException
import kotlin.test.assertFailsWith


class PrefillP15000Test {

    @Test
    fun `prefill P15000 uten sakstype skal gi en feilmelding som sier at sakType mangler`() {
        val mockedPrefill = mockkClass(PrefillDataModel::class)
        every { mockedPrefill.penSaksnummer } returns "12345"
        every { mockedPrefill.kravType } returns KravType.ALDER
        every { mockedPrefill.avdod } returns PersonId("1234", "5678")

        val exception = assertFailsWith<ResponseStatusException> {
            PrefillP15000(mockk()).prefill(
                mockedPrefill, mockk(), mockk(){
                    every { sakType} returns null
                }, mockk<Pensjonsinformasjon>{
                    every { avdod } returns mockk()
                }
            )
        }
        assertEquals(exception.reason,"Ved opprettelse av krav SED må saksbehandling være fullført i Pesys ( vilkårsprøving o.l ) og jordklode i brukerkontekst kan ikke benyttes")
    }
}
