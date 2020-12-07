package no.nav.eessi.pensjon.integrationtest

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

@SpringBootTest(classes = [UnsecuredWebMvcTestLauncher::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["unsecured-webmvctest"])
@AutoConfigureMockMvc
class PrefillUfoereIntegrationTest {

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
    fun `prefill sed P2200 ufoere med AVSL skal returnere valid sedjson`() {


        doReturn(NorskIdent("23123123")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(BrukerMock.createWith()).`when`(personV3Service).hentBruker(any())
        doReturn(PrefillTestHelper.readXMLresponse("P2200-AVSL.xml")).`when`(restTemplate).exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))
        doReturn("QX").`when`(kodeverkClient).finnLandkode2(any())

        val apijson = dummyApijson(sakid = "22922563", aktoerId = "0105094340092", sed = "P2200")
        val result = mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val validResponse = """
                    {
                      "sed" : "P2200",
                      "sedGVer" : "4",
                      "sedVer" : "1",
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
                            "land" : "QX"
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

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(BrukerMock.createWith(true, "Lever", "Gjenlev", "12312312312")).`when`(personV3Service).hentBruker("12312312312")

        doReturn(PrefillTestHelper.readXMLresponse("P2200-UP-INNV.xml")).
        doReturn(PrefillTestHelper.readXMLVedtakresponse("P6000-APUtland-301.xml")).
        `when`(restTemplate).exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))

        doReturn("QX").doReturn("XQ").`when`(kodeverkClient).finnLandkode2(any())

        val apijson = dummyApijson(sakid = "22874955", aktoerId = "0105094340092", vedtakid = "5134513451345", sed = "P2200")

        val result = mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val validResponse = """
            {
              "sed" : "P2200",
              "sedGVer" : "4",
              "sedVer" : "1",
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
                      "identifikator" : "12312312312",
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
                    "land" : "XQ"
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
