package no.nav.eessi.pensjon.integrationtest

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.fagmodul.eux.basismodel.Properties
import no.nav.eessi.pensjon.fagmodul.eux.basismodel.Rinasak
import no.nav.eessi.pensjon.fagmodul.eux.basismodel.Traits
import no.nav.eessi.pensjon.fagmodul.eux.bucmodel.Buc
import no.nav.eessi.pensjon.fagmodul.eux.bucmodel.DocumentsItem
import no.nav.eessi.pensjon.personoppslag.aktoerregister.AktoerId
import no.nav.eessi.pensjon.personoppslag.aktoerregister.AktoerregisterService
import no.nav.eessi.pensjon.personoppslag.aktoerregister.IdentGruppe
import no.nav.eessi.pensjon.personoppslag.aktoerregister.NorskIdent
import no.nav.eessi.pensjon.security.sts.STSService
import no.nav.eessi.pensjon.services.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.utils.toJson
import no.nav.eessi.pensjon.vedlegg.client.BrukerId
import no.nav.eessi.pensjon.vedlegg.client.BrukerIdType
import no.nav.eessi.pensjon.vedlegg.client.SafRequest
import no.nav.eessi.pensjon.vedlegg.client.Variables
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponents
import org.springframework.web.util.UriComponentsBuilder
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.Month

@SpringBootTest(classes = [UnsecuredWebMvcTestLauncher::class] ,webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["unsecured-webmvctest"])
@AutoConfigureMockMvc
class BucIntegrationSpringTest {

    @MockBean
    lateinit var stsService: STSService

    @MockBean
    lateinit var aktoerService: AktoerregisterService

    @MockBean(name = "euxOidcRestTemplate")
    lateinit var restEuxTemplate: RestTemplate


    @MockBean(name = "safGraphQlOidcRestTemplate")
    lateinit var restSafTemplate: RestTemplate

    @MockBean
    lateinit var kodeverkClient: KodeverkClient

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `Gett det finnes gjenlevende og en avdød på buc02 så skal det hentes og lever en liste av buc`() {

        val sedjson = String(Files.readAllBytes(Paths.get("src/test/resources/json/nav/P2100-PinNO-NAV.json")))

        val gjenlevendeFnr = "1234567890000"
        val gjenlevendeAktoerId = "1123123123123123"
        val avdodFnr = "01010100001"

        //gjenlevende aktoerid -> gjenlevendefnr
        doReturn(NorskIdent(gjenlevendeFnr)).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId(gjenlevendeAktoerId))

        //buc02 - avdød rinasak
        val rinaSakerBuc02 = listOf(dummyRinasak("1010", "P_BUC_02"))
        val rinaBuc02url = dummyRinasakAvdodUrl(avdodFnr, "P_BUC_02")
        doReturn( ResponseEntity.ok().body(rinaSakerBuc02.toJson()) ).whenever(restEuxTemplate).exchange( eq(rinaBuc02url.toUriString()), eq(HttpMethod.GET), eq(null), eq(String::class.java))

        //buc05 avdød rinasak
        val rinaBuc05url = dummyRinasakAvdodUrl(avdodFnr, "P_BUC_05")
        doReturn( ResponseEntity.ok().body( emptyList<Rinasak>().toJson()) ).whenever(restEuxTemplate).exchange( eq(rinaBuc05url.toUriString()), eq(HttpMethod.GET), eq(null), eq(String::class.java))

        //gjenlevende rinasak
        val rinaGjenlevUrl = dummyRinasakUrl(gjenlevendeFnr, null, null, null)
        doReturn( ResponseEntity.ok().body( emptyList<Rinasak>().toJson())).whenever(restEuxTemplate).exchange( eq(rinaGjenlevUrl.toUriString()), eq(HttpMethod.GET), eq(null), eq(String::class.java))

        //dummy date
        val lastupdate = LocalDate.of(2020, Month.AUGUST, 7).toString()

        //buc02
        val docItems = listOf(DocumentsItem(id = "1", creationDate = lastupdate, lastUpdate = lastupdate, status = "sent", type = "P2100"), DocumentsItem(id = "2", creationDate = lastupdate,  lastUpdate = lastupdate, status = "draft", type = "P4000"))
        val buc02 = Buc(id = "1010", processDefinitionName = "P_BUC_02", startDate = lastupdate, lastUpdate = lastupdate,  documents = docItems)

