package no.nav.eessi.pensjon.vedlegg

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.isNull
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.typeRefs
import no.nav.eessi.pensjon.vedlegg.client.EuxVedleggClient
import no.nav.eessi.pensjon.vedlegg.client.HentMetadataResponse
import no.nav.eessi.pensjon.vedlegg.client.SafClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.web.client.RestTemplate
import java.nio.file.Files
import java.nio.file.Paths

@ExtendWith(MockitoExtension::class)
internal class VedleggServiceTest  {

    @Mock
    lateinit var safClient : SafClient

    lateinit var vedleggService : VedleggService

    @BeforeEach
    fun setup() {
        val euxVedleggClient = EuxVedleggClient(RestTemplate())
        vedleggService = VedleggService(safClient, euxVedleggClient)
    }

    @Test
    fun `Gitt en liste av journalposter med tilhørende dokumenter Når man filtrer et konkret dokumentInfoId Så returner et dokument med dokumentInfoId`() {

        val metadataJson = String(Files.readAllBytes(Paths.get("src/test/resources/json/saf/hentMetadataResponse.json")))
        val metadata = mapJsonToAny(metadataJson, typeRefs<HentMetadataResponse>())

        doReturn(metadata).`when`(safClient).hentDokumentMetadata(any())

        assert(vedleggService.hentDokumentMetadata("12345678910", "439560100", "453743887")?.dokumentInfoId == "453743887")
    }

    @Test
    fun `Gitt en liste av journalposter der listen er tom Så returner verdien null`() {

        val metadataJson = """
            {
              "data": {
                "dokumentoversiktBruker": {
                  "journalposter": []
                }
              }
            }
        """.trimIndent()
        val metadata = mapJsonToAny(metadataJson, typeRefs<HentMetadataResponse>())

        doReturn(metadata).`when`(safClient).hentDokumentMetadata(any())

        assert(vedleggService.hentDokumentMetadata("12345678910", "123", "123") == null)
    }

    @Test
    fun `Gitt en liste av journalpostermed et dokument Så returner dokumentet`() {

        val metadataJson = """
            {
              "data": {
                "dokumentoversiktBruker": {
                  "journalposter": [
                    {
                      "tilleggsopplysninger": [
                        {
                        "nokkel":"eessi_pensjon_bucid",
                        "verdi":"1111"
                        }
                      ],
                      "journalpostId": "439532144",
                      "datoOpprettet": "2018-06-08T17:06:58",
                      "tittel": "journalpost tittel",
                      "tema": "SYK",
                      "dokumenter": [
                        {
                          "dokumentInfoId": "453708906",
                          "tittel": "P2000 alderpensjon",
                          "dokumentvarianter": [
                            {
                              "filnavn": "enfil.pdf",
                              "variantformat": "ARKIV"
                            },
                            {
                              "filnavn": "enfil.png",
                              "variantformat": "PRODUKSJON"
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
              }
            }
        """.trimIndent()
        val metadata = mapJsonToAny(metadataJson, typeRefs<HentMetadataResponse>())

        doReturn(metadata).`when`(safClient).hentDokumentMetadata(any())

        assert(vedleggService.hentDokumentMetadata("12345678910", "439532144", "453708906")?.tittel == "P2000 alderpensjon" )
    }

}

