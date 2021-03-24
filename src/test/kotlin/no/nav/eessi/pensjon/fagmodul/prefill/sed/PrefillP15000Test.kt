package no.nav.eessi.pensjon.fagmodul.prefill.sed

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.nav.eessi.pensjon.fagmodul.models.KravType
import no.nav.eessi.pensjon.fagmodul.models.PrefillDataModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import kotlin.test.assertFailsWith


class PrefillP15000Test {

    @Test
    fun `prefill P15000 uten sakstype skal gi en feilmelding som sier at sakType mangler`() {
        val mockedPrefill = mock<PrefillDataModel>()

        whenever(mockedPrefill.kravType).thenReturn(KravType.ALDER)

        val exception = assertFailsWith<ResponseStatusException> {
            PrefillP15000(mock()).prefill(
                mockedPrefill, mock(), mock(), mock()
            )
        }
        assertEquals(exception.reason,"Ved opprettelse av krav SED må saksbehandling være fullført i Pesys ( vilkårsprøving o.l ) og jordklode i brukerkontekst kan ikke benyttes")
    }
}