        val rinabucpath = "/buc/1010"
        doReturn( ResponseEntity.ok().body( buc02.toJson() ) ).whenever(restEuxTemplate).exchange( eq(rinabucpath), eq(HttpMethod.GET), eq(null), eq(String::class.java))

        //saf (vedlegg meta) gjenlevende
        val httpEntity = dummyHeader(dummySafReqeust(gjenlevendeAktoerId))
        doReturn( ResponseEntity.ok().body(  dummySafMetaResponse() ) ).whenever(restSafTemplate).exchange(eq("/"), eq(HttpMethod.POST), eq(httpEntity), eq(String::class.java))

        //buc02 sed
        val rinabucdocumentidpath = "/buc/1010/sed/1"
        doReturn( ResponseEntity.ok().body( sedjson ) ).whenever(restEuxTemplate).exchange( eq(rinabucdocumentidpath), eq(HttpMethod.GET), eq(null), eq(String::class.java))

        val result = mockMvc.perform(get("/buc/detaljer/$gjenlevendeAktoerId/avdod/$avdodFnr")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        verify(restEuxTemplate, times(1)).exchange("/rinasaker?fødselsnummer=01010100001&rinasaksnummer=&buctype=P_BUC_02&status=\"open\"", HttpMethod.GET, null, String::class.java)
        verify(restEuxTemplate, times(1)).exchange("/rinasaker?fødselsnummer=01010100001&rinasaksnummer=&buctype=P_BUC_05&status=\"open\"", HttpMethod.GET, null, String::class.java)
        verify(restEuxTemplate, times(1)).exchange("/rinasaker?fødselsnummer=1234567890000&rinasaksnummer=&buctype=&status=", HttpMethod.GET, null, String::class.java)
        verify(restEuxTemplate, times(1)).exchange("/buc/1010", HttpMethod.GET, null, String::class.java)
        verify(restEuxTemplate, times(1)).exchange("/buc/1010/sed/1", HttpMethod.GET, null, String::class.java)
        verify(restSafTemplate, times(1)).exchange(eq("/"), eq(HttpMethod.POST), eq(httpEntity), eq(String::class.java))

        JSONAssert.assertEquals(response, caseOneExpected(), false)

    }

    @Test
    fun `Gitt det finnes en gjenlevende og avdød hvor buc02 og buc05 finnes skal det returneres en liste av buc`() {

        val sedP2100json = String(Files.readAllBytes(Paths.get("src/test/resources/json/nav/P2100-PinNO-NAV.json")))
        val sedP8000json = String(Files.readAllBytes(Paths.get("src/test/resources/json/nav/P8000_NO-NAV.json")))

        val gjenlevendeFnr = "1234567890000"
        val gjenlevendeAktoerId = "1123123123123123"
        val avdodFnr = "01010100001"

        //gjenlevende aktoerid -> gjenlevendefnr
        doReturn(NorskIdent(gjenlevendeFnr)).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId(gjenlevendeAktoerId))

        //buc02 - avdød rinasak
        val rinaSakerBuc02 = listOf(dummyRinasak("1010", "P_BUC_02"))
        val rinaBuc02url = dummyRinasakAvdodUrl(avdodFnr, "P_BUC_02")
        doReturn( ResponseEntity.ok().body(rinaSakerBuc02.toJson()) ).whenever(restEuxTemplate).exchange( eq(rinaBuc02url.toUriString()), eq(HttpMethod.GET), eq(null), eq(String::class.java))

        //buc05 avdød rinasak
        val rinaSakerBuc05 = listOf(dummyRinasak("2020", "P_BUC_05"))
        val rinaBuc05url = dummyRinasakAvdodUrl(avdodFnr, "P_BUC_05")
        doReturn( ResponseEntity.ok().body( rinaSakerBuc05.toJson()) ).whenever(restEuxTemplate).exchange( eq(rinaBuc05url.toUriString()), eq(HttpMethod.GET), eq(null), eq(String::class.java))

        //gjenlevende rinasak
        val rinaGjenlevUrl = dummyRinasakUrl(gjenlevendeFnr, null, null, null)
        doReturn( ResponseEntity.ok().body( emptyList<Rinasak>().toJson())).whenever(restEuxTemplate).exchange( eq(rinaGjenlevUrl.toUriString()), eq(HttpMethod.GET), eq(null), eq(String::class.java))

