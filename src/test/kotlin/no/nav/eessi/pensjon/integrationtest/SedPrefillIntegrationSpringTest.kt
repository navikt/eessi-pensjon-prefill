package no.nav.eessi.pensjon.integrationtest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.fagmodul.personoppslag.BrukerMock
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper
import no.nav.eessi.pensjon.personoppslag.aktoerregister.AktoerId
import no.nav.eessi.pensjon.personoppslag.aktoerregister.AktoerregisterService
import no.nav.eessi.pensjon.personoppslag.aktoerregister.IdentGruppe
import no.nav.eessi.pensjon.personoppslag.aktoerregister.NorskIdent
import no.nav.eessi.pensjon.personoppslag.personv3.PersonV3Service
import no.nav.eessi.pensjon.security.sts.STSService
import no.nav.eessi.pensjon.services.kodeverk.KodeverkClient
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpEntity
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestTemplate

@SpringBootTest(classes = [UnsecuredWebMvcTestLauncher::class] ,webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["unsecured-webmvctest"])
@AutoConfigureMockMvc
class SedPrefillIntegrationSpringTest {

    @MockBean
    lateinit var stsService: STSService

    @MockBean
    lateinit var personV3Service: PersonV3Service

    @MockBean
    lateinit var aktoerService: AktoerregisterService

    @MockBean(name = "pensjonsinformasjonOidcRestTemplate")
    lateinit var restTemplate: RestTemplate

