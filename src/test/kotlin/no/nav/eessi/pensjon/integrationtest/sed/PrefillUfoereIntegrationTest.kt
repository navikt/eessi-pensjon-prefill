package no.nav.eessi.pensjon.integrationtest.sed

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.integrationtest.IntegrasjonsTestConfig
import no.nav.eessi.pensjon.personoppslag.Fodselsnummer
import no.nav.eessi.pensjon.personoppslag.FodselsnummerGenerator
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.model.AktoerId
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentType
import no.nav.eessi.pensjon.personoppslag.pdl.model.KjoennType
import no.nav.eessi.pensjon.personoppslag.pdl.model.NorskIdent
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.PersonPDLMock.medBarn
import no.nav.eessi.pensjon.prefill.PersonPDLMock.medFodsel
import no.nav.eessi.pensjon.prefill.PersonPDLMock.medForeldre
import no.nav.eessi.pensjon.prefill.PersonPDLMock.medKjoenn
import no.nav.eessi.pensjon.prefill.PersonPDLMock.medSivilstand
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper
import no.nav.eessi.pensjon.security.sts.STSService
import no.nav.eessi.pensjon.services.kodeverk.KodeverkClient
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestTemplate

@SpringBootTest(classes = [IntegrasjonsTestConfig::class, UnsecuredWebMvcTestLauncher::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["unsecured-webmvctest"])
@AutoConfigureMockMvc
@EmbeddedKafka
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
    fun `prefill sed P2200 ufoere med Barn og sak er AVSL skal returnere valid sedjson med barn`() {
        val pinHovedperson = FodselsnummerGenerator.generateFnrForTest(40)
        val aktoerHovedperson =  pinHovedperson+10
        val pinEktefelledperson = FodselsnummerGenerator.generateFnrForTest(36)

        val pinBarn1 = FodselsnummerGenerator.generateFnrForTest(12)
        val pinBarn2 = FodselsnummerGenerator.generateFnrForTest(18)
        val pinBarn3 = FodselsnummerGenerator.generateFnrForTest(19)

        val hovedPerson = PersonPDLMock.createWith(landkoder = true, "HOVED PERSON", "TESTER", fnr = pinHovedperson, aktoerid = aktoerHovedperson)
            .medFodsel(Fodselsnummer.fra(pinHovedperson)?.getBirthDate()!!)
            .medKjoenn(KjoennType.MANN)

        val ektefellePerson = PersonPDLMock.createWith(true, "JESSINE TORDNU", "BOUWMANS", fnr =  pinEktefelledperson, aktoerid = pinEktefelledperson+11)
            .medFodsel(Fodselsnummer.fra(pinEktefelledperson)?.getBirthDate()!!)
            .medKjoenn(KjoennType.KVINNE)
            .medSivilstand(hovedPerson)

        val barn1 = PersonPDLMock.createWith(true, "TOPPI DOTTO", "UNG", fnr = pinBarn1, aktoerid = pinBarn1+12)
            .medForeldre(hovedPerson)
            .medForeldre(ektefellePerson)
            .medFodsel(Fodselsnummer.fra(pinBarn1)?.getBirthDate()!!)
            .medKjoenn(KjoennType.MANN)

        val barn2 = PersonPDLMock.createWith(true, "EGIDIJS ER", "MED?", fnr = pinBarn2, aktoerid = pinBarn2+18)
            .medForeldre(hovedPerson)
            .medForeldre(ektefellePerson)
            .medKjoenn(KjoennType.KVINNE)
            .medFodsel(Fodselsnummer.fra(pinBarn2)?.getBirthDate()!!)

        val barn3 = PersonPDLMock.createWith(true, "BARN ALT", "GAMMELT", fnr = pinBarn3, aktoerid = pinBarn3+19)
            .medForeldre(hovedPerson)
            .medForeldre(ektefellePerson)
            .medKjoenn(KjoennType.KVINNE)
            .medFodsel(Fodselsnummer.fra(pinBarn3)?.getBirthDate()!!)

        val hovedPersonMedbarn = hovedPerson
            .medBarn(barn1)
            .medBarn(barn2)
            .medBarn(barn3)
            .medSivilstand(ektefellePerson)


        //mock hent av aktoer/fnr for innkommende hovedperson
        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(aktoerHovedperson)) } returns NorskIdent(pinHovedperson)

        every { personService.hentPerson(NorskIdent(pinHovedperson)) } returns hovedPersonMedbarn

        //ektefelle
        every { personService.hentPerson(NorskIdent(pinEktefelledperson)) } returns ektefellePerson
        //barn
        every { personService.hentPerson(NorskIdent(pinBarn1)) } returns barn1

        //every { personService.hentPerson(NorskIdent(pinBarn2)) } returns barn2
        //every { personService.hentPerson(NorskIdent(pinBarn3)) } returns barn3

        //pensjoninformasjon avsl.
        every { restTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java))} returns PrefillTestHelper.readXMLresponse("P2200-AVSL.xml")

        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val apijson = dummyApijson(sakid = "22922563", aktoerId = aktoerHovedperson, sed = "P2200")

        val result = mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        verify (exactly = 1) { personService.hentIdent(IdentType.NorskIdent, AktoerId(aktoerHovedperson)) }
        verify (exactly = 1) { personService.hentPerson(NorskIdent(pinHovedperson)) }
        verify (exactly = 1) { personService.hentPerson(NorskIdent(pinEktefelledperson)) }
        verify (exactly = 1) { personService.hentPerson(NorskIdent(pinBarn1)) }

        val validResponse = """
{
  "sed" : "P2200",
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
          "identifikator" : "20098143379",
          "land" : "NO"
        } ],
        "statsborgerskap" : [ {
          "land" : "QX"
        } ],
        "etternavn" : "TESTER",
        "fornavn" : "HOVED PERSON",
        "kjoenn" : "M",
        "foedested" : {
          "by" : "Unknown",
          "land" : "QX"
        },
        "foedselsdato" : "1981-09-20"
      },
      "adresse" : {
        "gate" : "Oppoverbakken 66",
        "by" : "SØRUMSAND",
        "postnummer" : "1920",
        "land" : "NO"
      }
    },
    "ektefelle" : {
      "person" : {
        "pin" : [ {
          "institusjonsnavn" : "NOINST002, NO INST002, NO",
          "institusjonsid" : "NO:noinst002",
          "identifikator" : "24098543399",
          "land" : "NO"
        } ],
        "statsborgerskap" : [ {
          "land" : "QX"
        } ],
        "etternavn" : "BOUWMANS",
        "fornavn" : "JESSINE TORDNU",
        "kjoenn" : "K",
        "foedested" : {
          "by" : "Unknown",
          "land" : "QX"
        },
        "foedselsdato" : "1985-09-24"
      },
      "type" : "ektefelle"
    },
    "barn" : [ {
      "mor" : {
        "person" : {
          "pin" : [ {
            "institusjonsnavn" : "NOINST002, NO INST002, NO",
            "institusjonsid" : "NO:noinst002",
            "identifikator" : "24098543399",
            "land" : "NO"
          } ],
          "fornavn" : "JESSINE TORDNU"
        }
      },
      "person" : {
        "pin" : [ {
          "institusjonsnavn" : "NOINST002, NO INST002, NO",
          "institusjonsid" : "NO:noinst002",
          "identifikator" : "22090954397",
          "land" : "NO"
        } ],
        "statsborgerskap" : [ {
          "land" : "QX"
        } ],
        "etternavn" : "UNG",
        "fornavn" : "TOPPI DOTTO",
        "kjoenn" : "M",
        "foedested" : {
          "by" : "Unknown",
          "land" : "QX"
        },
        "foedselsdato" : "2009-09-22"
      },
      "far" : {
        "person" : {
          "pin" : [ {
            "institusjonsnavn" : "NOINST002, NO INST002, NO",
            "institusjonsid" : "NO:noinst002",
            "identifikator" : "20098143379",
            "land" : "NO"
          } ],
          "fornavn" : "HOVED PERSON"
        }
      },
      "relasjontilbruker" : "BARN"
    } ],
    "krav" : {
      "dato" : "2020-07-01"
    }
  },
  "pensjon" : {
    "kravDato" : {
      "dato" : "2020-07-01"
    }
  },
  "sedGVer" : "4",
  "sedVer" : "2"
}            
        """.trimIndent()
        JSONAssert.assertEquals(response, validResponse, false)

    }


    @Test
    fun `prefill sed med kravtype førstehangbehandling norge men med vedtak bodsatt utland skal prefylle sed`() {

        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN_2)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_2)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN_2)

        every { restTemplate.exchange(eq("/aktor/$AKTOER_ID"), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns
                PrefillTestHelper.readXMLresponse("P2200-UP-INNV.xml")
        every { restTemplate.exchange(eq("/vedtak/5134513451345"), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns
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
