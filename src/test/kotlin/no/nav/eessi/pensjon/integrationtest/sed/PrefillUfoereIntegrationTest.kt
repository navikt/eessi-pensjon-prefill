package no.nav.eessi.pensjon.integrationtest.sed

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.eux.model.BucType.P_BUC_06
import no.nav.eessi.pensjon.eux.model.SedType.P2200
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.integrationtest.IntegrasjonsTestConfig
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.kodeverk.Postnummer
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.model.AktoerId
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentGruppe.FOLKEREGISTERIDENT
import no.nav.eessi.pensjon.personoppslag.pdl.model.KjoennType
import no.nav.eessi.pensjon.personoppslag.pdl.model.NorskIdent
import no.nav.eessi.pensjon.personoppslag.pdl.model.Npid
import no.nav.eessi.pensjon.prefill.KrrService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.PersonPDLMock.medBarn
import no.nav.eessi.pensjon.prefill.PersonPDLMock.medFodsel
import no.nav.eessi.pensjon.prefill.PersonPDLMock.medForeldre
import no.nav.eessi.pensjon.prefill.PersonPDLMock.medKjoenn
import no.nav.eessi.pensjon.prefill.PersonPDLMock.medSivilstand
import no.nav.eessi.pensjon.prefill.models.DigitalKontaktinfo
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper
import no.nav.eessi.pensjon.shared.person.Fodselsnummer
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpEntity
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestTemplate

private const val NPID_VOKSEN = "01220049651"
private const val RINA_SAK = "22874955"

