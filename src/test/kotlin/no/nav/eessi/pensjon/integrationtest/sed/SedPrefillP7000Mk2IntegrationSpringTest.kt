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
        const val FNR_BARN = "12011577847"      // STERK BUSK

        const val AKTOER_ID = "0123456789000"
        const val AKTOER_ID_2 = "0009876543210"
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
        "adresse" : "Adresse(gate=Oppoverbakken 66, bygning=null, by=SØRUMSAND, postnummer=1920, region=null, land=NO, kontaktpersonadresse=null, datoforadresseendring=null, postadresse=null, startdato=null)"
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
        "adresse" : "Adresse(gate=Oppoverbakken 66, bygning=null, by=SØRUMSAND, postnummer=1920, region=null, land=NO, kontaktpersonadresse=null, datoforadresseendring=null, postadresse=null, startdato=null)"
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
        "adresse" : "Adresse(gate=Oppoverbakken 66, bygning=null, by=SØRUMSAND, postnummer=1920, region=null, land=SE, kontaktpersonadresse=null, datoforadresseendring=null, postadresse=null, startdato=null)"
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
        "adresse" : "Adresse(gate=Oppoverbakken 66, bygning=null, by=SØRUMSAND, postnummer=1920, region=null, land=NO, kontaktpersonadresse=null, datoforadresseendring=null, postadresse=null, startdato=null)"
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


    private fun mockP6000requestdata(land: String, type: String? = "01") =  Pair(P6000Dokument(SedType.P6000, "123123", "23423asdasd3243423", land, "1"), mapJsonToAny(mockP6000Data(land, type), typeRefs<P6000>()))

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


}

