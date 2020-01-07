package no.nav.eessi.pensjon.fagmodul.api

import com.nhaarman.mockitokotlin2.*
import no.nav.eessi.pensjon.fagmodul.eux.EuxService
import no.nav.eessi.pensjon.logging.AuditLogger
import no.nav.eessi.pensjon.services.aktoerregister.AktoerregisterService
import no.nav.eessi.pensjon.services.arkiv.HentdokumentInnholdResponse
import no.nav.eessi.pensjon.services.arkiv.SafService
import no.nav.eessi.pensjon.services.arkiv.VariantFormat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import java.nio.file.Files
import java.nio.file.Paths

@ExtendWith(MockitoExtension::class)
class BucControllerTest {

    @Spy
    lateinit var mockEuxService: EuxService

    @Spy
    lateinit var auditLogger: AuditLogger

    @Mock
    lateinit var mockSafService: SafService

    @Mock
    lateinit var mockAktoerIdHelper: AktoerregisterService

    private lateinit var bucController: BucController


    @BeforeEach
    fun before() {
        this.bucController = BucController(mockEuxService, mockSafService, mockAktoerIdHelper, auditLogger)
    }


    @Test
    fun `gets valid bucs fagmodul can handle excpect list`() {
        val result = bucController.getBucs()
        Assertions.assertEquals(10, result.size)
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
        verify(mockEuxService, times(1)).leggTilVedleggPaaDokument(eq("123"), eq("456"), eq("7892"), any(), eq("pdf"))
    }
}
