package no.nav.eessi.pensjon.integrationtest.sed

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.document.P6000Dokument
import no.nav.eessi.pensjon.eux.model.document.Retning
import no.nav.eessi.pensjon.eux.model.sed.P6000
import no.nav.eessi.pensjon.eux.model.sed.P7000
import no.nav.eessi.pensjon.integrationtest.IntegrasjonsTestConfig
import no.nav.eessi.pensjon.pensjonsinformasjon.EPSaktype
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.model.AktoerId
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentType
import no.nav.eessi.pensjon.personoppslag.pdl.model.NorskIdent
import no.nav.eessi.pensjon.prefill.ApiRequest
import no.nav.eessi.pensjon.prefill.ApiSubject
import no.nav.eessi.pensjon.prefill.PensjonsinformasjonService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.SubjectFnr
import no.nav.eessi.pensjon.prefill.models.ReferanseTilPerson
import no.nav.eessi.pensjon.services.kodeverk.KodeverkClient
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
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.util.ResourceUtils
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals

@SpringBootTest(classes = [IntegrasjonsTestConfig::class, UnsecuredWebMvcTestLauncher::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["unsecured-webmvctest"])
@AutoConfigureMockMvc
@EmbeddedKafka
class SedPrefillP7000Mk2IntegrationSpringTest {

    @MockkBean
    private lateinit var kodeverkClient: KodeverkClient

    @MockkBean
    private lateinit var pensjoninformasjonservice: PensjonsinformasjonService

    @MockkBean
    private lateinit var personService: PersonService

    @Autowired
    private lateinit var mockMvc: MockMvc

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
        val p6000fraRequest = listOf(mockP6000requestdata("SE", "P6000SE-INNV.json"), mockP6000KomplettRequestdata("NO"))
        val payload = mapAnyToJson(p6000fraRequest)

        //mock apiRequest
        val apijson = dummyApiRequest(sakid = "21337890", aktoerId = AKTOER_ID, sed = "P7000", buc = "P_BUC_01", payload = payload ).toJson()

        val result = mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val p7000Actual = mapJsonToAny<P7000>(response, typeRefs())
        val p7000Person = p7000Actual.nav?.bruker?.person!!

        //eessisak
        val eessisak1 = p7000Actual.nav?.eessisak?.get(0)
        val eessisak2 = p7000Actual.nav?.eessisak?.get(1)

        //1.1.[1].1. Land *
        assertEquals("NO", eessisak1?.land)
        assertEquals("SE", eessisak2?.land)
        //1.1.[1].2. Saksnummer *
        assertEquals("21337890", eessisak1?.saksnummer)
        assertEquals("22874955", eessisak2?.saksnummer)
        //1.1.[1].3.1. Institusjonens ID *
        assertEquals("NO:noinst002", eessisak1?.institusjonsid)
        assertEquals("2342145134", eessisak2?.institusjonsid)
        //1.1.[1].3.2. Institusjonens navn *
        assertEquals("NOINST002, NO INST002, NO", eessisak1?.institusjonsnavn)
        assertEquals("NOINST002, NO INST002, NO", eessisak2?.institusjonsnavn)
        //2.1.7.1.[1].2.Personnummer (PIN)*
        assertEquals("12312312312", p7000Person.pin?.firstOrNull()?.identifikator)
        assertEquals("NO", p7000Person.pin?.firstOrNull()?.land)

        //pensjon
        val p700SamletVedtak = p7000Actual.p7000Pensjon?.samletVedtak
        val tildeltepensjoner = p700SamletVedtak?.tildeltepensjoner?.firstOrNull()!!
        val avslag = p700SamletVedtak.avslag?.firstOrNull()
        val belop = tildeltepensjoner.ytelser?.firstOrNull()?.beloep?.firstOrNull()!!

        //4.1.[1].1.Type pensjon
        assertEquals("01", tildeltepensjoner.pensjonType)
        //4.1.[1].2.1.1.Land*
        assertEquals("NO", eessisak1?.land)
        assertEquals("SE", eessisak2?.land)
        //4.1.[1].2.1.2.Personnummer
        assertEquals("12312312312", p7000Person.pin?.firstOrNull()?.identifikator)
        //4.1.[1].2.2.Saksnummer
        assertEquals("21337890", eessisak1?.saksnummer)
        assertEquals("22874955", eessisak2?.saksnummer)
        //4.1.[1].3.Innvilget pensjon
        assertEquals("04", tildeltepensjoner.innvilgetPensjon)
        //4.1.[1].4.Vedtakets utstedelsesdato
        assertEquals("2019-10-01", tildeltepensjoner.vedtaksDato)
        //4.1.[1].5.Startdato for pensjonsrettighet
        assertEquals("2020-10-01", tildeltepensjoner.startdatoPensjonsRettighet)
        //4.1.[1].6.[1].1.Utbetales fra
        assertEquals("2020-10-01", tildeltepensjoner.ytelser?.firstOrNull()?.startdatoretttilytelse )
        //4.1.[1].6.[1].1.Utbetales til
        assertEquals("2030-10-01", tildeltepensjoner.ytelser?.firstOrNull()?.sluttdatoUtbetaling )
        //4.1.[1].6.[1].3.Bruttopensjon
        assertEquals("523", belop.beloepBrutto)
        //4.1.[1].6.[1].4.Valuta
        assertEquals("HUF", belop.valuta)
        //4.1.[1].6.[1].5.Betalingshyppighet
        assertEquals("Annet", belop.betalingshyppighetytelse)
        //4.1.[1].7.Pensjonen er redusertgrunnet
        assertEquals("03", tildeltepensjoner.reduksjonsGrunn)
        //4.1.[1].8.1.Tidsfrister for krav om revurdering
        assertEquals("2026-01-23", tildeltepensjoner.revurderingtidsfrist)
        //4.1.[1].8.2.[1].1.Adressat for revurderingen
        assertEquals("Olesgate 15\nOslo\n0130\nNO", tildeltepensjoner.adressatForRevurdering?.firstOrNull()?.adressatforrevurdering)
        //5.1.[1].1.Type pensjon
        assertEquals("01", tildeltepensjoner.pensjonType)
        //5.1.[1].2.1.1.Land*
        assertEquals("NO", tildeltepensjoner.institusjon?.land)
        //5.1.[1].2.2.Saksnummer
        assertEquals("24234sdsd-4", tildeltepensjoner.institusjon?.saksnummer)
        //5.1.[1].3.Vedtakets utstedelsesdato (angitt på vedtaket)*
        assertEquals("2019-10-01",  tildeltepensjoner.vedtaksDato)
        //5.1.[1].4.Avslagsgrunner
        assertEquals("03", p700SamletVedtak.avslag?.get(0)?.begrunnelse)
        //5.1.[1].5.1.Tidsfrister for krav om revurdering
        assertEquals("seven weeks from the date the decision is received", avslag?.tidsfristForRevurdering)
        //5.1.[1].5.2.[1].1. Adressat for revurderingen
        assertEquals("Olesgate 15\nOslo\n0130\nNO", avslag?.adressatforRevurderingAvslag?.firstOrNull()?.adressatforrevurdering)
        //5.1.[1].5.2.[1].1.Adressat for revurderingen
        assertEquals("Olesgate 15\nOslo\n0130\nNO", tildeltepensjoner.adressatForRevurdering?.firstOrNull()?.adressatforrevurdering)
        //6.1.Dato
        assertEquals("2019-10-01",  tildeltepensjoner.vedtaksDato)
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
        val p6000fraRequest = listOf(mockP6000requestdata("SE","P6000SE-INNV.json"), mockP6000requestdata("NO", "P6000SE-INNV.json"))
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
                      "institusjonsnavn" : "NOINST002, NO INST002, NO",
                      "institusjonsid" : "NO:noinst002",
                      "identifikator" : "9876543210",
                      "land" : "NO"
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
                "pensjonType" : "01",
                "begrunnelse" : "03",
                "dato" : "2019-10-01",
                "tidsfristForRevurdering" : "seven weeks from the date the decision is received",
                "adressatforRevurderingAvslag" : [ {
                  "adressatforrevurdering" : "Olesgate 15\nOslo\n0130\nNO"
                } ]
              }, {
                "pensjonType" : "01",
                "begrunnelse" : "03",
                "dato" : "2019-10-01",
                "tidsfristForRevurdering" : "seven weeks from the date the decision is received",
                "pin" : {
                  "institusjonsnavn" : "NOINST002, NO INST002, NO",
                  "institusjonsid" : "NO:noinst002",
                  "identifikator" : "11067122781",
                  "land" : "NO"
                },
                "adressatforRevurderingAvslag" : [ {
                  "adressatforrevurdering" : "Olesgate 15\nOslo\n0130\nNO"
                } ]
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
    fun `prefill sed P7000 - Gitt gjenlevendepensjon med to P6000 med godkjent pensjon skal det preutfylles gyldig SED`() {
        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID))} returns NorskIdent(FNR_VOKSEN_3)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_3)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN_3, AKTOER_ID)

        val sak = V1Sak()
        sak.sakType = EPSaktype.ALDER.toString()
        sak.sakId = 100
        sak.kravHistorikkListe = V1KravHistorikkListe()

        every { pensjoninformasjonservice.hentRelevantPensjonSak(any(), any()) } returns sak
        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        //mock p6000 fra RINA med data som skal benyttes i P7000

        val p6000fraRequest = listOf(mockP6000requestdata("NO", "P7000/P6000Sendt.json"), mockP6000requestdata("SE", "P7000/P6000Mottatt.json"))
        val payload = mapAnyToJson(p6000fraRequest)

        //mock apiRequest
        val apijson = dummyApiRequest(sakid = "21337890", aktoerId = AKTOER_ID, sed = "P7000", buc = "P_BUC_01", subject = null, payload = payload ).toJson()

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
              "institusjonsid" : "SE:NAVAT07",
              "institusjonsnavn" : "NAV ACCEPTANCE TEST JYZ",
              "saksnummer" : "134513452",
              "land" : "SE"
            } ],
            "bruker" : {
              "person" : {
                    "pin" : [ {
                          "institusjonsnavn" : "SE ASCCEPTANCE TEST JAP",
                          "institusjonsid" : "SE:NAVAT07",
                          "identifikator" : "345315327578",
                          "land" : "SE"
                        }, {
                          "institusjonsnavn" : "NOINST002, NO INST002, NO",
                          "institusjonsid" : "NO:noinst002",
                          "identifikator" : "12312312312",
                          "land" : "NO"
                        } ],
                "etternavn" : "Gjenlev",
                "fornavn" : "Lever",
                "kjoenn" : "M",
                "foedselsdato" : "1988-07-12"
              }
            },
            "ektefelle" : {
              "person" : {
                "etternavn" : "Gjenlev"
              }
            }
          },
          "pensjon" : {
            "samletVedtak" : {
              "tildeltepensjoner" : [ {
                "ytelser" : [ {
                  "startdatoretttilytelse" : "2021-09-01",
                  "beloep" : [ {
                    "betalingshyppighetytelse" : "03",
                    "valuta" : "NOK",
                    "beloepBrutto" : "4441"
                  } ]
                } ],
                "pensjonType" : "01",
                "tildeltePensjonerLand" : "NO",
                "adressatForRevurdering" : [ {
                  "adressatforrevurdering" : "NAV ACCEPTANCE TEST 07\nPostboks 6600 Etterstad\nOslo\n0607\nNO"
                } ],
                "institusjon" : {
                  "institusjonsid" : "NO:NAVAT07",
                  "institusjonsnavn" : "NAV ACCEPTANCE TEST 07",
                  "saksnummer" : "22947392",
                  "land" : "NO",
                  "personNr" : "14115327578"
                },
                "reduksjonsGrunn" : "03",
                "startdatoPensjonsRettighet" : "2021-09-01",
                "revurderingtidsfrist" : "six weeks from the date the decision is received",
                "vedtaksDato" : "2021-08-12",
                "innvilgetPensjon" : "01"
              }, {
                "ytelser" : [ {
                  "startdatoretttilytelse" : "2020-02-05",
                  "beloep" : [ {
                    "betalingshyppighetytelse" : "99",
                    "valuta" : "EUR",
                    "beloepBrutto" : "1254",
                    "utbetalingshyppighetAnnen" : "biannual"
                  } ]
                } ],
                "pensjonType" : "01",
                "tildeltePensjonerLand" : "SE",
                "adressatForRevurdering" : [ {
                  "adressatforrevurdering" : "gate\nbygning\nby\n4587\nregion\nSE"
                } ],
                "institusjon" : {
                  "institusjonsid" : "SE:NAVAT07",
                  "institusjonsnavn" : "NAV ACCEPTANCE TEST JYZ",
                  "saksnummer" : "134513452",
                  "land" : "SE",
                  "personNr" : "345315327578"
                },
                "reduksjonsGrunn" : "02",
                "startdatoPensjonsRettighet" : "2017-02-01",
                "vedtaksDato" : "2020-01-02",
                "innvilgetPensjon" : "02"
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

        println(response)

        JSONAssert.assertEquals(response, validResponse, false)
    }


    @Test
    fun `prefill sed P7000 - Gitt en alderspensjon med flere P6000 med avslag skal det preutfylles gyldig SED`() {
        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN_3)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_3)) } returns PersonPDLMock.createWith(true, "Alder", "Pensjon", FNR_VOKSEN_3, AKTOER_ID)
        every { kodeverkClient.finnLandkode(any())} returns "QX"

        //mock p6000 fra RINA med data som skal benyttes i P7000
        val p6000fraRequest = listOf(mockP6000requestdata("SE", "P6000SE-INNV.json"), mockP6000requestdata("NO", "P6000SE-INNV.json"))
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
                      "institusjonsnavn" : "NOINST002, NO INST002, NO",
                      "institusjonsid" : "NO:noinst002",
                      "identifikator" : "12312312312",
                      "land" : "NO"
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
                "dato" : "2019-10-01",
                "tidsfristForRevurdering" : "seven weeks from the date the decision is received",
                "adressatforRevurderingAvslag" : [ {
                  "adressatforrevurdering" : "Olesgate 15\nOslo\n0130\nNO"
                } ]
              }, {
                "pensjonType" : "01",
                "begrunnelse" : "03",
                "dato" : "2019-10-01",
                "tidsfristForRevurdering" : "seven weeks from the date the decision is received",
                "pin" : {
                  "institusjonsnavn" : "NOINST002, NO INST002, NO",
                  "institusjonsid" : "NO:noinst002",
                  "identifikator" : "11067122781",
                  "land" : "NO"
                },
                "adressatforRevurderingAvslag" : [ {
                  "adressatforrevurdering" : "Olesgate 15\nOslo\n0130\nNO"
                } ]
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



    private fun mockP6000KomplettRequestdata(land: String, type: String? = "01") =  Pair(P6000Dokument(SedType.P6000, "123123", "23423asdasd3243423", land, "1", "url", LocalDate.of(2020, 10, 12), if (land == "NO") Retning.OUT else Retning.IN),
        mapJsonToAny(mockKomplettP6000(land, type), typeRefs<P6000>()))

    private fun mockP6000requestdata(land: String, filnavn: String) =  Pair(P6000Dokument(SedType.P6000, "123123", "23423asdasd3243423", land, "1", "url", LocalDate.of(2021, 11,13), if (land == "NO") Retning.OUT else Retning.IN),
        getP6000ekternfil(filnavn) ) }

    private fun getP6000ekternfil(filnavn: String): P6000 = mapJsonToAny(ResourceUtils.getFile("classpath:json/nav/$filnavn").readText(), typeRefs<P6000>())

    private fun mockP6000gjenlev() : String {
        return """
    "gjenlevende" : {
      "person" : {
        "pin" : [ {
          "institusjonsnavn" : "NOINST002, NO INST002, NO",
          "institusjonsid" : "NO:noinst002",
          "identifikator" : "11067122781",
          "land" : "NO",
          "institusjon" : {
            "saksnummer" : "24234sdsd-4",
            "land" : "NO",
            "personNr" : "01126712345",
            "innvilgetPensjon" : "01"
          }
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
   ${mockP6000gjenlev()}
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
            "utbetalingshyppighet": "Annet"
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
        "artikkel": "05",
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
          "institusjonsadresse": "Olesgate 15",
          "postnummer": "0130",
          "poststed": "Oslo",
          "land": "NO"
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
          "datoFrist": "2026-01-23"
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
        "type": "02",
        "virkningsdato": [
          {
            "sluttdato": "2034-10-01",
            "startdato": "2033-10-01"
          }
        ],
        "aarsak": {
          "annenytelseellerinntekt": "02",
          "inntektAnnen": "ewrwer"
        }
      }
    ]
  },
  "sedVer": "0",
  "sedGVer": "4",
  "sed": "P6000"
}
            
        """.trimIndent()
    }


