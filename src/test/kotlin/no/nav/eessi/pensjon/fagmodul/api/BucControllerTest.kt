package no.nav.eessi.pensjon.fagmodul.api

import com.nhaarman.mockitokotlin2.*
import org.mockito.Mock
import no.nav.eessi.pensjon.fagmodul.eux.EuxService
import no.nav.eessi.pensjon.helper.AktoerIdHelper
import no.nav.eessi.pensjon.services.arkiv.HentdokumentInnholdResponse
import no.nav.eessi.pensjon.services.arkiv.SafService
import no.nav.eessi.pensjon.services.arkiv.VariantFormat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class BucControllerTest {

    @Spy
    lateinit var mockEuxService: EuxService

    @Mock
    lateinit var mockSafService: SafService

    @Mock
    lateinit var mockAktoerIdHelper: AktoerIdHelper

    private lateinit var bucController: BucController

    @BeforeEach
    fun before() {
        this.bucController = BucController(mockEuxService, mockSafService, mockAktoerIdHelper, "T")
    }

    @Test
    fun enTest(){

        doReturn(HentdokumentInnholdResponse("abc",
                "enfil.pdf",
                "application/pdf")).whenever(mockSafService).hentDokumentInnhold(any(), any(), any())

        bucController.putVedleggTilDokument("123",
                "456",
                "7892",
                "1",
                "2",
                VariantFormat.ARKIV )
        verify(mockEuxService, times(1)).leggTilVedleggPaaDokument(any(), any(),any(),any(), any())
    }
}