        //dummy date
        val lastupdate = LocalDate.of(2020, Month.AUGUST, 7).toString()

        //buc02
        val docItems = listOf(DocumentsItem(id = "1", creationDate = lastupdate, lastUpdate = lastupdate, status = "sent", type = "P2100"), DocumentsItem(id = "2", creationDate = lastupdate,  lastUpdate = lastupdate, status = "draft", type = "P4000"))
        val buc02 = Buc(id = "1010", processDefinitionName = "P_BUC_02", startDate = lastupdate, lastUpdate = lastupdate,  documents = docItems)

        val rinabucpath = "/buc/1010"
        doReturn( ResponseEntity.ok().body( buc02.toJson() ) ).whenever(restEuxTemplate).exchange( eq(rinabucpath), eq(HttpMethod.GET), eq(null), eq(String::class.java))

        //buc05
        val doc05Items = listOf(DocumentsItem(id = "1", creationDate = lastupdate, lastUpdate = lastupdate, status = "sent", type = "P8000"), DocumentsItem(id = "2", creationDate = lastupdate,  lastUpdate = lastupdate, status = "draft", type = "P4000"))
        val buc05 = Buc(id = "2020", processDefinitionName = "P_BUC_05", startDate = lastupdate, lastUpdate = lastupdate,  documents = doc05Items)

        val rinabuc05path = "/buc/2020"
        doReturn( ResponseEntity.ok().body( buc05.toJson() ) ).whenever(restEuxTemplate).exchange( eq(rinabuc05path), eq(HttpMethod.GET), eq(null), eq(String::class.java))

        //saf (vedlegg meta) gjenlevende
        val httpEntity = dummyHeader(dummySafReqeust(gjenlevendeAktoerId))
        doReturn( ResponseEntity.ok().body(  dummySafMetaResponse() ) ).whenever(restSafTemplate).exchange(eq("/"), eq(HttpMethod.POST), eq(httpEntity), eq(String::class.java))

        //buc02 sed
        val rinabucdocumentidpath = "/buc/1010/sed/1"
        doReturn( ResponseEntity.ok().body( sedP2100json ) ).whenever(restEuxTemplate).exchange( eq(rinabucdocumentidpath), eq(HttpMethod.GET), eq(null), eq(String::class.java))

        //buc05 sed
        val rinabuc05documentidpath = "/buc/2020/sed/1"
        doReturn( ResponseEntity.ok().body( sedP8000json ) ).whenever(restEuxTemplate).exchange( eq(rinabuc05documentidpath), eq(HttpMethod.GET), eq(null), eq(String::class.java))


        val result = mockMvc.perform(get("/buc/detaljer/$gjenlevendeAktoerId/avdod/$avdodFnr")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        verify(restEuxTemplate, times(1)).exchange("/rinasaker?fødselsnummer=01010100001&rinasaksnummer=&buctype=P_BUC_02&status=\"open\"", HttpMethod.GET, null, String::class.java)
        verify(restEuxTemplate, times(1)).exchange("/rinasaker?fødselsnummer=01010100001&rinasaksnummer=&buctype=P_BUC_05&status=\"open\"", HttpMethod.GET, null, String::class.java)
        verify(restEuxTemplate, times(1)).exchange("/rinasaker?fødselsnummer=1234567890000&rinasaksnummer=&buctype=&status=", HttpMethod.GET, null, String::class.java)
        verify(restEuxTemplate, times(1)).exchange("/buc/1010", HttpMethod.GET, null, String::class.java)
        verify(restEuxTemplate, times(1)).exchange("/buc/1010/sed/1", HttpMethod.GET, null, String::class.java)
        verify(restEuxTemplate, times(1)).exchange("/buc/2020", HttpMethod.GET, null, String::class.java)
        verify(restEuxTemplate, times(1)).exchange("/buc/2020/sed/1", HttpMethod.GET, null, String::class.java)
        verify(restSafTemplate, times(1)).exchange(eq("/"), eq(HttpMethod.POST), eq(httpEntity), eq(String::class.java))

        JSONAssert.assertEquals(response, csseTwoExpected(), false)

    }


