package no.nav.eessi.pensjon.vedlegg

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockitokotlin2.*
import no.nav.eessi.pensjon.logging.AuditLogger
import no.nav.eessi.pensjon.vedlegg.client.Dokument
import no.nav.eessi.pensjon.vedlegg.client.HentMetadataResponse
import no.nav.eessi.pensjon.vedlegg.client.HentdokumentInnholdResponse
import no.nav.eessi.pensjon.vedlegg.client.SafException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.*
import org.springframework.util.LinkedMultiValueMap
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

@ExtendWith(MockitoExtension::class)
class VedleggControllerMockTest {

    @Mock
    lateinit var vedleggService: VedleggService

    @Mock
    lateinit var auditLogger: AuditLogger

    lateinit var vedleggController: VedleggController

    @BeforeEach
    fun setup() {
        vedleggController = VedleggController(vedleggService, auditLogger)
        vedleggController.initMetrics()
    }

    @Test
    fun `gitt en 400 httpstatuscode fra safClient når metadata hentes så returnes 400 httpstatuscode`() {
        whenever(vedleggService.hentDokumentMetadata("123"))
                .thenThrow(SafException("noe gikk galt", HttpStatus.valueOf(400)))

        val resp = vedleggController.hentDokumentMetadata("123")
        assertEquals(HttpStatus.valueOf(400), resp.statusCode)
        assertTrue(resp.body!!.contains("noe gikk galt"))
    }

    @Test
    fun `gitt en 200 httpstatuscode fra safClient når dokument metadata hentes så returnes 200 httpstatuscode`() {

        val responseJson = javaClass.getResource("/json/saf/hentMetadataResponse.json").readText()
                .trim()
                .replace("\r", "")
                .replace("\n", "")
                .replace(" ", "")
        val mapper = jacksonObjectMapper()

        whenever(vedleggService.hentDokumentMetadata("123"))
                .thenReturn(mapper.readValue(responseJson, HentMetadataResponse::class.java))

        val resp = vedleggController.hentDokumentMetadata("123")
        assertEquals(HttpStatus.valueOf(200), resp.statusCode)
        assertEquals(resp.body!!.trim().replace("\r", "").replace("\n", "").replace(" ", ""), responseJson)
    }

    @Test
    fun `gitt en 400 httpstatuscode fra safClient når dokumentinnhold hentes så returnes 400 httpstatuscode`() {
        whenever(vedleggService.hentDokumentInnhold("123", "4567", "ARKIV"))
                .thenThrow(SafException("noe gikk galt", HttpStatus.valueOf(400)))

        val resp = vedleggController.getDokumentInnhold("123", "4567", "ARKIV")
        assertEquals(HttpStatus.valueOf(400), resp.statusCode)
        assertTrue(resp.body!!.contains("noe gikk galt"))
    }

    @Test
    fun `gitt en 200 httpstatuscode fra safClient når dokumentinnhold hentes så returnes 200 httpstatuscode`() {
        whenever(vedleggService.hentDokumentInnhold("123", "4567", "ARKIV"))
                .thenReturn(HentdokumentInnholdResponse("WVdKag==", "enFil.pdf", "application/pdf"))

        val resp = vedleggController.getDokumentInnhold("123", "4567", "ARKIV")
        assertEquals(HttpStatus.valueOf(200), resp.statusCode)
        assertEquals(resp.body?.replace("\r","") , javaClass.getResource("/json/saf/hentDokumentInnholdResponse.json").readText().replace("\r","")
        )
    }

    @Test
    fun `gitt Et Gyldig PutVedleggTilDokument Saa Kall EuxPutVedleggPaaDokument`() {
        val filInnhold = String(Base64.getEncoder().encode(Files.readAllBytes(Paths.get("src/test/resources/etbilde.pdf"))))

        val rinasakid = "456"
        val rinadocid = "7892"

        val filnavn = "P2000 - Krav om alderspensjon.pdf"
        val filtype = "application/pdf".split("/")[1]

        doReturn(Dokument("4444444","P2000 - Krav om alderspensjon", emptyList())).`when`(vedleggService).hentDokumentMetadata(any(), any(), any())
        doReturn(HentdokumentInnholdResponse(filInnhold, filnavn, "application/pdf"))
                .whenever(vedleggService).hentDokumentInnhold(any(), any(), any())

        doNothing().whenever(vedleggService).leggTilVedleggPaaDokument(any(), any(), any(), any(), any(), any())


        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA

        val disposition = ContentDisposition
                .builder("form-data")
                .name("file")
                .build().toString()

        val attachmentMeta = LinkedMultiValueMap<String, String>()
        attachmentMeta.add(HttpHeaders.CONTENT_DISPOSITION, disposition)
        val dokumentInnholdBinary = Base64.getDecoder().decode(filInnhold)
        val attachmentPart = HttpEntity(dokumentInnholdBinary, attachmentMeta)

        val body = LinkedMultiValueMap<String, Any>()
        body.add("multipart", attachmentPart)

        vedleggController.putVedleggTilDokument("123",
                rinasakid,
                rinadocid,
                "1",
                "2",
                "ARKIV")

        verify(vedleggService, times(1)).leggTilVedleggPaaDokument(
                eq("123"),
                eq(rinasakid),
                eq(rinadocid),
                eq(filInnhold),
                eq(filnavn),
                eq(filtype)
        )
    }

    @Test
    fun `Gitt en person med journalposter som inneholder RinaSakIder når SAF metadata blir kallet Så hent RinaSakIDer`() {
        val aktoerId = "1212"

        doReturn(listOf("1212")).`when`(vedleggService).hentRinaSakIderFraMetaData(aktoerId)

        val resp = vedleggController.hentRinaIderFraMetadata(aktoerId)
        assertEquals(1, resp.size)
    }

    @Test
    fun `Gitt en person uten journalposter som inneholder RinaSakIder når SAF metadata blir kallet Så returner tom liste`() {
        val aktoerId = "1212"

        doReturn(emptyList<String>()).`when`(vedleggService).hentRinaSakIderFraMetaData(aktoerId)

        val resp = vedleggController.hentRinaIderFraMetadata(aktoerId)
        assertEquals(0, resp.size)
    }

}