@SpringBootTest(classes = [IntegrasjonsTestConfig::class, UnsecuredWebMvcTestLauncher::class, PrefillUfoereIntegrationTest.TestConfig::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("unsecured-webmvctest", "excludeKodeverk")
@AutoConfigureMockMvc
@EmbeddedKafka
class PrefillUfoereIntegrationTest {

    @MockkBean
    private lateinit var pensjonsinformasjonOidcRestTemplate: RestTemplate

    @MockkBean
    private lateinit var personService: PersonService

    @MockkBean
    private lateinit var krrService: KrrService

    @MockkBean
    private lateinit var kodeverkClient: KodeverkClient

    @Autowired
    private lateinit var mockMvc: MockMvc

    private companion object {
        const val FNR_VOKSEN = "11067122781"    // KRAFTIG VEGGPRYD
        const val FNR_VOKSEN_2 = "12312312312"
        const val AKTOER_ID = "0123456789000"
    }

    @TestConfiguration
    internal class TestConfig{
        @Bean
        @Primary
        fun restTemplate(): RestTemplate = mockk()
    }

    @BeforeEach
    fun setUp() {
        every { kodeverkClient.hentPostSted(any()) } returns Postnummer("1068", "SØRUMSAND")
        every { kodeverkClient.finnLandkode(any()) } returns "XQ"
    }

    @Test
    fun `prefill sed P2200 ufoere med AVSL skal returnere valid sedjson`() {

        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()
        every { krrService.hentPersonerFraKrr(any()) } returns DigitalKontaktinfo(epostadresse = "melleby11@melby.no", mobiltelefonnummer = "11111111", aktiv = true, personident = FNR_VOKSEN)
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java))} returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2200-AVSL.xml")
        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val apijson = dummyApijson(sakid = "22922563", aktoerId = AKTOER_ID, sed = P2200.name)
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
    fun `prefill sed P2200 ufoere med Barn og sak er AVSL skal returnere valid sedjson med barn`() {
        val pinHovedperson = FodselsnummerGenerator.generateFnrForTest(40)
        val aktoerHovedperson =  pinHovedperson+10
        val pinEktefelleperson = FodselsnummerGenerator.generateFnrForTest(36)

        val pinBarn1 = FodselsnummerGenerator.generateFnrForTest(12)
        val pinBarn2 = FodselsnummerGenerator.generateFnrForTest(17)
        val pinBarn3 = FodselsnummerGenerator.generateFnrForTest(19)

        val hovedPerson = PersonPDLMock.createWith(landkoder = true, "HOVED PERSON", "TESTER", fnr = pinHovedperson, aktoerid = aktoerHovedperson)
            .medFodsel(Fodselsnummer.fra(pinHovedperson)?.getBirthDate()!!)
            .medKjoenn(KjoennType.MANN)

        val ektefellePerson = PersonPDLMock.createWith(true, "JESSINE TORDNU", "BOUWMANS", fnr =  pinEktefelleperson, aktoerid = pinEktefelleperson+11)
            .medFodsel(Fodselsnummer.fra(pinEktefelleperson)?.getBirthDate()!!,)
            .medKjoenn(KjoennType.KVINNE)
            .medSivilstand(hovedPerson)

        val barn1 = PersonPDLMock.createWith(true, "TOPPI DOTTO", "UNG", fnr = pinBarn1, aktoerid = pinBarn1+12)
            .medForeldre(hovedPerson)
            .medForeldre(ektefellePerson)
            .medFodsel(Fodselsnummer.fra(pinBarn1)?.getBirthDate()!!,)
            .medKjoenn(KjoennType.MANN)

        val barn2 = PersonPDLMock.createWith(true, "EGIDIJS ER", "MED", fnr = pinBarn2, aktoerid = pinBarn2+18)
            .medForeldre(hovedPerson)
            .medForeldre(ektefellePerson)
            .medKjoenn(KjoennType.KVINNE)
            .medFodsel(Fodselsnummer.fra(pinBarn2)?.getBirthDate()!!,)

        val barn3 = PersonPDLMock.createWith(true, "BARN VOKSEN", "GAMMELT", fnr = pinBarn3, aktoerid = pinBarn3+19)
            .medForeldre(hovedPerson)
            .medForeldre(ektefellePerson)
            .medKjoenn(KjoennType.KVINNE)
            .medFodsel(Fodselsnummer.fra(pinBarn3)?.getBirthDate()!!,)

        val hovedPersonMedbarn = hovedPerson
            .medBarn(barn1)
            .medBarn(barn2)
            .medBarn(barn3)
            .medSivilstand(ektefellePerson)


        //mock hent av aktoer/fnr for innkommende hovedperson
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(aktoerHovedperson)) } returns NorskIdent(pinHovedperson)
        every { personService.hentPerson(NorskIdent(pinHovedperson)) } returns hovedPersonMedbarn
        every { krrService.hentPersonerFraKrr(any()) } returns DigitalKontaktinfo(epostadresse = "melleby11@melby.no", reservert = false, mobiltelefonnummer = "11111111", aktiv = true, personident = FNR_VOKSEN)

        //ektefelle
        every { personService.hentPerson(NorskIdent(pinEktefelleperson)) } returns ektefellePerson
        //barn
        every { personService.hentPerson(NorskIdent(pinBarn1)) } returns barn1
        every { personService.hentPerson(NorskIdent(pinBarn2)) } returns barn2
        every { personService.hentPerson(NorskIdent(pinBarn3)) } returns barn3

        //pensjoninformasjon avsl.
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java))} returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2200-AVSL.xml")

        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val apijson = dummyApijson(sakid = "22922563", aktoerId = aktoerHovedperson, sed = P2200.name)

        val result = mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        verify (exactly = 1) { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(aktoerHovedperson)) }
        verify (exactly = 1) { personService.hentPerson(NorskIdent(pinHovedperson)) }
        verify (exactly = 1) { personService.hentPerson(NorskIdent(pinEktefelleperson)) }
        verify (exactly = 1) { personService.hentPerson(NorskIdent(pinBarn1)) }

        val hovedpersonfdato = Fodselsnummer.fra(pinHovedperson)?.getBirthDate()
        val ekktefellefdato = Fodselsnummer.fra(pinEktefelleperson)?.getBirthDate()
        val barn1fdato = Fodselsnummer.fra(pinBarn1)?.getBirthDate()
        val barn2fdato = Fodselsnummer.fra(pinBarn2)?.getBirthDate()
        val barn3fdato = Fodselsnummer.fra(pinBarn3)?.getBirthDate()

        println("barn1 fdato: $barn1fdato, barn1fnr: $pinBarn1")

        val xP2200 = SED.fromJsonToConcrete(response)

        assertEquals(3, xP2200.nav?.barn?.size)

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
                      "identifikator" : "$pinHovedperson",
                      "land" : "NO"
                    }, {
                      "identifikator" : "123123123",
                      "land" : "QX"
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
                    "foedselsdato" : "$hovedpersonfdato",
                    "sivilstand" : [ {
                      "fradato" : "2010-01-10",
                      "status" : "gift"
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
                "ektefelle" : {
                  "person" : {
                    "pin" : [ {
                      "institusjonsnavn" : "NOINST002, NO INST002, NO",
                      "institusjonsid" : "NO:noinst002",
                      "identifikator" : "$pinEktefelleperson",
                      "land" : "NO"
                    }, {
                      "identifikator" : "123123123",
                      "land" : "QX"
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
                    "foedselsdato" : "$ekktefellefdato",
                    "sivilstand" : [ {
                      "fradato" : "2010-01-10",
                      "status" : "gift"
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
                  "type" : "ektefelle"
                },
                "barn" : [ {
                  "mor" : {
                    "person" : {
                      "pin" : [ {
                        "institusjonsnavn" : "NOINST002, NO INST002, NO",
                        "institusjonsid" : "NO:noinst002",
                        "identifikator" : "$pinEktefelleperson",
                        "land" : "NO"
                      } ],
                      "fornavn" : "JESSINE TORDNU"
                    }
                  },
                  "person" : {
                    "pin" : [ {
                      "institusjonsnavn" : "NOINST002, NO INST002, NO",
                      "institusjonsid" : "NO:noinst002",
                      "identifikator" : "$pinBarn1",
                      "land" : "NO"
                    }, {
                      "identifikator" : "123123123",
                      "land" : "QX"
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
                    "foedselsdato" : "$barn1fdato",
                    "sivilstand" : [ {
                      "fradato" : "2000-10-01",
                      "status" : "enslig"
                    } ]
                  },
                  "far" : {
                    "person" : {
                      "pin" : [ {
                        "institusjonsnavn" : "NOINST002, NO INST002, NO",
                        "institusjonsid" : "NO:noinst002",
                        "identifikator" : "$pinHovedperson",
                        "land" : "NO"
                      } ],
                      "fornavn" : "HOVED PERSON"
                    }
                  },
                  "relasjontilbruker" : "BARN",
                  "relasjontilbruker43" : "BARN"                     
                }, {
                  "mor" : {
                    "person" : {
                      "pin" : [ {
                        "institusjonsnavn" : "NOINST002, NO INST002, NO",
                        "institusjonsid" : "NO:noinst002",
                        "identifikator" : "$pinEktefelleperson",
                        "land" : "NO"
                      } ],
                      "fornavn" : "JESSINE TORDNU"
                    }
                  },
                  "person" : {
                    "pin" : [ {
                      "institusjonsnavn" : "NOINST002, NO INST002, NO",
                      "institusjonsid" : "NO:noinst002",
                      "identifikator" : "$pinBarn2",
                      "land" : "NO"
                    }, {
                      "identifikator" : "123123123",
                      "land" : "QX"
                    } ],
                    "statsborgerskap" : [ {
                      "land" : "QX"
                    } ],
                    "etternavn" : "MED",
                    "fornavn" : "EGIDIJS ER",
                    "kjoenn" : "K",
                    "foedested" : {
                      "by" : "Unknown",
                      "land" : "QX"
                    },
                    "foedselsdato" : "$barn2fdato",
                    "sivilstand" : [ {
                      "fradato" : "2000-10-01",
                      "status" : "enslig"
                    } ]
                  },
                  "far" : {
                    "person" : {
                      "pin" : [ {
                        "institusjonsnavn" : "NOINST002, NO INST002, NO",
                        "institusjonsid" : "NO:noinst002",
                        "identifikator" : "$pinHovedperson",
                        "land" : "NO"
                      } ],
                      "fornavn" : "HOVED PERSON"
                    }
                  },
                  "relasjontilbruker" : "BARN",
                  "relasjontilbruker43" : "BARN"                     
                }, {
                  "mor" : {
                    "person" : {
                      "pin" : [ {
                        "institusjonsnavn" : "NOINST002, NO INST002, NO",
                        "institusjonsid" : "NO:noinst002",
                        "identifikator" : "$pinEktefelleperson",
                        "land" : "NO"
                      } ],
                      "fornavn" : "JESSINE TORDNU"
                    }
                  },
                  "person" : {
                    "pin" : [ {
                      "institusjonsnavn" : "NOINST002, NO INST002, NO",
                      "institusjonsid" : "NO:noinst002",
                      "identifikator" : "$pinBarn3",
                      "land" : "NO"
                    }, {
                      "identifikator" : "123123123",
                      "land" : "QX"
                    } ],
                    "statsborgerskap" : [ {
                      "land" : "QX"
                    } ],
                    "etternavn" : "GAMMELT",
                    "fornavn" : "BARN VOKSEN",
                    "kjoenn" : "K",
                    "foedested" : {
                      "by" : "Unknown",
                      "land" : "QX"
                    },
                    "foedselsdato" : "$barn3fdato",
                    "sivilstand" : [ {
                      "fradato" : "2000-10-01",
                      "status" : "enslig"
                    } ]
                  },
                  "far" : {
                    "person" : {
                      "pin" : [ {
                        "institusjonsnavn" : "NOINST002, NO INST002, NO",
                        "institusjonsid" : "NO:noinst002",
                        "identifikator" : "$pinHovedperson",
                        "land" : "NO"
                      } ],
                      "fornavn" : "HOVED PERSON"
                    }
                  },
                  "relasjontilbruker" : "BARN",
                  "relasjontilbruker43" : "BARN"                  
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

        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN_2)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_2)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN_2)
        every { krrService.hentPersonerFraKrr(any()) } returns DigitalKontaktinfo(epostadresse = "melleby11@melby.no", mobiltelefonnummer = "11111111", aktiv = true, personident = FNR_VOKSEN)

        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java))} returns
                PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2200-UP-INNV.xml")

        every { pensjonsinformasjonOidcRestTemplate.exchange(eq("/vedtak/5134513451345"), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns
                PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/vedtak/P6000-APUtland-301.xml")

        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val apijson = dummyApijson(sakid = RINA_SAK, aktoerId = AKTOER_ID, vedtakid = "5134513451345", sed = P2200.name)

        val result = mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val validResponse = validResponse(FNR_VOKSEN_2)
        JSONAssert.assertEquals(response, validResponse, true)
    }

    @Test
    fun `Gitt en SED med bruker som har NPID, kravtype førstegangsbehandling Norge og vedtak bosatt utland så skal SEDen preutfylles`() {

        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns Npid(NPID_VOKSEN)
        every { personService.hentPerson(Npid(NPID_VOKSEN)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", NPID_VOKSEN)
        every { krrService.hentPersonerFraKrr(any()) } returns DigitalKontaktinfo(epostadresse = "melleby11@melby.no", mobiltelefonnummer = "11111111", aktiv = true, personident = FNR_VOKSEN)

        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java))} returns
                PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2200-UP-INNV.xml")

        every { pensjonsinformasjonOidcRestTemplate.exchange(eq("/vedtak/5134513451345"), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns
                PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/vedtak/P6000-APUtland-301.xml")

        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val apijson = dummyApijson(sakid = RINA_SAK, aktoerId = AKTOER_ID, vedtakid = "5134513451345", sed = P2200.name)

        val result = mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val validResponse = validResponse(NPID_VOKSEN)
        JSONAssert.assertEquals(validResponse, response, true)
    }

    private fun validResponse(ident: String) = """
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
                          "identifikator" : "$ident",
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


    private fun dummyApijson(sakid: String, vedtakid: String? = "", aktoerId: String, sed: String? = P2200.name, buc: String? = P_BUC_06.name, subject: String? = null, refperson: String? = null): String {
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
