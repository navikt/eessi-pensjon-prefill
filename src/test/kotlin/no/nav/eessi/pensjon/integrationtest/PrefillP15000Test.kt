package no.nav.eessi.pensjon.integrationtest

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.fagmodul.personoppslag.BrukerMock
import no.nav.eessi.pensjon.fagmodul.prefill.model.KravType
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonService
import no.nav.eessi.pensjon.personoppslag.aktoerregister.AktoerId
import no.nav.eessi.pensjon.personoppslag.aktoerregister.AktoerregisterService
import no.nav.eessi.pensjon.personoppslag.aktoerregister.IdentGruppe
import no.nav.eessi.pensjon.personoppslag.aktoerregister.NorskIdent
import no.nav.eessi.pensjon.personoppslag.personv3.PersonV3Service
import no.nav.eessi.pensjon.security.sts.STSService
import no.nav.eessi.pensjon.services.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.services.pensjonsinformasjon.EPSaktype
import no.nav.eessi.pensjon.services.pensjonsinformasjon.KravArsak
import no.nav.pensjon.v1.avdod.V1Avdod
import no.nav.pensjon.v1.kravhistorikk.V1KravHistorikk
import no.nav.pensjon.v1.kravhistorikkliste.V1KravHistorikkListe
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.sak.V1Sak
import no.nav.pensjon.v1.vedtak.V1Vedtak
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
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
class PrefillP15000Test {

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

    @MockBean
    lateinit var pensjoninformasjonservice: PensjonsinformasjonService

    @Autowired
    private lateinit var mockMvc: MockMvc


