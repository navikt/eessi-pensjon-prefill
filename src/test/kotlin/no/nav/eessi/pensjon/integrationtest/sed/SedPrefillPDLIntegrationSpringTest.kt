package no.nav.eessi.pensjon.integrationtest.sed

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.eux.model.BucType.P_BUC_02
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.integrationtest.IntegrasjonsTestConfig
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.kodeverk.Postnummer
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.model.AktoerId
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentGruppe.AKTORID
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentGruppe.FOLKEREGISTERIDENT
import no.nav.eessi.pensjon.personoppslag.pdl.model.NorskIdent
import no.nav.eessi.pensjon.prefill.KrrService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.PesysService
import no.nav.eessi.pensjon.prefill.models.DigitalKontaktinfo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestTemplate


//Daniel
@SpringBootTest(classes = [IntegrasjonsTestConfig::class, UnsecuredWebMvcTestLauncher::class, SedPrefillPDLIntegrationSpringTest.TestConfig::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("unsecured-webmvctest", "excludeKodeverk")
@AutoConfigureMockMvc
@DirtiesContext
@EmbeddedKafka
class SedPrefillPDLIntegrationSpringTest {

    @MockkBean
    lateinit var kodeverkClient: KodeverkClient

    @MockkBean
    lateinit var personService: PersonService

    @MockkBean
    lateinit var krrService: KrrService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var pesysService: PesysService

    companion object {
        const val FNR_VOKSEN = "11067122781"    // KRAFTIG VEGGPRYD
        const val FNR_VOKSEN_2 = "22117320034"  // LEALAUS KAKE

        const val AKTOER_ID = "0123456789000"
        const val AKTOER_ID_2 = "0009876543210"
    }

    @TestConfiguration
    internal class TestConfig{
        @Bean
        @Primary
        fun restTemplate(): RestTemplate = mockk()

        @Bean
        fun pesysClientRestTemplate(): RestTemplate = mockk()

        @Bean
        fun pesysService() = mockk<PesysService>()
    }

    @BeforeEach
    fun setUp() {
        every { kodeverkClient.finnLandkode(any()) } returns "QX"
        every { kodeverkClient.hentPostSted(any()) } returns Postnummer("1068", "SØRUMSAND")
    }


    @Test
    @Throws(Exception::class)
    fun `prefill sed P2000 alder return valid sedjson`() {
        every { pesysService.hentP2000data(any(),any(),any()) } returns XmlToP2xxxMapper.readP2000FromXml("/pensjonsinformasjon/krav/P2000-AP-UP-21337890.xml")
        every { kodeverkClient.finnLandkode(any()) } returns "QX"
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, fnr = FNR_VOKSEN, aktoerid = AKTOER_ID)
        every { krrService.hentPersonerFraKrr(any()) } returns DigitalKontaktinfo(
            epostadresse = "melleby11@melby.no",
            aktiv = true,
            kanVarsles = true,
            reservert = false,
            mobiltelefonnummer = "11111111",
            personident = FNR_VOKSEN
        )

        val apijson = dummyApijson(sedType = SedType.P2000, sakid = "21337890", aktoerId = AKTOER_ID, vedtakid = "21337890")

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
                }, {
                  "identifikator" : "123123123",
                  "land" : "QX"
                } ],
                "statsborgerskap" : [ {
                  "land" : "QX"
                } ],
                "etternavn" : "Testesen",
                "fornavn" : "Test",
                "kjoenn" : "M",
                "foedselsdato" : "1988-07-12",
                "sivilstand" : [ {
                  "fradato" : "2000-10-01",
                  "status" : "enslig"
                } ],
                "kontakt" : {
                  "telefon" : [ {
                    "type" : "mobil",
                    "nummer" : "11111111"
                  } ],
                  "email" : [ {
                    "adresse" : "melleby11@melby.no"
                  } ]
                }
              },
              "adresse" : {
                "gate" : "Oppoverbakken 66",
                "by" : "SØRUMSAND",
                "postnummer" : "1920",
                "land" : "NO"
              }
            },
            "krav" : {
              "dato" : "2014-06-04"
            }
          },
          "pensjon" : {
            "ytelser" : [ {
              "mottasbasertpaa" : "botid",
              "ytelse" : "10",
              "status" : "02"
            } ],
            "kravDato" : {
              "dato" : "2014-06-04"
            },
            "etterspurtedokumenter" : "P5000 and P6000"
          },
          "sedGVer" : "4",
          "sedVer" : "2"
        }         
        """.trimIndent()

        println("**** $actual")
        JSONAssert.assertEquals(excpected, actual , true)

    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P2100 P_BUC_02 Gjenlevende har med avdod skal returnere en gyldig SED`() {

        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentIdent(AKTORID, NorskIdent(FNR_VOKSEN_2)) } returns AktoerId(AKTOER_ID_2)

        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN, AKTOER_ID)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_2)) } returns PersonPDLMock.createWith(true, "Avdød", "Død", FNR_VOKSEN_2, AKTOER_ID_2, true)

        every { krrService.hentPersonerFraKrr(eq(FNR_VOKSEN)) } returns DigitalKontaktinfo(epostadresse = "melleby11@melby.no", true, true, false, "22603511", FNR_VOKSEN)
        every { krrService.hentPersonerFraKrr(eq(FNR_VOKSEN_2)) } returns DigitalKontaktinfo(epostadresse = "melleby11@melby.no", true, true, false, "22603522", FNR_VOKSEN_2)

        every { pesysService.hentP2100data(any(),any(),any()) } returns XmlToP2xxxMapper.readP2100FromXml("/pensjonsinformasjon/krav/P2100-GL-UTL-INNV.xml")

        val apijson = dummyApijson(sakid = "22874955", aktoerId = AKTOER_ID, sedType = SedType.P2100, buc = P_BUC_02, fnravdod = FNR_VOKSEN_2, vedtakid = "22874955")

        val result = mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val actual = result.response.getContentAsString(charset("UTF-8"))
        println("**** $actual")

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
                    }, {
                      "identifikator" : "123123123",
                      "land" : "QX"
                    } ],
                    "statsborgerskap" : [ {
                      "land" : "QX"
                    } ],
                    "etternavn" : "Død",
                    "fornavn" : "Avdød",
                    "kjoenn" : "M",
                    "foedselsdato" : "1921-07-12",
                    "sivilstand" : [ {
                      "fradato" : "2000-10-01",
                      "status" : "enslig"
                    } ],
                    "kontakt" : {
                      "telefon" : [ {
                        "type" : "mobil",
                        "nummer" : "22603511"
                      } ],
                      "email" : [ {
                        "adresse" : "melleby11@melby.no"
                      } ]
                    }
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
                    }, {
                      "identifikator" : "123123123",
                      "land" : "QX"
                    } ],
                    "statsborgerskap" : [ {
                      "land" : "QX"
                    } ],
                    "etternavn" : "Gjenlev",
                    "fornavn" : "Lever",
                    "kjoenn" : "M",
                    "foedselsdato" : "1988-07-12",
                    "sivilstand" : [ {
                      "fradato" : "2000-10-01",
                      "status" : "enslig"
                    } ],
                    "kontakt" : {
                      "telefon" : [ {
                        "type" : "mobil",
                        "nummer" : "22603511"
                      } ],
                      "email" : [ {
                        "adresse" : "melleby11@melby.no"
                      } ]
                    }
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
    fun `prefill sed P2000 alder return valid sedjson check tps to pdl`() {
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()
        every { krrService.hentPersonerFraKrr(any()) } returns DigitalKontaktinfo(epostadresse = "melleby11@melby.no", true, true, false, "11111111", FNR_VOKSEN)

//        every {pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java))  } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2000-AP-UP-21337890.xml")
        every { pesysService.hentP2000data(any(),any(),any()) } returns XmlToP2xxxMapper.readP2000FromXml("/pensjonsinformasjon/krav/P2000-AP-UP-21337890.xml")

        val apijson = dummyApijson(sedType = SedType.P2000, sakid = "21337890", aktoerId = AKTOER_ID, vedtakid = "21337890")

        val validResponse = """
        {
          "sed" : "P2000",
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
                }, {
                  "identifikator" : "123123123",
                  "land" : "QX"
                } ],
                "statsborgerskap" : [ {
                  "land" : "QX"
                } ],
                "etternavn" : "Testesen",
                "fornavn" : "Test",
                "kjoenn" : "M",
                "foedselsdato" : "1988-07-12",
                "sivilstand" : [ {
                  "fradato" : "2000-10-01",
                  "status" : "enslig"
                } ],
                "kontakt" : {
                  "telefon" : [ {
                    "type" : "mobil",
                    "nummer" : "11111111"
                  } ],
                  "email" : [ {
                    "adresse" : "melleby11@melby.no"
                  } ]
                }
              },
              "adresse" : {
                "gate" : "Oppoverbakken 66",
                "by" : "SØRUMSAND",
                "postnummer" : "1920",
                "land" : "NO"
              }
            },
            "krav" : {
              "dato" : "2014-06-04"
            }
          },
          "pensjon" : {
            "kravDato" : {
              "dato" : "2014-06-04"
            }
          },
          "sedGVer" : "4",
          "sedVer" : "2"
        }
        """.trimIndent()

        val result = mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))
        JSONAssert.assertEquals(validResponse, response, false)

    }

}