    private fun caseOneExpected(): String {
        return """
            [{"type":"P_BUC_02","caseId":"1010","creator":{"country":"","institution":"","name":null},"sakType":null,"status":null,"startDate":1596751200000,"lastUpdate":1596751200000,"institusjon":[],"seds":[{"id":"1","parentDocumentId":null,"type":"P2100","status":"sent","creationDate":1596751200000,"lastUpdate":1596751200000,"displayName":null,"participants":null,"attachments":[],"version":"1","firstVersion":null,"lastVersion":null,"allowsAttachments":null,"message":null},{"id":"2","parentDocumentId":null,"type":"P4000","status":"draft","creationDate":1596751200000,"lastUpdate":1596751200000,"displayName":null,"participants":null,"attachments":[],"version":"1","firstVersion":null,"lastVersion":null,"allowsAttachments":null,"message":null}],"error":null,"readOnly":false,"subject":{"gjenlevende":{"fnr":"1234567890000"},"avdod":{"fnr":"01010100001"}}}]
        """.trimIndent()
    }

    private fun csseTwoExpected(): String {
        return """
            [{"type":"P_BUC_02","caseId":"1010","creator":{"country":"","institution":"","name":null},"sakType":null,"status":null,"startDate":1596751200000,"lastUpdate":1596751200000,"institusjon":[],"seds":[{"id":"1","parentDocumentId":null,"type":"P2100","status":"sent","creationDate":1596751200000,"lastUpdate":1596751200000,"displayName":null,"participants":null,"attachments":[],"version":"1","firstVersion":null,"lastVersion":null,"allowsAttachments":null,"message":null},{"id":"2","parentDocumentId":null,"type":"P4000","status":"draft","creationDate":1596751200000,"lastUpdate":1596751200000,"displayName":null,"participants":null,"attachments":[],"version":"1","firstVersion":null,"lastVersion":null,"allowsAttachments":null,"message":null}],"error":null,"readOnly":false,"subject":{"gjenlevende":{"fnr":"1234567890000"},"avdod":{"fnr":"01010100001"}}},{"type":"P_BUC_05","caseId":"2020","creator":{"country":"","institution":"","name":null},"sakType":null,"status":null,"startDate":1596751200000,"lastUpdate":1596751200000,"institusjon":[],"seds":[{"id":"1","parentDocumentId":null,"type":"P8000","status":"sent","creationDate":1596751200000,"lastUpdate":1596751200000,"displayName":null,"participants":null,"attachments":[],"version":"1","firstVersion":null,"lastVersion":null,"allowsAttachments":null,"message":null},{"id":"2","parentDocumentId":null,"type":"P4000","status":"draft","creationDate":1596751200000,"lastUpdate":1596751200000,"displayName":null,"participants":null,"attachments":[],"version":"1","firstVersion":null,"lastVersion":null,"allowsAttachments":null,"message":null}],"error":null,"readOnly":false,"subject":{"gjenlevende":{"fnr":"1234567890000"},"avdod":{"fnr":"01010100001"}}}]            
        """.trimIndent()
    }

    private fun dummyHeader(value: String?): HttpEntity<String> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        return HttpEntity(value, headers)
    }

    private fun dummySafReqeust(aktoerId: String): String {
            val request = SafRequest(variables = Variables(BrukerId(aktoerId, BrukerIdType.AKTOERID), 10000))
            return request.toJson()
    }

    private fun dummySafMetaResponse(): String {
        return """
            {
              "data": {
                "dokumentoversiktBruker": {
                  "journalposter": [
                    {
                      "tilleggsopplysninger": [],
                      "journalpostId": "439532144",
                      "datoOpprettet": "2018-06-08T17:06:58",
                      "tittel": "MASKERT_FELT",
                      "tema": "PEN",
                      "dokumenter": []
                    }
                  ]
                }
              }
            }
        """.trimIndent()
    }

    private fun dummyRinasakAvdodUrl(avod: String, bucType: String? = "P_BUC_02", status: String? =  "\"open\"") = dummyRinasakUrl(avod, bucType, null, status)
    private fun dummyRinasakUrl(fnr: String, bucType: String? = null, euxCaseId: String? = null, status: String? = null) : UriComponents {
        val uriComponent = UriComponentsBuilder.fromPath("/rinasaker")
                .queryParam("fødselsnummer", fnr)
                .queryParam("rinasaksnummer", euxCaseId ?: "")
                .queryParam("buctype", bucType ?: "")
                .queryParam("status", status ?: "")
                .build()
        return uriComponent
    }

    private fun dummyRinasak(rinaSakId: String, bucType: String): Rinasak {
        return Rinasak(rinaSakId, bucType, Traits(), "", Properties(), "open")
    }
}