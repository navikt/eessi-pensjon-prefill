package no.nav.eessi.pensjon.api.arkiv

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockitokotlin2.whenever
import no.nav.eessi.pensjon.logging.AuditLogger
import no.nav.eessi.pensjon.services.arkiv.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import java.nio.file.Files
import java.nio.file.Paths

@ExtendWith(MockitoExtension::class)
class SafControllerTest {

    @Mock
    lateinit var safService: SafService

    @Mock
    lateinit var auditLogger: AuditLogger

    lateinit var safController: SafController

    @BeforeEach
    fun setup() {
        safController = SafController(safService, auditLogger)

    }

    @Test
    fun `gitt en 400 httpstatuscode fra safService når metadata hentes så returnes 400 httpstatuscode`() {
        whenever(safService.hentDokumentMetadata("123"))
                .thenThrow(SafException("noe gikk galt", HttpStatus.valueOf(400)))

        val resp = safController.hentDokumentMetadata("123")
        assertEquals(HttpStatus.valueOf(400), resp.statusCode)
        assertTrue(resp.body!!.contains("noe gikk galt"))
    }

    @Test
    fun `gitt en 200 httpstatuscode fra safService når dokument metadata hentes så returnes 200 httpstatuscode`() {

        val responseJson = String(Files.readAllBytes(Paths.get("src/test/resources/json/saf/hentMetadataResponse.json")))
                .trim()
                .replace("\r", "")
                .replace("\n", "")
                .replace(" ", "")
        val mapper = jacksonObjectMapper()

        whenever(safService.hentDokumentMetadata("123"))
                .thenReturn(mapper.readValue(responseJson, HentMetadataResponse::class.java))

        val resp = safController.hentDokumentMetadata("123")
        assertEquals(HttpStatus.valueOf(200), resp.statusCode)
        assertEquals(resp.body!!.trim().replace("\r", "").replace("\n", "").replace(" ", ""), responseJson)
    }

    @Test
    fun `gitt en 400 httpstatuscode fra safService når dokumentinnhold hentes så returnes 400 httpstatuscode`() {
        whenever(safService.hentDokumentInnhold("123", "4567", VariantFormat.ARKIV))
                .thenThrow(SafException("noe gikk galt", HttpStatus.valueOf(400)))

        val resp = safController.getDokumentInnhold("123", "4567", VariantFormat.ARKIV)
        assertEquals(HttpStatus.valueOf(400), resp.statusCode)
        assertTrue(resp.body!!.contains("noe gikk galt"))
    }

    @Test
    fun `gitt en 200 httpstatuscode fra safService når dokumentinnhold hentes så returnes 200 httpstatuscode`() {
        whenever(safService.hentDokumentInnhold("123", "4567", VariantFormat.ARKIV))
                .thenReturn(HentdokumentInnholdResponse("WVdKag==", "enFil.pdf", "application/pdf"))

        val resp = safController.getDokumentInnhold("123", "4567", VariantFormat.ARKIV)
        assertEquals(HttpStatus.valueOf(200), resp.statusCode)
        assertEquals(resp.body?.replace("\r","") , String(Files.readAllBytes(Paths.get("src/test/resources/json/saf/hentDokumentInnholdResponse.json"))).replace("\r","")
        )
    }
}
