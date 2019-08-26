package no.nav.eessi.pensjon.fagmodul.api

import com.nhaarman.mockitokotlin2.*
import org.mockito.Mock
import no.nav.eessi.pensjon.fagmodul.eux.EuxService
import no.nav.eessi.pensjon.fagmodul.eux.basismodel.Vedlegg
import no.nav.eessi.pensjon.helper.AktoerIdHelper
import no.nav.eessi.pensjon.services.arkiv.HentdokumentInnholdResponse
import no.nav.eessi.pensjon.services.arkiv.SafService
import no.nav.eessi.pensjon.services.arkiv.VariantFormat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import java.nio.file.Files
import java.nio.file.Paths

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
    fun `gittEtGyldigPutVedleggTilDokumentSaaKKallEuxPutVedleggPaaDokument`() {

        val etVedlegg = String(Files.readAllBytes(Paths.get("src/test/resources/etbilde.pdf")))

        doReturn(HentdokumentInnholdResponse(etVedlegg,
                "enfil.pdf",
                "application/pdf")).whenever(mockSafService).hentDokumentInnhold(any(), any(), any())

        bucController.putVedleggTilDokument("123",
                "456",
                "7892",
                "1",
                "2",
                VariantFormat.ARKIV )
        val vedlegg = Vedlegg("enfil.pdf", etVedlegg)
        verify(mockEuxService, times(1)).leggTilVedleggPaaDokument(eq("123"), eq("456"), eq("7892"), eq(vedlegg), eq("pdf"))
    }
}
