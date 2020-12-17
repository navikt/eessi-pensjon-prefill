package no.nav.eessi.pensjon.integrationtest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
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
class SedPrefillIntegrationSpringTest {

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
    fun `prefill sed P6000 missing vedtakid throw error bad request and reason Mangler vedtakID`() {

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))

        val apijson = dummyApijson(sakid = "EESSI-PEN-123", aktoerId = "0105094340092", sed = "P6000")

        mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andDo(print())
                .andExpect(status().isBadRequest)
                .andExpect(status().reason(Matchers.containsString("Mangler vedtakID")))

    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P2000 missing saksnummer throw error bad request and reason Mangler sakId`() {

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(BrukerMock.createWith()).`when`(personV3Service).hentBruker(any())

        val apijson = dummyApijson(sakid = "", aktoerId = "0105094340092")

        mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andDo(print())
                .andExpect(status().isBadRequest)
                .andExpect(status().reason(Matchers.containsString("Mangler sakId")))

    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P2000 alder with uføre pensjondata throw error bad request and mesage Du kan ikke opprette alderspensjonskrav i en uføretrygdsak`() {

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(BrukerMock.createWith()).`when`(personV3Service).hentBruker(any())
        doReturn(PrefillTestHelper.readXMLresponse("P2200-UP-INNV.xml")).`when`(restTemplate).exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))

        val apijson = dummyApijson(sakid = "22874955", aktoerId = "0105094340092")

        mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andDo(print())
                .andExpect(status().isBadRequest)
                .andExpect(status().reason(Matchers.containsString("Du kan ikke opprette alderspensjonskrav i en uføretrygdsak (PESYS-saksnr: 22874955 har sakstype UFOREP)")))

    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P5000 with missing avdodfnr and Subject throws error bad request`() {
        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))

        val apijson = dummyApijson(sakid = "EESSI-PEN-123", aktoerId = "0105094340092", sed = "P5000", buc = "P_BUC_02")

        mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andDo(print())
                .andExpect(status().isBadRequest)
                .andExpect(status().reason(Matchers.containsString("Mangler fnr for avdød")))

    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P5000 P_BUC_02 Gjenlevende har med avdod skal returnere en gyldig SED`() {

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(AktoerId("3323332333233323")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.AktoerId, NorskIdent("9876543210"))

        doReturn(BrukerMock.createWith(true, "Lever", "Gjenlev", "12312312312")).`when`(personV3Service).hentBruker("12312312312")
        doReturn(BrukerMock.createWith(true, "Avdød", "Død", "9876543210")).`when`(personV3Service).hentBruker("9876543210")

        doReturn(PrefillTestHelper.readXMLresponse("P2100-GL-UTL-INNV.xml")).`when`(restTemplate).exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))

        doReturn("QX").doReturn("XQ").`when`(kodeverkClient).finnLandkode2(any())

        val subject = dummyApiSubjectjson("9876543210")
        val apijson = dummyApijson(sakid = "22874955", aktoerId = "0105094340092", sed = "P5000", buc = "P_BUC_02", subject = subject)

        val result = mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val mapper = jacksonObjectMapper()
        val sedRootNode = mapper.readTree(response)
        val gjenlevendePIN = finnPin(sedRootNode.at("/pensjon/gjenlevende/person"))
        val avdodPIN = finnPin(sedRootNode.at("/nav/bruker"))

        Assertions.assertEquals("12312312312", gjenlevendePIN)
        Assertions.assertEquals("9876543210", avdodPIN)

    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P6000 P_BUC_02 Gjenlevende har med avdod skal returnere en gyldig SED`() {

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(AktoerId("3323332333233323")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.AktoerId, NorskIdent("9876543210"))

        doReturn(BrukerMock.createWith(true, "Lever", "Gjenlev", "12312312312")).`when`(personV3Service).hentBruker("12312312312")
        doReturn(BrukerMock.createWith(true, "Avdød", "Død", "9876543210")).`when`(personV3Service).hentBruker("9876543210")

        doReturn(PrefillTestHelper.readXMLVedtakresponse("P6000-BARNEP-GJENLEV.xml")).`when`(restTemplate).exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))

        doReturn("QX").doReturn("XQ").`when`(kodeverkClient).finnLandkode2(any())

        val subject = dummyApiSubjectjson("9876543210")
        val apijson = dummyApijson(sakid = "22874955", vedtakid = "987654321122355466", aktoerId = "0105094340092", sed = "P6000", buc = "P_BUC_02", subject = subject)

        val result = mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val mapper = jacksonObjectMapper()
        val sedRootNode = mapper.readTree(response)
        val gjenlevendePIN = finnPin(sedRootNode.at("/pensjon/gjenlevende/person"))
        val avdodPIN = finnPin(sedRootNode.at("/nav/bruker"))

        Assertions.assertEquals("12312312312", gjenlevendePIN)
        Assertions.assertEquals("9876543210", avdodPIN)

    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P3000_SE Gjenlevende har med avdod skal returnere en gyldig SED`() {

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(AktoerId("3323332333233323")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.AktoerId, NorskIdent("9876543210"))

        doReturn(BrukerMock.createWith(true, "Lever", "Gjenlev", "12312312312")).`when`(personV3Service).hentBruker("12312312312")
        doReturn(BrukerMock.createWith(true, "Avdød", "Død", "9876543210")).`when`(personV3Service).hentBruker("9876543210")
        doReturn("QX").doReturn("XQ").`when`(kodeverkClient).finnLandkode2(any())

        val subject = dummyApiSubjectjson("9876543210")
        val apijson = dummyApijson(sakid = "22874955", vedtakid = "9876543211", aktoerId = "0105094340092", sed = "P3000_SE", buc = "P_BUC_10",  subject = subject)

        val result = mockMvc.perform(post("/sed/preview")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val mapper = jacksonObjectMapper()
        val sedRootNode = mapper.readTree(response)
        val gjenlevendePIN = finnPin(sedRootNode.at("/pensjon/gjenlevende/person"))
        val annenPersonPIN = finnPin(sedRootNode.at("/nav/annenperson/person"))
        val avdodPIN = finnPin(sedRootNode.at("/nav/bruker"))

        Assertions.assertEquals("12312312312", gjenlevendePIN)
        Assertions.assertEquals("12312312312", annenPersonPIN)
        Assertions.assertEquals("9876543210", avdodPIN)

    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P4000 med forsikret person skal returnere en gyldig SED`() {

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))

        doReturn(BrukerMock.createWith(true, "Lever", "Gjenlev", "12312312312")).`when`(personV3Service).hentBruker("12312312312")
        doReturn("QX").doReturn("XQ").`when`(kodeverkClient).finnLandkode2(any())

        val apijson = dummyApijson(sakid = "22874955", vedtakid = "9876543211", aktoerId = "0105094340092", sed = "P4000", buc = "P_BUC_05")

        val result = mockMvc.perform(post("/sed/preview")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val validResponse = """
            {
              "sed" : "P4000",
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
                }
              }
            }
        """.trimIndent()

        val mapper = jacksonObjectMapper()
        val sedRootNode = mapper.readTree(response)
        val forsikretPin = finnPin(sedRootNode.at("/nav/bruker"))

        Assertions.assertEquals("12312312312", forsikretPin)
        JSONAssert.assertEquals(response, validResponse, true)



    }



    @Test
    @Throws(Exception::class)
    fun `prefill sed P2000 alder return valid sedjson`() {


        doReturn(NorskIdent("23123123")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(BrukerMock.createWith()).`when`(personV3Service).hentBruker(any())
        doReturn(PrefillTestHelper.readXMLresponse("P2000-AP-UP-21337890.xml")).`when`(restTemplate).exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))
        doReturn("QX").`when`(kodeverkClient).finnLandkode2(any())

        val apijson = dummyApijson(sakid = "21337890", aktoerId = "0105094340092")

        val validResponse = """
            {
              "sed" : "P2000",
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
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        JSONAssert.assertEquals(response, validResponse, false)

    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P2000 alder med AVSL returnerer en valid sedjson`() {


        doReturn(NorskIdent("23123123")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(BrukerMock.createWith()).`when`(personV3Service).hentBruker(any())
        doReturn(PrefillTestHelper.readXMLresponse("P2000krav-alderpensjon-avslag.xml")).`when`(restTemplate).exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))
        doReturn("QX").`when`(kodeverkClient).finnLandkode2(any())

        val apijson = dummyApijson(sakid = "22889955", aktoerId = "0105094340092")


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
              "sed" : "P2000",
              "sedGVer" : "4",
              "sedVer" : "1",
              "nav" : {
                "eessisak" : [ {
                  "institusjonsid" : "NO:noinst002",
                  "institusjonsnavn" : "NOINST002, NO INST002, NO",
                  "saksnummer" : "22889955",
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
                  "dato" : "2019-04-30"
                }
              },
              "pensjon" : {
                "kravDato" : {
                  "dato" : "2019-04-30"
                }
              }
            }
        """.trimIndent()

        JSONAssert.assertEquals(response, validResponse, true)

    }



    @Test
    fun `prefill sed med kravtype førstehangbehandling norge men med vedtak bodsatt utland skal prefylle sed`() {

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(BrukerMock.createWith(true, "Lever", "Gjenlev", "12312312312")).`when`(personV3Service).hentBruker("12312312312")

        doReturn(PrefillTestHelper.readXMLresponse("AP_FORSTEG_BH.xml")).
        doReturn(PrefillTestHelper.readXMLVedtakresponse("P6000-APUtland-301.xml")).
        `when`(restTemplate).exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))

        doReturn("QX").doReturn("XQ").`when`(kodeverkClient).finnLandkode2(any())

        val apijson = dummyApijson(sakid = "22580170", aktoerId = "0105094340092", vedtakid = "5134513451345")

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
              "sed" : "P2000",
              "sedGVer" : "4",
              "sedVer" : "1",
              "nav" : {
                "eessisak" : [ {
                  "institusjonsid" : "NO:noinst002",
                  "institusjonsnavn" : "NOINST002, NO INST002, NO",
                  "saksnummer" : "22580170",
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
                  "dato" : "2018-05-31"
                }
              },
              "pensjon" : {
                "kravDato" : {
                  "dato" : "2018-05-31"
                }
              }
            }

        """.trimIndent()

        JSONAssert.assertEquals(response, validResponse, true)

    }


    /** test på validering av pensjoninformasjon krav **/
    @Test
    @Throws(Exception::class)
    fun `prefill sed med kun utland, ikke korrekt sakid skal kaste en Exception`() {

        doReturn(NorskIdent("23123123")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(BrukerMock.createWith()).`when`(personV3Service).hentBruker(any())
        doReturn(PrefillTestHelper.readXMLresponse("P2000-AP-KUNUTL-IKKEVIRKNINGTID.xml")).`when`(restTemplate).exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))

        val apijson = dummyApijson(sakid = "1232123123", aktoerId = "0105094340092")
        val expectedError = """Dersom kravet gjelder "Førstegangsbehandling kun utland" eller "Utsendelse til avtaleland", se egen rutine på Navet.""".trimIndent()

        mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andDo(print())
                .andExpect(status().isNotFound)
                .andExpect(status().reason(Matchers.containsString(expectedError)))

    }


    @Test
    @Throws(Exception::class)
    fun `prefill sed med kravtype kun utland skal kaste en Exception`() {

        doReturn(NorskIdent("23123123")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(BrukerMock.createWith()).`when`(personV3Service).hentBruker(any())
        doReturn(PrefillTestHelper.readXMLresponse("P2000-AP-KUNUTL-IKKEVIRKNINGTID.xml")).`when`(restTemplate).exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))

        val apijson = dummyApijson(sakid = "21920707", aktoerId = "0105094340092")

        mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andDo(print())
                .andExpect(status().isBadRequest)
                .andExpect(status().reason(Matchers.containsString("Søknad gjelder Førstegangsbehandling kun utland. Se egen rutine på navet")))

    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed med kravtype førstehangbehandling skal kaste en Exception`() {

        doReturn(NorskIdent("23123123")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(BrukerMock.createWith()).`when`(personV3Service).hentBruker(any())
        doReturn(PrefillTestHelper.readXMLresponse("AP_FORSTEG_BH.xml")).`when`(restTemplate).exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))

        val apijson = dummyApijson(sakid = "22580170", aktoerId = "0105094340092")

        mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andDo(print())
                .andExpect(status().isBadRequest)
                .andExpect(status().reason(Matchers.containsString("Det er ikke markert for bodd/arbeidet i utlandet. Krav SED P2000 blir ikke opprettet")))

    }


    @Test
    @Throws(Exception::class)
    fun `prefill sed med ALDERP uten korrekt kravårsak skal kaste en Exception`() {
        //nå krever P2100 avdod person med så hvor ofre vil dette kunne skje?

        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(AktoerId("3323332333233323")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.AktoerId, NorskIdent("9876543210"))

        doReturn(BrukerMock.createWith(true, "Lever", "Gjenlev", "12312312312")).`when`(personV3Service).hentBruker("12312312312")
        doReturn(BrukerMock.createWith(true, "Avdød", "Død", "9876543210")).`when`(personV3Service).hentBruker("9876543210")

        doReturn(PrefillTestHelper.readXMLresponse("P2000-AP-LP-RVUR-20541862.xml")).`when`(restTemplate).exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))

        val subject = dummyApiSubjectjson("9876543210")
        val apijson = dummyApijson(sakid = "20541862", aktoerId = "0105094340092", sed = "P2100", buc = "P_BUC_02", subject = subject)

        mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andDo(print())
                .andExpect(status().isBadRequest)
                .andExpect(status().reason(Matchers.containsString("Ingen gyldig kravårsak funnet for ALDER eller UFØREP for utfylling av en krav SED P2100")))
    }


    @Test
    @Throws(Exception::class)
    fun `prefill sed med uten korrekt kravtype skal kaste en Exception`() {
        doReturn(NorskIdent("12312312312")).`when`(aktoerService).hentGjeldendeIdent(IdentGruppe.NorskIdent, AktoerId("0105094340092"))
        doReturn(BrukerMock.createWith(true, "Lever", "Gjenlev", "12312312312")).`when`(personV3Service).hentBruker("12312312312")
        doReturn(PrefillTestHelper.readXMLresponse("P2000-AP-MANGLER_BOSATT_UTLAND.xml")).`when`(restTemplate).exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))

        val apijson = dummyApijson(sakid = "21920707", aktoerId = "0105094340092", sed = "P2000")

        mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andDo(print())
                .andExpect(status().isBadRequest)
                .andExpect(status().reason(Matchers.containsString("Du kan ikke opprette krav-SED P2000 fra brukerkontekst. Dersom det gjelder Utsendelse til avtaleland, se egen rutine for utsendelse av SED på Navet.")))
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

    private fun dummyApiSubjectjson(avdodfnr: String): String {
        return """
            { 
                "gjenlevende" : null, 
                "avdod" : { "fnr": "$avdodfnr"}
            }              
        """.trimIndent()
    }


    private fun finnPin(pinNode: JsonNode): String? {
        return pinNode.findValue("pin")
                .filter { pin -> pin.get("land").textValue() == "NO" }
                .map { pin -> pin.get("identifikator").textValue() }
                .lastOrNull()
    }

}
