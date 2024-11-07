package no.nav.eessi.pensjon.integrationtest.sed

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.eux.model.BucType.*
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.integrationtest.IntegrasjonsTestConfig
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.pensjonsinformasjon.models.EPSaktype
import no.nav.eessi.pensjon.pensjonsinformasjon.models.KravArsak
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.model.*
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentGruppe.*
import no.nav.eessi.pensjon.prefill.KrrService
import no.nav.eessi.pensjon.prefill.PensjonsinformasjonService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.PersonPDLMock.medBeskyttelse
import no.nav.eessi.pensjon.prefill.PersonPDLMock.medUtlandAdresse
import no.nav.eessi.pensjon.prefill.models.KrrPerson
import no.nav.pensjon.v1.kravhistorikk.V1KravHistorikk
import no.nav.pensjon.v1.kravhistorikkliste.V1KravHistorikkListe
import no.nav.pensjon.v1.sak.V1Sak
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestTemplate

@SpringBootTest(classes = [IntegrasjonsTestConfig::class, UnsecuredWebMvcTestLauncher::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("unsecured-webmvctest", "excludeKodeverk")
@AutoConfigureMockMvc
@EmbeddedKafka
class SedPrefillP8000IntegrationSpringTest {

    @MockkBean
    lateinit var pdlRestTemplate: RestTemplate

    @MockkBean
    private lateinit var kodeverkClient: KodeverkClient

    @MockkBean
    private lateinit var pensjoninformasjonservice: PensjonsinformasjonService

    @MockkBean
    private lateinit var personService: PersonService

    @MockkBean
    private lateinit var krrService: KrrService

    @Autowired
    private lateinit var mockMvc: MockMvc

    private companion object {
        const val FNR_VOKSEN_3 = "12312312312"
        const val FNR_VOKSEN_4 = "9876543210"
        const val FNR_BARN = "12011577847"      // STERK BUSK

        const val AKTOER_ID = "0123456789000"
        const val AKTOER_ID_2 = "0009876543210"
    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P8000 - Gitt gjenlevendepensjon Og henvendelse gjelder søker SÅ skal det produseres en Gyldig P8000 med referanse til person 02`() {
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID))} returns NorskIdent(FNR_VOKSEN_3)
        every { personService.hentIdent(AKTORID, NorskIdent(FNR_VOKSEN_4)) } returns AktoerId(AKTOER_ID_2)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_3)) } returns PersonPDLMock
            .createWith(true, "Lever", "Gjenlev", FNR_VOKSEN_3, AKTOER_ID)
            .medUtlandAdresse(
                gateOgnr = "OlssenGate",
                postnummer = "9898",
                landkode = "SE",
                bygning = "bygning",
                region = "Akershus",
                bySted = "UTLANDBY"
            )
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_4)) } returns PersonPDLMock.createWith(true, "Avdød", "Død", FNR_VOKSEN_4, AKTOER_ID_2, true)
        every { krrService.hentPersonFraKrr(any()) } returns KrrPerson(false,"melleby11@melby.no", "11111111")

        val sak = V1Sak().apply {
            sakType = EPSaktype.GJENLEV.toString()
            sakId  = 100
            kravHistorikkListe = V1KravHistorikkListe()
        }

        every { pensjoninformasjonservice.hentRelevantPensjonSak(any(), any()) } returns sak
        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val subject = dummyApiSubjectjson(FNR_VOKSEN_4)
        val apijson = dummyApijson(sakid = "21337890", aktoerId = AKTOER_ID, sed = "P8000", buc = P_BUC_05.name, subject = subject, refperson = "\"SOKER\"")

        val validResponse = """
          {
          "sed" : "P8000",
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
                  "identifikator" : "9876543210",
                  "land" : "NO"
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
            },
            "annenperson" : {
              "person" : {
                "pin" : [ {
                  "institusjonsnavn" : "NOINST002, NO INST002, NO",
                  "institusjonsid" : "NO:noinst002",
                  "identifikator" : "12312312312",
                  "land" : "NO"
                }, {
                  "institusjonsnavn" : "NOINST002, NO INST002, NO",
                  "institusjonsid" : "NO:noinst002",
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
                "rolle" : "01",
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
                "gate" : "OlssenGate",
                "bygning" : "bygning",
                "by" : "UTLANDBY",
                "postnummer" : "9898",
                "region" : "Akershus",
                "land" : "QX"
              }
            }
          },
          "pensjon" : {
            "anmodning" : {
              "referanseTilPerson" : "02"
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

    @Test
    @Throws(Exception::class)
    fun `prefill sed P8000 - Gitt alderpensjon Og henvendelse gjelder avdød SÅ skal det produseres en Gyldig P8000 med avdød og gjenlevende`() {

        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN_3)
        every { personService.hentIdent(AKTORID, NorskIdent(FNR_VOKSEN_4)) } returns AktoerId(AKTOER_ID_2)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_3)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN_3, AKTOER_ID)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_4)) } returns PersonPDLMock.createWith(true, "Avdød", "Død", FNR_VOKSEN_4, AKTOER_ID_2, true)

        every { krrService.hentPersonFraKrr(eq(FNR_VOKSEN_3)) } returns KrrPerson(false,"melleby11@melby.no", "11111111")
        every { krrService.hentPersonFraKrr(eq(FNR_VOKSEN_4)) } returns KrrPerson(false,"melleby12@melby.no", "22222222")


        val sak = V1Sak().apply {
            sakType = EPSaktype.ALDER.toString()
            sakId  = 100
            kravHistorikkListe = V1KravHistorikkListe()
            kravHistorikkListe.kravHistorikkListe.add(V1KravHistorikk().apply { kravArsak = KravArsak.GJNL_SKAL_VURD.name })
        }

        every {pensjoninformasjonservice.hentRelevantPensjonSak(any(), any()) } returns sak
        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val subject = dummyApiSubjectjson(FNR_VOKSEN_4)
        val apijson = dummyApijson(sakid = "21337890", aktoerId = AKTOER_ID, sed = "P8000", buc = P_BUC_05.name, subject = subject, refperson = "\"AVDOD\"")

        val validResponse = """
        {
          "sed" : "P8000",
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
                  "identifikator" : "9876543210",
                  "land" : "NO"
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
            },
            "annenperson" : {
              "person" : {
                "pin" : [ {
                  "institusjonsnavn" : "NOINST002, NO INST002, NO",
                  "institusjonsid" : "NO:noinst002",
                  "identifikator" : "12312312312",
                  "land" : "NO"
                }, {
                  "institusjonsnavn" : "NOINST002, NO INST002, NO",
                  "institusjonsid" : "NO:noinst002",
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
                "rolle" : "01",
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
            }
          },
          "pensjon" : {
            "anmodning" : {
              "referanseTilPerson" : "01"
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


    @Test
    @Throws(Exception::class)
    fun `prefill sed P8000 - Gitt alderpensjon Og henvendelse gjelder søker SÅ skal det produseres en Gyldig P8000 med referanse der søker er gjenlevende`() {
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN_3)
        every { personService.hentIdent(AKTORID, NorskIdent(FNR_VOKSEN_4)) } returns AktoerId(AKTOER_ID_2)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_3))  } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN_3, AKTOER_ID)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_4)) } returns PersonPDLMock.createWith(true, "Avdød", "Død", FNR_VOKSEN_4, AKTOER_ID_2, true)

        every { krrService.hentPersonFraKrr(eq(FNR_VOKSEN_3))  } returns KrrPerson(false,"melleby11@melby.no", "11111111")
        every { krrService.hentPersonFraKrr(eq(FNR_VOKSEN_4))  } returns KrrPerson(false,"melleby12@melby.no", "22222222")

        val v1Kravhistorikk = V1KravHistorikk()
        v1Kravhistorikk.kravArsak = KravArsak.GJNL_SKAL_VURD.name

        val sak = V1Sak().apply {
            sakType = EPSaktype.ALDER.toString()
            sakId  = 21337890
            kravHistorikkListe = V1KravHistorikkListe()
            kravHistorikkListe.kravHistorikkListe.add(V1KravHistorikk().apply { kravArsak = KravArsak.GJNL_SKAL_VURD.name })
        }

        every { pensjoninformasjonservice.hentRelevantPensjonSak(any(), any()) } returns sak
        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val subject = dummyApiSubjectjson(FNR_VOKSEN_4)
        val apijson = dummyApijson(sakid = "21337890", aktoerId = AKTOER_ID, sed = "P8000", buc = P_BUC_05.name, subject = subject, refperson = "\"SOKER\"")

        val validResponse = """
            {
              "sed" : "P8000",
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
                      "identifikator" : "12312312312",
                      "land" : "NO"
                    } ],
                    "etternavn" : "Gjenlev",
                    "fornavn" : "Lever",
                    "kjoenn" : "M",
                    "foedselsdato" : "1988-07-12"
                  },
                  "adresse" : {
                    "gate" : "Oppoverbakken 66",
                    "by" : "SØRUMSAND",
                    "bygning" : "bygning",
                    "postnummer" : "1920",
                    "region" : "region",
                    "land" : "NO"
                  }
                }
              },
              "pensjon" : {
                "anmodning" : {
                  "referanseTilPerson" : "02"
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

    @Test
    @Throws(Exception::class)
    fun `prefill sed P8000 - Gitt barnepensjon og henvendelse gjelder søker som er barn med kode 6 SÅ skal det produseres en Gyldig P8000 med referanse der søker er gjenlevende og adresse ikke blir preutfylt `() {

        val sak = V1Sak().apply {
            sakType = EPSaktype.BARNEP.toString()
            sakId  = 100
            kravHistorikkListe = V1KravHistorikkListe()
            kravHistorikkListe.kravHistorikkListe.add(V1KravHistorikk().apply { kravArsak = KravArsak.GJNL_SKAL_VURD.name })
        }

        every { pensjoninformasjonservice.hentRelevantPensjonSak(any(), any()) } returns sak
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_BARN)
        every { personService.hentIdent(AKTORID, NorskIdent(FNR_VOKSEN_4)) } returns AktoerId(AKTOER_ID_2)
        every { krrService.hentPersonFraKrr(any()) } returns KrrPerson(false,"melleby11@melby.no", "11111111")

        val diskeBarn = PersonPDLMock.createWith(true, "Barn", "Diskret", FNR_BARN, AKTOER_ID)
                            .medBeskyttelse(AdressebeskyttelseGradering.STRENGT_FORTROLIG)
        every { personService.hentPerson(NorskIdent(FNR_BARN)) } returns diskeBarn
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_4)) } returns PersonPDLMock.createWith(true, "Avdød", "Død", FNR_VOKSEN_4, AKTOER_ID_2, true)
        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val subject = dummyApiSubjectjson(FNR_VOKSEN_4)
        val apijson = dummyApijson(sakid = "21337890", aktoerId = AKTOER_ID, sed = "P8000", buc = P_BUC_05.name, subject = subject, refperson = "\"SOKER\"")

        val result = mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val validResponse = """
        {
          "sed" : "P8000",
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
                  "identifikator" : "9876543210",
                  "land" : "NO"
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
            },
            "annenperson" : {
              "person" : {
                "pin" : [ {
                  "institusjonsnavn" : "NOINST002, NO INST002, NO",
                  "institusjonsid" : "NO:noinst002",
                  "identifikator" : "12011577847",
                  "land" : "NO"
                }, {
                  "institusjonsnavn" : "NOINST002, NO INST002, NO",
                  "institusjonsid" : "NO:noinst002",
                  "identifikator" : "123123123",
                  "land" : "QX"
                } ],
                "statsborgerskap" : [ {
                  "land" : "QX"
                } ],
                "etternavn" : "Diskret",
                "fornavn" : "Barn",
                "kjoenn" : "M",
                "foedselsdato" : "1988-07-12",
                "rolle" : "01",
                "kontakt" : {
                  "telefon" : [ {
                    "type" : "mobil",
                    "nummer" : "11111111"
                  } ],
                  "email" : [ {
                    "adresse" : "melleby11@melby.no"
                  } ]
                }
              }
            }
          },
          "pensjon" : {
            "anmodning" : {
              "referanseTilPerson" : "02"
            }
          },
          "sedGVer" : "4",
          "sedVer" : "2"
        }
        """.trimIndent()

        JSONAssert.assertEquals(response, validResponse, false)

    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P8000 - Gitt en alderspensjon så skal det genereres en P8000 uten referanse til person`() {
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN_3)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_3)) } returns PersonPDLMock.createWith(true, "Alder", "Pensjon", FNR_VOKSEN_3, AKTOER_ID)
        every { krrService.hentPersonFraKrr(any()) } returns KrrPerson(false,"melleby11@melby.no", "11111111")
        every { kodeverkClient.finnLandkode(any())} returns "QX"

        val apijson = dummyApijson(sakid = "21337890", aktoerId = AKTOER_ID, sed = "P8000", buc = P_BUC_05.name)

        val validResponse = """
            {
              "sed" : "P8000",
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
                      "identifikator" : "12312312312",
                      "land" : "NO"
                    } ],
                    "etternavn" : "Pensjon",
                    "fornavn" : "Alder",
                    "kjoenn" : "M",
                    "foedselsdato" : "1988-07-12"
                  },
                  "adresse" : {
                    "gate" : "Oppoverbakken 66",
                    "by" : "SØRUMSAND",
                    "bygning" : "bygning",
                    "postnummer" : "1920",
                    "region" : "region",
                    "land" : "NO"
                  }
                }
              },
              "pensjon" : { }
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


    @Test
    fun `prefill sed P8000 - Med Bruk av Syntetisk fnr Gitt en alderspensjon så skal det genereres en P8000 uten referanse til person`() {
        val syntFnr = "54496214261"

        every { pensjoninformasjonservice.hentRelevantPensjonSak(any(), any()) } returns V1Sak()
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(syntFnr)
        every { personService.hentPerson(NorskIdent(syntFnr)) } returns PersonPDLMock.createWith(true, "Alder", "Pensjon", syntFnr, AKTOER_ID)
        every { krrService.hentPersonFraKrr(any()) } returns KrrPerson(false,"melleby11@melby.no", "11111111")
        every { kodeverkClient.finnLandkode(any())} returns "QX"

        val apijson = dummyApijson(sakid = "21337890", aktoerId = AKTOER_ID, sed = "P8000", buc = P_BUC_05.name)

        val validResponse = """
            {
              "sed" : "P8000",
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
                      "identifikator" : "$syntFnr",
                      "land" : "NO"
                    } ],
                    "etternavn" : "Pensjon",
                    "fornavn" : "Alder",
                    "kjoenn" : "M",
                    "foedselsdato" : "1988-07-12"
                  },
                  "adresse" : {
                    "gate" : "Oppoverbakken 66",
                    "by" : "SØRUMSAND",
                    "bygning" : "bygning",
                    "postnummer" : "1920",
                    "region" : "region",
                    "land" : "NO"
                  }
                }
              },
              "pensjon" : { }
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


    private fun dummyApijson(sakid: String, vedtakid: String? = "", aktoerId: String, sed: String? = SedType.P2000.name, buc: String? = P_BUC_06.name, subject: String? = null, refperson: String? = null): String {
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

    private fun dummyApiSubjectjson(avdodfnr: String): String {
        return """
            { 
                "gjenlevende" : null, 
                "avdod" : { "fnr": "$avdodfnr"}
            }              
        """.trimIndent()
    }
}

