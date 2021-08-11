package no.nav.eessi.pensjon.integrationtest.sed

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.eux.model.document.P6000Dokument
import no.nav.eessi.pensjon.eux.model.sed.P6000
import no.nav.eessi.pensjon.eux.model.sed.SedType
import no.nav.eessi.pensjon.fagmodul.models.ReferanseTilPerson
import no.nav.eessi.pensjon.fagmodul.prefill.ApiRequest
import no.nav.eessi.pensjon.fagmodul.prefill.ApiSubject
import no.nav.eessi.pensjon.fagmodul.prefill.PersonPDLMock
import no.nav.eessi.pensjon.fagmodul.prefill.SubjectFnr
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonService
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.model.AktoerId
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentType
import no.nav.eessi.pensjon.personoppslag.pdl.model.NorskIdent
import no.nav.eessi.pensjon.security.sts.STSService
import no.nav.eessi.pensjon.services.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.services.pensjonsinformasjon.EPSaktype
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.toJson
import no.nav.eessi.pensjon.utils.typeRefs
import no.nav.pensjon.v1.kravhistorikkliste.V1KravHistorikkListe
import no.nav.pensjon.v1.sak.V1Sak
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestTemplate
import java.time.LocalDate

@SpringBootTest(classes = [UnsecuredWebMvcTestLauncher::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["unsecured-webmvctest"])
@AutoConfigureMockMvc
class SedPrefillP7000Mk2IntegrationSpringTest {

    @MockkBean
    lateinit var stsService: STSService

    @MockkBean(name = "pensjonsinformasjonOidcRestTemplate")
    lateinit var restTemplate: RestTemplate

    @MockkBean
    lateinit var kodeverkClient: KodeverkClient

    @MockkBean
    lateinit var pensjoninformasjonservice: PensjonsinformasjonService

    @MockkBean
    lateinit var personService: PersonService

    @Autowired
    lateinit var mockMvc: MockMvc


    private companion object {
        const val FNR_VOKSEN_3 = "12312312312"
        const val FNR_VOKSEN_4 = "9876543210"

        const val AKTOER_ID = "0123456789000"
        const val AKTOER_ID_2 = "0009876543210"
    }

    @Test
    fun `prefill sed P7000 - Gitt en alderspensjon med flere P6000 med invilgelse og avslag skal det preutfylles gyldig SED`() {
        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN_3)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_3)) } returns PersonPDLMock.createWith(true, "Alder", "Pensjon", FNR_VOKSEN_3, AKTOER_ID)
        every { kodeverkClient.finnLandkode(any())} returns "QX"

        //mock p6000 fra RINA med data som skal benyttes i P7000
        val p6000fraRequest = listOf(mockP6000requestdata("SE"), mockP6000KomplettRequestdata("NO"))
        val payload = mapAnyToJson(p6000fraRequest)

        //mock apiRequest
        val apijson = dummyApiRequest(sakid = "21337890", aktoerId = AKTOER_ID, sed = "P7000", buc = "P_BUC_01", payload = payload ).toJson()

        val validResponse = """ 
        {
          "sed" : "P7000",
          "nav" : {
            "eessisak" : [ {
              "institusjonsid" : "NO:noinst002",
              "institusjonsnavn" : "NOINST002, NO INST002, NO",
              "saksnummer" : "21337890",
              "land" : "NO"
            }, {
              "institusjonsid" : "2342145134",
              "institusjonsnavn" : "NOINST002, NO INST002, NO",
              "saksnummer" : "22874955",
              "land" : "SE"
            } ],
            "bruker" : {
              "person" : {
                "pin" : [ {
                  "identifikator" : "12312312312",
                  "land" : "NO",
                  "institusjon" : { }
                } ],
                "etternavn" : "Pensjon",
                "fornavn" : "Alder",
                "kjoenn" : "M",
                "foedselsdato" : "1988-07-12"
              }
            },
            "ektefelle" : {
              "person" : {
                "etternavn" : "Pensjon"
              }
            }
          },
          "pensjon" : {
            "samletVedtak" : {
              "avslag" : [ {
                "pensjonType" : "01",
                "begrunnelse" : "03",
                "dato" : "2020-12-16",
                "pin" : {
                  "institusjonsnavn" : "NOINST002, NO INST002, NO",
                  "institusjonsid" : "NO:noinst002",
                  "identifikator" : "12312312312",
                  "land" : "SE"
                },
                "adresse" : "Oppoverbakken 66, SØRUMSAND, SE"
              } ],
              "tildeltepensjoner" : [ {
                "pensjonType" : "01",
                "ytelser" : [ {
                  "startdatoretttilytelse" : "2020-10-01",
                  "sluttdatoretttilytelse" : "2030-10-01",
                  "beloep" : [ {
                    "betalingshyppighetytelse" : "03",
                    "valuta" : "HUF",
                    "beloep" : "523"
                  } ]
                }, {
                  "startdatoretttilytelse" : "2020-10-01",
                  "sluttdatoretttilytelse" : "2025-10-01",
                  "beloep" : [ {
                    "betalingshyppighetytelse" : "99",
                    "valuta" : "ISK",
                    "beloep" : "234"
                  } ]
                } ],
                "vedtakPensjonType" : "01",
                "tildeltePensjonerLand" : "NO",
                "adressatForRevurdering" : [ {
                  "adressatforrevurdering" : "null\nasdfsdf \nnull \nsafsd \nasdfsdf \nsadfasdf \nHR "
                } ],
                "institusjon" : {
                  "saksnummer" : "24234sdsd-4",
                  "land" : "NO",
                  "personNr" : "01126712345"
                },
                "reduksjonsGrunn" : "02",
                "startdatoPensjonsRettighet" : "2020-10-01",
                "revurderingtidsfrist" : "2019-10-01"
              } ]
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
        println("*************  response ******************")
        println(response)
        println("**********  response END ******************")
        JSONAssert.assertEquals(response, validResponse, false)
    }



    @Test
    @Throws(Exception::class)
    fun `prefill sed P7000 - Gitt gjenlevendepensjon med flere P6000 med avslag skal det preutfylles gyldig SED`() {
        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID))} returns NorskIdent(FNR_VOKSEN_3)
        every { personService.hentIdent(IdentType.AktoerId, NorskIdent(FNR_VOKSEN_4)) } returns AktoerId(AKTOER_ID_2)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_3)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN_3, AKTOER_ID)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_4)) } returns PersonPDLMock.createWith(true, "Avdød", "Død", FNR_VOKSEN_4, AKTOER_ID_2, true)

        val sak = V1Sak()
        sak.sakType = EPSaktype.GJENLEV.toString()
        sak.sakId = 100
        sak.kravHistorikkListe = V1KravHistorikkListe()

        every { pensjoninformasjonservice.hentRelevantPensjonSak(any(), any()) } returns sak
        every { kodeverkClient.finnLandkode(any()) } returns "QX"


        //mock p6000 fra RINA med data som skal benyttes i P7000
        val p6000fraRequest = listOf(mockP6000requestdata("SE", "03"), mockP6000requestdata("NO", "03"))
        val payload = mapAnyToJson(p6000fraRequest)

        //mock apiRequest
        val subject = dummyApiSubject(FNR_VOKSEN_4)
        val apijson = dummyApiRequest(sakid = "21337890", aktoerId = AKTOER_ID, sed = "P7000", buc = "P_BUC_02", subject = subject, payload = payload ).toJson()

        val validResponse = """
{
  "sed" : "P7000",
  "nav" : {
    "eessisak" : [ {
      "institusjonsid" : "NO:noinst002",
      "institusjonsnavn" : "NOINST002, NO INST002, NO",
      "saksnummer" : "21337890",
      "land" : "NO"
    }, {
      "institusjonsid" : "2342145134",
      "institusjonsnavn" : "NOINST002, NO INST002, NO",
      "saksnummer" : "22874955",
      "land" : "SE"
    } ],
    "bruker" : {
      "person" : {
        "pin" : [ {
          "identifikator" : "9876543210",
          "land" : "NO",
          "institusjon" : { }
        } ],
        "etternavn" : "Død",
        "fornavn" : "Avdød",
        "kjoenn" : "M",
        "foedselsdato" : "1921-07-12"
      }
    },
    "ektefelle" : {
      "person" : {
        "etternavn" : "Død"
      }
    }
  },
  "pensjon" : {
    "gjenlevende" : {
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
        "foedselsdato" : "1988-07-12",
        "rolle" : "01"
      },
      "adresse" : {
        "gate" : "Oppoverbakken 66",
        "by" : "SØRUMSAND",
        "postnummer" : "1920",
        "land" : "NO"
      }
    },
    "samletVedtak" : {
      "avslag" : [ {
        "pensjonType" : "03",
        "begrunnelse" : "03",
        "dato" : "2020-12-16",
        "adresse" : "Oppoverbakken 66, SØRUMSAND, NO"
      }, {
        "pensjonType" : "03",
        "begrunnelse" : "03",
        "dato" : "2020-12-16",
        "pin" : {
          "institusjonsnavn" : "NOINST002, NO INST002, NO",
          "institusjonsid" : "NO:noinst002",
          "identifikator" : "11067122781",
          "land" : "NO"
        },
        "adresse" : "Oppoverbakken 66, SØRUMSAND, NO"
      } ]
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
        JSONAssert.assertEquals(response, validResponse, false)
    }

    @Test
    fun `prefill sed P7000 - Gitt en alderspensjon med flere P6000 med avslag skal det preutfylles gyldig SED`() {
        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN_3)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_3)) } returns PersonPDLMock.createWith(true, "Alder", "Pensjon", FNR_VOKSEN_3, AKTOER_ID)
        every { kodeverkClient.finnLandkode(any())} returns "QX"

        //mock p6000 fra RINA med data som skal benyttes i P7000
        val p6000fraRequest = listOf(mockP6000requestdata("SE"), mockP6000requestdata("NO"))
        val payload = mapAnyToJson(p6000fraRequest)

        //mock apiRequest
        val apijson = dummyApiRequest(sakid = "21337890", aktoerId = AKTOER_ID, sed = "P7000", buc = "P_BUC_01", payload = payload ).toJson()

        val validResponse = """ 
{
  "sed" : "P7000",
  "nav" : {
    "eessisak" : [ {
      "institusjonsid" : "NO:noinst002",
      "institusjonsnavn" : "NOINST002, NO INST002, NO",
      "saksnummer" : "21337890",
      "land" : "NO"
    }, {
      "institusjonsid" : "2342145134",
      "institusjonsnavn" : "NOINST002, NO INST002, NO",
      "saksnummer" : "22874955",
      "land" : "SE"
    } ],
    "bruker" : {
      "person" : {
        "pin" : [ {
          "identifikator" : "12312312312",
          "land" : "NO",
          "institusjon" : { }
        } ],
        "etternavn" : "Pensjon",
        "fornavn" : "Alder",
        "kjoenn" : "M",
        "foedselsdato" : "1988-07-12"
      }
    },
    "ektefelle" : {
      "person" : {
        "etternavn" : "Pensjon"
      }
    }
  },
  "pensjon" : {
    "samletVedtak" : {
      "avslag" : [ {
        "pensjonType" : "01",
        "begrunnelse" : "03",
        "dato" : "2020-12-16",
        "pin" : {
          "institusjonsnavn" : "NOINST002, NO INST002, NO",
          "institusjonsid" : "NO:noinst002",
          "identifikator" : "12312312312",
          "land" : "SE"
        },
        "adresse" : "Oppoverbakken 66, SØRUMSAND, SE"
      }, {
        "pensjonType" : "01",
        "begrunnelse" : "03",
        "dato" : "2020-12-16",
        "pin" : {
          "institusjonsnavn" : "NOINST002, NO INST002, NO",
          "institusjonsid" : "NO:noinst002",
          "identifikator" : "12312312312",
          "land" : "NO"
        },
        "adresse" : "Oppoverbakken 66, SØRUMSAND, NO"
      } ]
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
        JSONAssert.assertEquals(response, validResponse, false)
    }

    @Test
    fun `prefill sed P7000 - Gitt en alderspensjon uten noen P6000 - gyldig SED`() {
        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN_3)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_3)) } returns PersonPDLMock.createWith(true, "Alder", "Pensjon", FNR_VOKSEN_3, AKTOER_ID)
        every { kodeverkClient.finnLandkode(any())} returns "QX"

        //mock apiRequest
        val apijson = dummyApiRequest(sakid = "21337890", aktoerId = AKTOER_ID, sed = "P7000", buc = "P_BUC_01").toJson()
        val validResponse = """ 
            {
              "sed" : "P7000",
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
                      "land" : "NO",
                      "institusjon" : { }
                    } ],
                    "etternavn" : "Pensjon",
                    "fornavn" : "Alder",
                    "kjoenn" : "M",
                    "foedselsdato" : "1988-07-12"
                  }
                },
                "ektefelle" : {
                  "person" : {
                    "etternavn" : "Pensjon"
                  }
                }
              },
              "pensjon" : {
                "bruker" : {
                  "person" : {
                    "kjoenn" : "M"
                  }
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
        JSONAssert.assertEquals(response, validResponse, false)
    }


    private fun dummyApiRequest(sakid: String, vedtakid: String? = "", aktoerId: String, sed: String? = "P2000", buc: String? = "P_BUC_06", subject: ApiSubject? = null, refperson: ReferanseTilPerson? = null, payload: String? = null): ApiRequest {
        return ApiRequest(
            sakId = sakid,
            vedtakId = vedtakid,
            aktoerId = aktoerId,
            payload = payload,
            buc = buc,
            sed = sed,
            documentid =  null,
            euxCaseId = "123123",
            institutions = emptyList(),
            subject = subject,
            subjectArea = "Pensjon",
            referanseTilPerson = refperson
        )
    }

    private fun dummyApiSubject(avdodfnr: String): ApiSubject {
        return ApiSubject(
                null,
                avdod = SubjectFnr(avdodfnr)
        )
    }



    private fun mockP6000KomplettRequestdata(land: String, type: String? = "01") =  Pair(P6000Dokument(SedType.P6000, "123123", "23423asdasd3243423", land, "1", "url", LocalDate.of(2020, 10, 12)), mapJsonToAny(mockKomplettP6000(land, type), typeRefs<P6000>()))

    private fun mockP6000requestdata(land: String, type: String? = "01") =  Pair(P6000Dokument(SedType.P6000, "123123", "23423asdasd3243423", land, "1", "url", LocalDate.of(2021, 11,13)), mapJsonToAny(mockP6000Data(land, type), typeRefs<P6000>()))

    private fun mockP6000Data(land: String = "SE", type: String? = "01"): String {
       return """
            {
              "sed" : "P6000",
              "sedGVer" : "4",
              "sedVer" : "2",
              "nav" : {
                "eessisak" : [ {
                  "institusjonsid" : "2342145134",
                  "institusjonsnavn" : "NOINST002, NO INST002, NO",
                  "saksnummer" : "22874955",
                  "land" : "$land"
                } ],
                "bruker" : {
                  "person" : {
                    "pin" : [ {
                      "institusjonsnavn" : "NOINST002, NO INST002, NO",
                      "institusjonsid" : "NO:noinst002",
                      "identifikator" : "12312312312",
                      "land" : "$land"
                    } ],
                    "statsborgerskap" : [ {
                      "land" : "QX"
                    } ],
                    "etternavn" : "Pensjonist",
                    "fornavn" : "Alder",
                    "kjoenn" : "M",
                    "foedselsdato" : "1988-07-12"
                  },
                  "adresse" : {
                    "gate" : "Oppoverbakken 66",
                    "by" : "SØRUMSAND",
                    "postnummer" : "1920",
                    "land" : "$land"
                  }
                }
              },
              "pensjon" : {
               ${mockP6000gjenlev(type)}
                "vedtak" : [ {
                  "type" : "$type",
                  "resultat" : "02",
                  "avslagbegrunnelse" : [ {
                    "begrunnelse" : "03"
                  } ]
                } ],
                "sak" : {
                  "kravtype" : [ {
                    "datoFrist" : "six weeks from the date the decision is received"
                  } ]
                },
                "tilleggsinformasjon" : {
                  "dato" : "2020-12-16",
                  "andreinstitusjoner" : [ {
                    "institusjonsid" : "32423423",
                    "institusjonsnavn" : "NOINST002, NO INST002, NO",
                    "institusjonsadresse" : "Postboks 6600 Etterstad TEST",
                    "postnummer" : "0607",
                    "land" : "$land",
                    "poststed" : "Oslo"
                  } ]
                }
              }
            }            
        """.trimIndent()
    }

    private fun mockP6000gjenlev(type: String?) : String? {
        if (type != "03") return ""
        return """
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
    },            
        """.trimIndent()
    }

    private fun mockKomplettP6000(land: String = "SE", type: String? = "01") : String {
        return """
{
  "nav": {
    "bruker": {
      "mor": {
        "person": {
          "etternavnvedfoedsel": "asdfsdf",
          "fornavn": "asfsdf"
        }
      },
      "person": {
        "fornavn": "Gul",
        "pin": [
          {
            "sektor": "pensjoner",
            "identifikator": "weqrwerwqe",
            "land": "BG"
          },
          {
            "sektor": "alle",
            "land": "$land",
            "identifikator": "01126712345"
          }
        ],
        "kjoenn": "f",
        "etternavn": "Konsoll",
        "foedselsdato": "1967-12-01",
        "tidligereetternavn": "sdfsfasdf",
        "statsborgerskap": [
          {
            "land": "BE"
          },
          {
            "land": "BG"
          },
          {
            "land": "GR"
          },
          {
            "land": "GB"
          }
        ],
        "foedested": {
          "region": "sfgdfdgs",
          "land": "DK",
          "by": "gafdgsf"
        },
        "fornavnvedfoedsel": "werwerwe",
        "tidligerefornavn": "asdfdsffsd",
        "etternavnvedfoedsel": "werwreq"
      },
      "adresse": {
        "region": "sadfsd",
        "land": "BG",
        "gate": "dsfasdf",
        "bygning": "asdfsfd",
        "postnummer": "safdsdf",
        "by": "sfdsdaf"
      },
      "far": {
        "person": {
          "fornavn": "safasfsd",
          "etternavnvedfoedsel": "sadfsfd"
        }
      }
    },
    "eessisak": [
      {
        "saksnummer": "24234sdsd-4",
        "land": "$land"
      },
      {
        "saksnummer": "retretretert",
        "land": "HR"
      }
    ]
  },
  "pensjon": {
   ${mockP6000gjenlev(type)}
   "vedtak": [
      {
        "grunnlag": {
          "framtidigtrygdetid": "0",
          "medlemskap": "02",
          "opptjening": {
            "forsikredeAnnen": "01"
          }
        },
        "beregning": [
          {
            "beloepBrutto": {
              "ytelseskomponentGrunnpensjon": "2344",
              "beloep": "523",
              "ytelseskomponentTilleggspensjon": "234"
            },
            "periode": {
              "tom": "2030-10-01",
              "fom": "2020-10-01"
            },
            "valuta": "HUF",
            "beloepNetto": {
              "beloep": "344"
            },
            "utbetalingshyppighetAnnen": "13213",
            "utbetalingshyppighet": "maaned_12_per_aar"
          },
          {
            "utbetalingshyppighetAnnen": "werwer",
            "beloepBrutto": {
              "beloep": "234",
              "ytelseskomponentTilleggspensjon": "22",
              "ytelseskomponentGrunnpensjon": "342"
            },
            "periode": {
              "fom": "2020-10-01",
              "tom": "2025-10-01"
            },
            "beloepNetto": {
              "beloep": "12"
            },
            "utbetalingshyppighet": "annet",
            "valuta": "ISK"
          }
        ],
        "basertPaa": "02",
        "delvisstans": {
          "utbetaling": {
            "begrunnelse": "sfdgsdf\nfdg\ns",
            "beloepBrutto": "24234",
            "valuta": "SEK"
          },
          "indikator": "1"
        },
        "virkningsdato": "2020-10-01",
        "artikkel": "02",
        "kjoeringsdato": "2020-12-01",
        "type": "$type",
        "basertPaaAnnen": "sadfsdf",
        "ukjent": {
          "beloepBrutto": {
            "ytelseskomponentAnnen": "sdfsfd\nsdf\nsfd"
          }
        },
        "resultat": "01",
        "avslagbegrunnelse": [
          {
            "begrunnelse": "03",
            "annenbegrunnelse": "fsafasfd\nasd\nfsda"
          },
          {
            "begrunnelse": "02",
            "annenbegrunnelse": "tet\nertert\nretret"
          }
        ],
        "begrunnelseAnnen": "afsdaf\nsdafsfasd\nsadfsd"
      },
      {
        "beregning": [
          {
            "utbetalingshyppighetAnnen": "gagfdgg",
            "valuta": "ERN",
            "beloepBrutto": {
              "ytelseskomponentTilleggspensjon": "12",
              "ytelseskomponentGrunnpensjon": "122",
              "beloep": "234"
            },
            "beloepNetto": {
              "beloep": "23"
            },
            "periode": {
              "tom": "2043-10-01",
              "fom": "2032-10-01"
            },
            "utbetalingshyppighet": "kvartalsvis"
          }
        ],
        "avslagbegrunnelse": [
          {
            "annenbegrunnelse": "324234\n234\n234\n4",
            "begrunnelse": "04"
          },
          {
            "annenbegrunnelse": "sdfafs\nsdfsdf\nfsadfsdf",
            "begrunnelse": "04"
          }
        ],
        "grunnlag": {
          "framtidigtrygdetid": "0",
          "medlemskap": "03",
          "opptjening": {
            "forsikredeAnnen": "03"
          }
        },
        "artikkel": "03",
        "basertPaaAnnen": "wertwertwert",
        "delvisstans": {
          "utbetaling": {
            "begrunnelse": "sdfsdf\nsdfsdf\nsdf\nfsd",
            "beloepBrutto": "234",
            "valuta": "NZD"
          },
          "indikator": "0"
        },
        "type": "03",
        "begrunnelseAnnen": "sdfsdf\nsd\nfsd",
        "resultat": "03",
        "kjoeringsdato": "2022-10-01",
        "ukjent": {
          "beloepBrutto": {
            "ytelseskomponentAnnen": "dsfsdf\ns\ndf\nsdf"
          }
        },
        "virkningsdato": "2030-10-01",
        "basertPaa": "01"
      }
    ],
    "tilleggsinformasjon": {
      "person": {
        "pinannen": {
          "identifikator": "retertret",
          "sektor": "alle"
        }
      },
      "andreinstitusjoner": [
        {
          "institusjonsadresse": "asdfsdf",
          "region": "sadfasdf",
          "postnummer": "asdfsdf",
          "bygningsnr": "sdafsadf",
          "poststed": "safsd",
          "land": "HR"
        }
      ],
      "dato": "2019-10-01",
      "anneninformation": "werwer\nwer\nwer",
      "annen": {
        "institusjonsadresse": {
          "land": "BE"
        }
      },
      "opphoer": {
        "dato": "2022-10-01",
        "annulleringdato": "2024-10-01"
      },
      "saksnummerAnnen": "werwer",
      "saksnummer": "werwer",
      "artikkel48": "0"
    },
    "sak": {
      "artikkel54": "0",
      "kravtype": [
        {
          "datoFrist": "fasfsda"
        }
      ],
      "reduksjon": [
        {
          "artikkeltype": "02"
        },
        {
          "artikkeltype": "03"
        }
      ]
    },
    "reduksjon": [
      {
        "type": "02",
        "virkningsdato": [
          {
            "sluttdato": "2021-09-01",
            "startdato": "2020-12-01"
          },
          {
            "sluttdato": "2022-10-01",
            "startdato": "2034-10-01"
          }
        ],
        "aarsak": {
          "annenytelseellerinntekt": "06",
          "inntektAnnen": "adfasfsd"
        }
      },
      {
        "virkningsdato": [
          {
            "sluttdato": "2034-10-01",
            "startdato": "2033-10-01"
          }
        ],
        "aarsak": {
          "annenytelseellerinntekt": "02",
          "inntektAnnen": "ewrwer"
        },
        "type": "02"
      }
    ]
  },
  "sedVer": "0",
  "sedGVer": "4",
  "sed": "P6000"
}
            
        """.trimIndent()
    }

}