    @MockBean
    lateinit var kodeverkClient: KodeverkClient

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    @Throws(Exception::class)
    fun `preview sed P6000 missing vedtakid throw error bad request and reason Mangler vedtakID`() {

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))

        val apijson = """
            {
              "sakId" : "EESSI-PEN-123",
              "vedtakId" : "",
              "kravId" : null,
              "aktoerId" : "0105094340092",
              "fnr" : null,
              "avdodfnr" : null,
              "payload" : null,
              "buc" : "P_BUC_06",
              "sed" : "P6000",
              "documentid" : null,
              "euxCaseId" : "123123",
              "institutions" : [ {
                "country" : "FI",
                "institution" : "FI:Finland",
                "name" : "Finland test"
              } ],
              "subjectArea" : "Pensjon",
              "skipSEDkey" : null
            }
        """.trimIndent()

        mockMvc.perform(post("/sed/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andDo(print())
                .andExpect(status().isBadRequest)
                .andExpect(status().reason(Matchers.containsString("Mangler vedtakID")))

    }

    @Test
    @Throws(Exception::class)
    fun `preview sed P2000 missing saksnummer throw error bad request and reason Mangler sakId`() {

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(BrukerMock.createWith()).`when`(personV3Service).hentBruker(any())

        val apijson = """
            {
              "sakId" : "",
              "vedtakId" : "",
              "kravId" : null,
              "aktoerId" : "0105094340092",
              "fnr" : null,
              "avdodfnr" : null,
              "payload" : null,
              "buc" : "P_BUC_01",
              "sed" : "P2000",
              "documentid" : null,
              "euxCaseId" : "123123",
              "institutions" : [ {
                "country" : "FI",
                "institution" : "FI:Finland",
                "name" : "Finland test"
              } ],
              "subjectArea" : "Pensjon",
              "skipSEDkey" : null
            }
        """.trimIndent()

        mockMvc.perform(post("/sed/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andDo(print())
                .andExpect(status().isBadRequest)
                .andExpect(status().reason(Matchers.containsString("Mangler sakId")))

    }


    @Test
    @Throws(Exception::class)
    fun `preview sed P2000 alder with uføre pensjondata throw error bad request and mesage Du kan ikke opprette alderspensjonskrav i en uføretrygdsak`() {

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(BrukerMock.createWith()).`when`(personV3Service).hentBruker(any())
        doReturn(PrefillTestHelper.readXMLresponse("P2200-UP-INNV.xml")).`when`(restTemplate).exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))

        val apijson = """
            {
              "sakId" : "22874955",
              "vedtakId" : "",
              "kravId" : null,
              "aktoerId" : "0105094340092",
              "fnr" : null,
              "avdodfnr" : null,
              "payload" : null,
              "buc" : "P_BUC_06",
              "sed" : "P2000",
              "documentid" : null,
              "euxCaseId" : "123123",
              "institutions" : [ {
                "country" : "FI",
                "institution" : "FI:Finland",
                "name" : "Finland test"
              } ],
              "subjectArea" : "Pensjon",
              "skipSEDkey" : null
            }
        """.trimIndent()

        mockMvc.perform(post("/sed/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andDo(print())
                .andExpect(status().isBadRequest)
                .andExpect(status().reason(Matchers.containsString("Du kan ikke opprette alderspensjonskrav i en uføretrygdsak (PESYS-saksnr: 22874955 har sakstype UFOREP)")))

    }

    @Test
    @Throws(Exception::class)
    fun `preview sed P5000 with missing avdodfnr and Subject throws error bad request`() {

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))

        val apijson = """
            {
              "sakId" : "EESSI-PEN-123",
              "vedtakId" : "",
              "kravId" : null,
              "aktoerId" : "0105094340092",
              "fnr" : null,
              "avdodfnr" : null,
              "payload" : null,
              "buc" : "P_BUC_02",
              "sed" : "P5000",
              "documentid" : null,
              "euxCaseId" : "123123",
              "institutions" : [ {
                "country" : "FI",
                "institution" : "FI:Finland",
                "name" : "Finland test"
              } ],
              "subjectArea" : "Pensjon",
              "skipSEDkey" : null
            }
        """.trimIndent()

        mockMvc.perform(post("/sed/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andDo(print())
                .andExpect(status().isBadRequest)
                .andExpect(status().reason(Matchers.containsString("Mangler fnr for avdød")))

    }

    @Test
    @Throws(Exception::class)
    fun `preview sed P5000 P_BUC_02 Gjenlevende har med avdod skal returnere en gyldig SED`() {

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(AktoerId("3323332333233323")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.AktoerId, NorskIdent("9876543210"))

        doReturn(BrukerMock.createWith(true,"Lever", "Gjenlev", "12312312312")).`when`(personV3Service).hentBruker("12312312312")
        doReturn(BrukerMock.createWith(true, "Avdød", "Død", "9876543210")).`when`(personV3Service).hentBruker("9876543210")

        doReturn(PrefillTestHelper.readXMLresponse("P2100-GL-UTL-INNV.xml")).`when`(restTemplate).exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))

        doReturn("QX").doReturn("XQ").`when`(kodeverkClient).finnLandkode2(any())

        val apijson = """
            {
              "sakId" : "22874955",
              "vedtakId" : "",
              "kravId" : null,
              "aktoerId" : "0105094340092",
              "fnr" : null,
              "avdodfnr" : null,
              "payload" : null,
              "buc" : "P_BUC_02",
              "sed" : "P5000",
              "documentid" : null,
              "euxCaseId" : "123123",
              "institutions" : [ {
                "country" : "FI",
                "institution" : "FI:Finland",
                "name" : "Finland test"
              } ],
              "subjectArea" : "Pensjon",
              "skipSEDkey" : null,
              "subject" : { "fnr" : "12345678901", "avdod" : "9876543210"}
            }
        """.trimIndent()

        val result = mockMvc.perform(post("/sed/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val mapper = jacksonObjectMapper()
        val sedRootNode = mapper.readTree(response)
        val gjenlevendePIN =  finnPin(sedRootNode.at("/pensjon/gjenlevende/person"))
        val avdodPIN = finnPin(sedRootNode.at("/nav/bruker"))

        Assertions.assertEquals("12312312312", gjenlevendePIN)
        Assertions.assertEquals("9876543210", avdodPIN)

    }

    private fun finnPin(pinNode: JsonNode): String? {
        return pinNode.findValue("pin")
                .filter { pin -> pin.get("land").textValue() == "NO" }
                .map { pin -> pin.get("identifikator").textValue() }
                .lastOrNull()
    }

    @Test
    @Throws(Exception::class)
    fun `preview sed P2000 alder return valid sedjson`() {


        doReturn(NorskIdent("23123123")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(BrukerMock.createWith()).`when`(personV3Service).hentBruker(any())
        doReturn(PrefillTestHelper.readXMLresponse("P2000-AP-UP-21337890.xml")).`when`(restTemplate).exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))
        doReturn("QX").`when`(kodeverkClient).finnLandkode2(any())

        val apijson = """
            {
              "sakId" : "21337890",
              "vedtakId" : "",
              "kravId" : null,
              "aktoerId" : "0105094340092",
              "fnr" : null,
              "avdodfnr" : null,
              "payload" : null,
              "buc" : "P_BUC_06",
              "sed" : "P2000",
              "documentid" : null,
              "euxCaseId" : "123123",
              "institutions" : [ {
                "country" : "FI",
                "institution" : "FI:Finland",
                "name" : "Finland test"
              } ],
              "subjectArea" : "Pensjon",
              "skipSEDkey" : null
            }
        """.trimIndent()

        val validResponse = """
            {
              "sed" : "P2000",
              "sedGVer" : "4",
              "sedVer" : "1",
              "nav" : {
                "eessisak" : [ {
                  "institusjonsid" : "NO:noinst002",
                  "institusjonsnavn" : "NOINST002, NO INST002, NO",
                  "saksnummer" : "21337890",
                  "land" : "NO"
                } ],
                "bruker" : {
                  "person" : {
                    "pin" : [ {
                      "institusjonsnavn" : "NOINST002, NO INST002, NO",
                      "institusjonsid" : "NO:noinst002",
                      "identifikator" : "3123",
                      "land" : "NO"
                    } ],
                    "statsborgerskap" : [ {
                      "land" : "QX"
                    } ],
                    "etternavn" : "Testesen",
                    "fornavn" : "Test",
                    "kjoenn" : "M",
                    "foedselsdato" : "1988-07-12"
                  },
                  "adresse" : {
                    "gate" : "Oppoverbakken 66",
                    "by" : "SØRUMSAND",
                    "postnummer" : "1920",
                    "land" : "QX"
                  }
                },
                "krav" : {
                  "dato" : "2018-06-28"
                }
              },
              "pensjon" : {
                "kravDato" : {
                  "dato" : "2018-06-28"
                }
              }
            }
        """.trimIndent()

        val result = mockMvc.perform(post("/sed/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        JSONAssert.assertEquals(response, validResponse, false)


    }

}
