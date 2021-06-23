package no.nav.eessi.pensjon.integrationtest.sed

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.fagmodul.prefill.PersonPDLMock
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.model.AktoerId
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentType
import no.nav.eessi.pensjon.personoppslag.pdl.model.NorskIdent
import no.nav.eessi.pensjon.security.sts.STSService
import no.nav.eessi.pensjon.services.kodeverk.KodeverkClient
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestTemplate

@SpringBootTest(classes = [UnsecuredWebMvcTestLauncher::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["unsecured-webmvctest"])
@AutoConfigureMockMvc
class PrefillUfoereIntegrationTest {

    @MockkBean
    lateinit var stsService: STSService

    @MockkBean(name = "pensjonsinformasjonOidcRestTemplate")
    lateinit var restTemplate: RestTemplate

    @MockkBean
    lateinit var personService: PersonService

    @MockkBean
    lateinit var kodeverkClient: KodeverkClient

    @Autowired
    private lateinit var mockMvc: MockMvc

    private companion object {
        const val FNR_VOKSEN = "11067122781"    // KRAFTIG VEGGPRYD
        const val FNR_VOKSEN_2 = "12312312312"  //
        const val AKTOER_ID = "0123456789000"
    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P2200 ufoere med AVSL skal returnere valid sedjson`() {

        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()
        every { restTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java))} returns PrefillTestHelper.readXMLresponse("P2200-AVSL.xml")
        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val apijson = dummyApijson(sakid = "22922563", aktoerId = AKTOER_ID, sed = "P2200")
        val result = mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val validResponse = """
                    {
                      "sed" : "P2200",
                      "sedGVer" : "4",
                      "sedVer" : "2",
                      "nav" : {
                        "eessisak" : [ {
                          "institusjonsid" : "NO:noinst002",
                          "institusjonsnavn" : "NOINST002, NO INST002, NO",
                          "saksnummer" : "22922563",
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
                            "land" : "NO"
                          }
                        },
                        "krav" : {
                          "dato" : "2020-07-01"
                        }
                      },
                      "pensjon" : {
                        "kravDato" : {
                          "dato" : "2020-07-01"
                        }
                      }
                    }            
        """.trimIndent()
        JSONAssert.assertEquals(response, validResponse, false)
    }

    @Test
    fun `prefill sed med kravtype førstehangbehandling norge men med vedtak bodsatt utland skal prefylle sed`() {

        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN_2)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_2)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN_2)
        every { restTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns
                PrefillTestHelper.readXMLresponse("P2200-UP-INNV.xml") andThen
                PrefillTestHelper.readXMLVedtakresponse("P6000-APUtland-301.xml")
        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val apijson = dummyApijson(sakid = "22874955", aktoerId = AKTOER_ID, vedtakid = "5134513451345", sed = "P2200")

        val result = mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val validResponse = """
            {
              "sed" : "P2200",
              "sedGVer" : "4",
              "sedVer" : "2",
              "nav" : {
                "eessisak" : [ {
                  "institusjonsid" : "NO:noinst002",
                  "institusjonsnavn" : "NOINST002, NO INST002, NO",
                  "saksnummer" : "22874955",
                  "land" : "NO"
                } ],
                "bruker" : {
                  "person" : {
                    "pin" : [ {
                      "institusjonsnavn" : "NOINST002, NO INST002, NO",
                      "institusjonsid" : "NO:noinst002",
                      "identifikator" : "$FNR_VOKSEN_2",
                      "land" : "NO"
                    } ],
                    "statsborgerskap" : [ {
                      "land" : "QX"
                    } ],
                    "etternavn" : "Gjenlev",
                    "fornavn" : "Lever",
                    "kjoenn" : "M",
                    "foedselsdato" : "1988-07-12"
                  },
                  "adresse" : {
                    "gate" : "Oppoverbakken 66",
                    "by" : "SØRUMSAND",
                    "postnummer" : "1920",
                    "land" : "NO"
                  }
                },
                "krav" : {
                  "dato" : "2019-07-15"
                }
              },
              "pensjon" : {
                "kravDato" : {
                  "dato" : "2019-07-15"
                }
              }
            }
        """.trimIndent()
        JSONAssert.assertEquals(response, validResponse, true)
    }


    private fun dummyApijson(sakid: String, vedtakid: String? = "", aktoerId: String, sed: String? = "P2000", buc: String? = "P_BUC_06", subject: String? = null, refperson: String? = null): String {
        return """
            {
              "sakId" : "$sakid",
              "vedtakId" : "$vedtakid",
              "kravId" : null,
              "aktoerId" : "$aktoerId",
              "fnr" : null,
              "avdodfnr" : null,
              "payload" : null,
              "buc" : "$buc",
              "sed" : "$sed",
              "documentid" : null,
              "euxCaseId" : "123123",
              "institutions" : [],
              "subjectArea" : "Pensjon",
              "skipSEDkey" : null,
              "referanseTilPerson" : $refperson,
              "subject" : $subject
            }
        """.trimIndent()
    }
}
