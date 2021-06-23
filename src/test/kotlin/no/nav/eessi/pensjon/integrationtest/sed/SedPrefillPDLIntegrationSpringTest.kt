package no.nav.eessi.pensjon.integrationtest.sed

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.eux.model.sed.SedType
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
class SedPrefillPDLIntegrationSpringTest {

    @MockkBean
    lateinit var stsService: STSService

    @MockkBean(name = "pensjonsinformasjonOidcRestTemplate")
    lateinit var restTemplate: RestTemplate

    @MockkBean
    lateinit var kodeverkClient: KodeverkClient

    @MockkBean
    lateinit var personService: PersonService

    @Autowired
    private lateinit var mockMvc: MockMvc

    companion object {
        const val FNR_VOKSEN = "11067122781"    // KRAFTIG VEGGPRYD
        const val FNR_VOKSEN_2 = "22117320034"  // LEALAUS KAKE

        const val AKTOER_ID = "0123456789000"
        const val AKTOER_ID_2 = "0009876543210"
    }


    @Test
    @Throws(Exception::class)
    fun `prefill sed P2000 alder return valid sedjson`() {
        every { restTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("P2000-AP-UP-21337890.xml")
        every { kodeverkClient.finnLandkode(any()) } returns "QX"
        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, fnr = FNR_VOKSEN, aktoerid = AKTOER_ID)

        val apijson = dummyApijson(sedType = SedType.P2000, sakid = "21337890", aktoerId = AKTOER_ID)

        val result = mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn()

        val actual = result.response.getContentAsString(charset("UTF-8"))

        val excpected = """
            {
              "sed" : "P2000",
              "sedGVer" : "4",
              "sedVer" : "2",
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
                      "identifikator" : "11067122781",
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
        JSONAssert.assertEquals(excpected, actual , true)

    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P2100 P_BUC_02 Gjenlevende har med avdod skal returnere en gyldig SED`() {

        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentIdent(IdentType.AktoerId, NorskIdent(FNR_VOKSEN_2)) } returns AktoerId(AKTOER_ID_2)

        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN, AKTOER_ID)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_2)) } returns PersonPDLMock.createWith(true, "Avdød", "Død", FNR_VOKSEN_2, AKTOER_ID_2, true)

        every { restTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("P2100-GL-UTL-INNV.xml")

        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val apijson = dummyApijson(sakid = "22874955", aktoerId = AKTOER_ID, sedType = SedType.P2100, buc = "P_BUC_02", fnravdod = FNR_VOKSEN_2)

        val result = mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val actual = result.response.getContentAsString(charset("UTF-8"))

        val expected = """
            {
              "sed" : "P2100",
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
                      "identifikator" : "22117320034",
                      "land" : "NO"
                    } ],
                    "statsborgerskap" : [ {
                      "land" : "QX"
                    } ],
                    "etternavn" : "Død",
                    "fornavn" : "Avdød",
                    "kjoenn" : "M",
                    "foedselsdato" : "1921-07-12"
                  },
                  "adresse" : {
                    "gate" : "Oppoverbakken 66",
                    "by" : "SØRUMSAND",
                    "postnummer" : "1920",
                    "land" : "NO"
                  }                  
                }
              },
              "pensjon" : {
                "gjenlevende" : {
                  "person" : {
                    "pin" : [ {
                      "institusjonsnavn" : "NOINST002, NO INST002, NO",
                      "institusjonsid" : "NO:noinst002",
                      "identifikator" : "11067122781",
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
                }
              }
            }
          """.trimIndent()

        JSONAssert.assertEquals(expected, actual, true)
    }


    @Test
    @Throws(Exception::class)
    fun `prefill sed P2000 alder return valid sedjson checkk tps to pdl`() {
        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()

        every {restTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java))  } returns PrefillTestHelper.readXMLresponse("P2000-AP-UP-21337890.xml")
        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val apijson = dummyApijson(sedType = SedType.P2000, sakid = "21337890", aktoerId = AKTOER_ID)

        val validResponse = """
            {
              "sed" : "P2000",
              "sedGVer" : "4",
              "sedVer" : "2",
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
                    "land" : "NO"
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

        val result = mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))
        JSONAssert.assertEquals(response, validResponse, false)

    }

}