    @Test
    @Throws(Exception::class)
    fun `prefill P15000 P_BUC_10 fra vedtakskontekst hvor saktype er GJENLEV og pensjoninformasjon gir BARNEP med GJENLEV`() {

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(AktoerId("3323332333233323")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.AktoerId, NorskIdent("9876543210"))

        doReturn(BrukerMock.createWith(true, "Lever", "Gjenlev", "12312312312")).`when`(personV3Service).hentBruker("12312312312")
        doReturn(BrukerMock.createWith(true, "Avdød", "Død", "9876543210", erDod = true)).`when`(personV3Service).hentBruker("9876543210")

        val banrepSak = V1Sak()
        banrepSak.sakType = "BARNEP"
        banrepSak.sakId = 22915555L
        banrepSak.status = "INNV"

        doReturn(banrepSak).`when`(pensjoninformasjonservice).hentRelevantPensjonSak(any(), any())

        val pensjonsinformasjon = Pensjonsinformasjon()
        val avdod = V1Avdod()
        avdod.avdodFar = "9876543210"
        avdod.avdodFarAktorId = "3323332333233323"
        avdod.avdodMor = "12312312441"
        avdod.avdodMorAktorId = "123343242034739845719384257134513"

        pensjonsinformasjon.avdod = avdod
        pensjonsinformasjon.vedtak = V1Vedtak()

        val v1Kravhistorikk = V1KravHistorikk()
        v1Kravhistorikk.kravArsak = KravArsak.GJNL_SKAL_VURD.name

        val sak = V1Sak()
        sak.sakType = EPSaktype.BARNEP.toString()
        sak.sakId = 100
        sak.kravHistorikkListe = V1KravHistorikkListe()
        sak.kravHistorikkListe.kravHistorikkListe.add(v1Kravhistorikk)

        doReturn(pensjonsinformasjon).`when`(pensjoninformasjonservice).hentMedVedtak("123123123")
        doReturn("QX").doReturn("XQ").`when`(kodeverkClient).finnLandkode2(any())

        val apijson =  dummyApijson(sakid = "22915555", vedtakid = "123123123", aktoerId = "0105094340092", sed = "P15000", buc = "P_BUC_10", kravtype = KravType.GJENLEV, kravdato = "01-01-2020", fnravdod = "9876543210")

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
          "sed" : "P15000",
          "sedGVer" : "4",
          "sedVer" : "1",
          "nav" : {
            "eessisak" : [ {
              "institusjonsid" : "NO:noinst002",
              "institusjonsnavn" : "NOINST002, NO INST002, NO",
              "saksnummer" : "22915555",
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
              }
            },
            "krav" : {
              "dato" : "01-01-2020",
              "type" : "02"
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
                  "land" : "XQ"
                } ],
                "etternavn" : "Gjenlev",
                "fornavn" : "Lever",
                "kjoenn" : "M",
                "foedselsdato" : "1988-07-12",
                "relasjontilavdod" : {
                  "relasjon" : "06"
                },
                "rolle" : "01"
              },
              "adresse" : {
                "gate" : "Oppoverbakken 66",
                "by" : "SØRUMSAND",
                "postnummer" : "1920",
                "land" : "XQ"
              }
            }
          }
        }
        """.trimIndent()

    JSONAssert.assertEquals(response, validResponse, true)

   }

    @Test
    @Throws(Exception::class)
    fun `prefill P15000 P_BUC_10 fra vedtakskontekst hvor saktype er ALDER og pensjoninformasjon returnerer ALDER med GJENLEV`() {

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(AktoerId("3323332333233323")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.AktoerId, NorskIdent("9876543210"))

        doReturn(BrukerMock.createWith(true, "Lever", "Gjenlev", "12312312312")).`when`(personV3Service).hentBruker("12312312312")
        doReturn(BrukerMock.createWith(true, "Avdød", "Død", "9876543210", erDod = true)).`when`(personV3Service).hentBruker("9876543210")

        val aldersak = V1Sak()
        aldersak.sakType = "ALDER"
        aldersak.sakId = 22915555L
        aldersak.status = "INNV"

        doReturn(aldersak).`when`(pensjoninformasjonservice).hentRelevantPensjonSak(any(), any())

        val pensjonsinformasjon = Pensjonsinformasjon()
        val avdod = V1Avdod()
        avdod.avdod = "9876543210"
        avdod.avdodAktorId = "3323332333233323"

        pensjonsinformasjon.avdod = avdod
        pensjonsinformasjon.vedtak = V1Vedtak()

        val v1Kravhistorikk = V1KravHistorikk()
        v1Kravhistorikk.kravArsak = KravArsak.GJNL_SKAL_VURD.name

        val sak = V1Sak()
        sak.sakType = EPSaktype.ALDER.toString()
        sak.sakId = 100
        sak.kravHistorikkListe = V1KravHistorikkListe()
        sak.kravHistorikkListe.kravHistorikkListe.add(v1Kravhistorikk)

        doReturn(pensjonsinformasjon).`when`(pensjoninformasjonservice).hentMedVedtak("123123123")
        doReturn("QX").doReturn("XQ").`when`(kodeverkClient).finnLandkode2(any())

        val apijson =  dummyApijson(sakid = "22915555", vedtakid = "123123123", aktoerId = "0105094340092", sed = "P15000", buc = "P_BUC_10", kravtype = KravType.ALDER, kravdato = "01-01-2020", fnravdod = "9876543210")

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
          "sed" : "P15000",
          "sedGVer" : "4",
          "sedVer" : "1",
          "nav" : {
            "eessisak" : [ {
              "institusjonsid" : "NO:noinst002",
              "institusjonsnavn" : "NOINST002, NO INST002, NO",
              "saksnummer" : "22915555",
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
                "postnummer" : "1920",
                "land" : "XQ"
              }
            },
            "krav" : {
              "dato" : "01-01-2020",
              "type" : "01"
            }
          }
        }
        """.trimIndent()

        JSONAssert.assertEquals(response, validResponse, true)

    }

    @Test
    @Throws(Exception::class)
    fun `prefill P15000 P_BUC_10 hvor saktype er ALDER men data fra pensjonsinformasjon gir UFOREP som resulterer i en bad request`() {

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(BrukerMock.createWith()).`when`(personV3Service).hentBruker(any())

        val aldersak = V1Sak()
        aldersak.sakType = "UFOREP"
        aldersak.sakId = 22874955
        aldersak.status = "INNV"

        doReturn(aldersak).`when`(pensjoninformasjonservice).hentRelevantPensjonSak(any(), any())

        val pensjonsinformasjon = Pensjonsinformasjon()
        pensjonsinformasjon.vedtak = V1Vedtak()
        pensjonsinformasjon.vedtak.vedtakStatus = "INNV"

        doReturn(pensjonsinformasjon).`when`(pensjoninformasjonservice).hentMedVedtak("123123123")

        val apijson = dummyApijson(sakid = "22874955", vedtakid = "123123123", aktoerId = "0105094340092", sed = "P15000", buc = "P_BUC_10", kravtype = KravType.ALDER, kravdato = "01-01-2020")

        mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(status().reason(Matchers.containsString("Du kan ikke opprette alderspensjonskrav i en uføretrygdsak (PESYS-saksnr: 22874955 har sakstype UFOREP)")))

    }

    @Test
    @Throws(Exception::class)
    fun `prefill P15000 P_BUC_10 hvor saktype er UFOREP men data fra pensjonsinformasjon gir ALDER som resulterer i en bad request`() {

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(BrukerMock.createWith()).`when`(personV3Service).hentBruker(any())

        val aldersak = V1Sak()
        aldersak.sakType = "ALDER"
        aldersak.sakId = 21337890
        aldersak.status = "INNV"

        doReturn(aldersak).`when`(pensjoninformasjonservice).hentRelevantPensjonSak(any(), any())

        val pensjonsinformasjon = Pensjonsinformasjon()
        pensjonsinformasjon.vedtak = V1Vedtak()
        pensjonsinformasjon.vedtak.vedtakStatus = "INNV"

        doReturn(pensjonsinformasjon).`when`(pensjoninformasjonservice).hentMedVedtak("123123123")

        val apijson = dummyApijson(sakid = "21337890", vedtakid = "123123123" , aktoerId = "0105094340092", sed = "P15000", buc = "P_BUC_10", kravtype = KravType.UFOREP, kravdato = "01-01-2020")

        mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(status().reason(Matchers.containsString("Du kan ikke opprette uføretrygdkrav i en alderspensjonssak (PESYS-saksnr: 21337890 har sakstype ALDER)")))
    }

    @Test
    @Throws(Exception::class)
    fun `prefill P15000 P_BUC_10 hvor saktype er ALDER`() {

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(BrukerMock.createWith(true, "Lever", "Gjenlev", "12312312312")).`when`(personV3Service).hentBruker("12312312312")

        val aldersak = V1Sak()
        aldersak.sakType = "ALDER"
        aldersak.sakId = 21337890
        aldersak.status = "INNV"

        doReturn(aldersak).`when`(pensjoninformasjonservice).hentRelevantPensjonSak(any(), any())

        val pensjonsinformasjon = Pensjonsinformasjon()
        pensjonsinformasjon.vedtak = V1Vedtak()
        pensjonsinformasjon.vedtak.vedtakStatus = "INNV"

        doReturn(pensjonsinformasjon).`when`(pensjoninformasjonservice).hentMedVedtak("123123123")

        doReturn("QX").doReturn("XQ").`when`(kodeverkClient).finnLandkode2(any())

        val apijson = dummyApijson(sakid = "21337890", vedtakid = "123123123" , aktoerId = "0105094340092", sed = "P15000", buc = "P_BUC_10", kravtype = KravType.ALDER, kravdato = "01-01-2020")

        val result = mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        print(response)

        val validResponse = """
            {
              "sed" : "P15000",
              "sedGVer" : "4",
              "sedVer" : "1",
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
                    "postnummer" : "1920",
                    "land" : "XQ"
                  }
                },
                "krav" : {
                  "dato" : "01-01-2020",
                  "type" : "01"
                }
              }
            }
        """.trimIndent()

        JSONAssert.assertEquals(response, validResponse, true)

    }

    @Test
    @Throws(Exception::class)
    fun `prefill P15000 P_BUC_10 hvor saktype er UFOREP`() {

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(BrukerMock.createWith(true, "Lever", "Gjenlev", "12312312312")).`when`(personV3Service).hentBruker("12312312312")

        val aldersak = V1Sak()
        aldersak.sakType = "UFOREP"
        aldersak.sakId = 22874955
        aldersak.status = "INNV"

        doReturn(aldersak).`when`(pensjoninformasjonservice).hentRelevantPensjonSak(any(), any())

        val pensjonsinformasjon = Pensjonsinformasjon()
        pensjonsinformasjon.vedtak = V1Vedtak()
        pensjonsinformasjon.vedtak.vedtakStatus = "INNV"

        doReturn(pensjonsinformasjon).`when`(pensjoninformasjonservice).hentMedVedtak("123123123")
        doReturn("QX").doReturn("XQ").`when`(kodeverkClient).finnLandkode2(any())

        val apijson = dummyApijson(sakid = "22874955", vedtakid = "123123123" ,aktoerId = "0105094340092", sed = "P15000", buc = "P_BUC_10", kravtype = KravType.UFOREP, kravdato = "01-01-2020")

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
              "sed" : "P15000",
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
                    "postnummer" : "1920",
                    "land" : "XQ"
                  }
                },
                "krav" : {
                  "dato" : "01-01-2020",
                  "type" : "03"
                }
              }
            }

        """.trimIndent()

        JSONAssert.assertEquals(response, validResponse, true)

    }

    @Test
    @Throws(Exception::class)
    fun `prefill P15000 P_BUC_10 hvor saktype er GJENLEV og pensjoninformasjon gir UFOREP med GJENLEV`() {

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(AktoerId("3323332333233323")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.AktoerId, NorskIdent("9876543210"))

        doReturn(BrukerMock.createWith(true, "Lever", "Gjenlev", "12312312312")).`when`(personV3Service).hentBruker("12312312312")
        doReturn(BrukerMock.createWith(true, "Avdød", "Død", "9876543210", erDod = true)).`when`(personV3Service).hentBruker("9876543210")

        val aldersak = V1Sak()
        aldersak.sakType = "UFOREP"
        aldersak.sakId = 22915550
        aldersak.status = "INNV"

        doReturn(aldersak).`when`(pensjoninformasjonservice).hentRelevantPensjonSak(any(), any())

        val pensjonsinformasjon = Pensjonsinformasjon()
        pensjonsinformasjon.vedtak = V1Vedtak()
        pensjonsinformasjon.vedtak.vedtakStatus = "INNV"

        doReturn(pensjonsinformasjon).`when`(pensjoninformasjonservice).hentMedVedtak("123123123")
        doReturn("QX").doReturn("XQ").`when`(kodeverkClient).finnLandkode2(any())

        val apijson = dummyApijson(sakid = "22915550", vedtakid = "123123123", aktoerId = "0105094340092", sed = "P15000", buc = "P_BUC_10", kravtype = KravType.GJENLEV, kravdato = "01-01-2020", fnravdod = "9876543210")

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
              "sed" : "P15000",
              "sedGVer" : "4",
              "sedVer" : "1",
              "nav" : {
                "eessisak" : [ {
                  "institusjonsid" : "NO:noinst002",
                  "institusjonsnavn" : "NOINST002, NO INST002, NO",
                  "saksnummer" : "22915550",
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
                  }
                },
                "krav" : {
                  "dato" : "01-01-2020",
                  "type" : "02"
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
                      "land" : "XQ"
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
                    "land" : "XQ"
                  }
                }
              }
            }
        """.trimIndent()

        JSONAssert.assertEquals(response, validResponse, true)

    }

    @Test
    @Throws(Exception::class)
    fun `prefill P15000 P_BUC_10 hvor saktype er GJENLEV og pensjoninformasjon gir BARNEP med GJENLEV`() {

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(AktoerId("3323332333233323")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.AktoerId, NorskIdent("9876543210"))

        doReturn(BrukerMock.createWith(true, "Lever", "Gjenlev", "12312312312")).`when`(personV3Service).hentBruker("12312312312")
        doReturn(BrukerMock.createWith(true, "Avdød", "Død", "9876543210", erDod = true)).`when`(personV3Service).hentBruker("9876543210")

        val aldersak = V1Sak()
        aldersak.sakType = "UFOREP"
        aldersak.sakId = 22915555
        aldersak.status = "INNV"

        doReturn(aldersak).`when`(pensjoninformasjonservice).hentRelevantPensjonSak(any(), any())

        val pensjonsinformasjon = Pensjonsinformasjon()
        pensjonsinformasjon.vedtak = V1Vedtak()
        pensjonsinformasjon.vedtak.vedtakStatus = "INNV"

        val avdod = V1Avdod()
        avdod.avdodFar = "9876543210"
        avdod.avdodFarAktorId = "3323332333233323"
        avdod.avdodMor = "12312312441"
        avdod.avdodMorAktorId = "123343242034739845719384257134513"
        pensjonsinformasjon.avdod = avdod

        doReturn(pensjonsinformasjon).`when`(pensjoninformasjonservice).hentMedVedtak("123123123")
        doReturn("QX").doReturn("XQ").`when`(kodeverkClient).finnLandkode2(any())

        val apijson = dummyApijson(sakid = "22915555", vedtakid = "123123123", aktoerId = "0105094340092", sed = "P15000", buc = "P_BUC_10", kravtype = KravType.GJENLEV, kravdato = "01-01-2020", fnravdod = "9876543210")

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
              "sed" : "P15000",
              "sedGVer" : "4",
              "sedVer" : "1",
              "nav" : {
                "eessisak" : [ {
                  "institusjonsid" : "NO:noinst002",
                  "institusjonsnavn" : "NOINST002, NO INST002, NO",
                  "saksnummer" : "22915555",
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
                  }
                },
                "krav" : {
                  "dato" : "01-01-2020",
                  "type" : "02"
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
                      "land" : "XQ"
                    } ],
                    "etternavn" : "Gjenlev",
                    "fornavn" : "Lever",
                    "kjoenn" : "M",
                    "foedselsdato" : "1988-07-12",
                    "relasjontilavdod" : {
                      "relasjon" : "06"
                    },
                    "rolle" : "01"
                  },
                  "adresse" : {
                    "gate" : "Oppoverbakken 66",
                    "by" : "SØRUMSAND",
                    "postnummer" : "1920",
                    "land" : "XQ"
                  }
                }
              }
            }
        """.trimIndent()

        JSONAssert.assertEquals(response, validResponse, true)

    }

}

