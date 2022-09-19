package no.nav.eessi.pensjon.integrationtest.sed

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.eux.model.sed.X009
import no.nav.eessi.pensjon.integrationtest.IntegrasjonsTestConfig
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.model.AktoerId
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentType
import no.nav.eessi.pensjon.personoppslag.pdl.model.NorskIdent
import no.nav.eessi.pensjon.prefill.ApiRequest
import no.nav.eessi.pensjon.prefill.ApiSubject
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.SubjectFnr
import no.nav.eessi.pensjon.prefill.models.KravType
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.utils.toJson
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
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

@SpringBootTest(classes = [IntegrasjonsTestConfig::class, UnsecuredWebMvcTestLauncher::class, SedPrefillIntegrationSpringTest.TestConfig::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("unsecured-webmvctest", "excludeKodeverk")
@AutoConfigureMockMvc
@EmbeddedKafka
class SedPrefillIntegrationSpringTest {

    @MockkBean
    private lateinit var pensjonsinformasjonOidcRestTemplate: RestTemplate

    @MockkBean
    private lateinit var kodeverkClient: KodeverkClient

    @MockkBean
    private lateinit var personService: PersonService

    @Autowired
    private lateinit var mockMvc: MockMvc

    private companion object {
        const val FNR_VOKSEN = "11067122781"    // KRAFTIG VEGGPRYD
        const val FNR_VOKSEN_3 = "12312312312"
        const val FNR_VOKSEN_4 = "9876543210"

        const val AKTOER_ID = "0123456789000"
        const val AKTOER_ID_2 = "0009876543210"
    }

    @TestConfiguration
    internal class TestConfig{
        @Bean
        @Primary
        fun restTemplate(): RestTemplate = mockk()
    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P6000 missing vedtakid throw error bad request and reason Mangler vedtakID`() {

        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID )) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, fnr = FNR_VOKSEN, aktoerid = AKTOER_ID)

        val apijson = dummyApijson(sakid = "EESSI-PEN-123", aktoerId = AKTOER_ID, sedType = SedType.P6000)

        mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andExpect(status().isBadRequest)
                .andExpect(status().reason(Matchers.containsString("Mangler vedtakID")))

    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P2000 missing saksnummer throw error bad request and reason Mangler sakId`() {
        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID )) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, fnr = FNR_VOKSEN, aktoerid = AKTOER_ID)

        val apijson = dummyApijson(sakid = "", aktoerId = AKTOER_ID)

        mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andExpect(status().isBadRequest)
                .andExpect(status().reason(Matchers.containsString("Mangler sakId")))

    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P2000 alder with uføre pensjondata throw error bad request and mesage Du kan ikke opprette alderspensjonskrav i en uføretrygdsak`() {

        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID )) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, fnr = FNR_VOKSEN, aktoerid = AKTOER_ID)

        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2200-UP-INNV.xml")

        val apijson = dummyApijson(sakid = "22874955", aktoerId = AKTOER_ID)

        mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andExpect(status().isBadRequest)
                .andExpect(status().reason(Matchers.containsString("Du kan ikke opprette alderspensjonskrav i en uføretrygdsak (PESYS-saksnr: 22874955 har sakstype UFOREP)")))

    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P2000 med sak fra GJENLEV feiler`() {
        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, fnr = FNR_VOKSEN, aktoerid = AKTOER_ID)
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/GJ_P2000_BH_MED_UTL.xml")

        //feil saknr
        val apijson = dummyApijson(sakid = "22932988", aktoerId = AKTOER_ID)

        mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isBadRequest)
            .andExpect(status().reason(Matchers.containsString("Det finnes ingen iverksatte vedtak for førstegangsbehandling kun utland. Vennligst gå til EESSI-Pensjon fra vedtakskontekst.")))

    }


    @Test
    @Throws(Exception::class)
    fun `prefill sed P6000 P_BUC_02 Gjenlevende har med avdod skal returnere en gyldig SED`() {
        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentIdent(IdentType.AktoerId, NorskIdent(FNR_VOKSEN_4)) } returns AktoerId(AKTOER_ID_2)

        every { personService.hentPerson(NorskIdent(FNR_VOKSEN))  } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN, AKTOER_ID)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_4)) } returns PersonPDLMock.createWith(true, "Avdød", "Død", FNR_VOKSEN_4, AKTOER_ID_2, true)

        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/vedtak/P6000-BARNEP-GJENLEV.xml")
        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val apijson = dummyApijson(sakid = "22874955", vedtakid = "987654321122355466", aktoerId = AKTOER_ID, sedType = SedType.P6000, buc = "P_BUC_02", fnravdod = FNR_VOKSEN_4)

        val result = mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val mapper = jacksonObjectMapper()
        val sedRootNode = mapper.readTree(response)
        val gjenlevendePIN = finnPin(sedRootNode.at("/pensjon/gjenlevende/person"))
        val avdodPIN = finnPin(sedRootNode.at("/nav/bruker"))

        Assertions.assertEquals(FNR_VOKSEN, gjenlevendePIN)
        Assertions.assertEquals(FNR_VOKSEN_4, avdodPIN)

        println(response)

    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P6000 P_BUC_01 Alderpensjon med avslag skal returnere en gyldig SED`() {
        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN_3)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_3))} returns PersonPDLMock.createWith(true, "Alder", "Pensjonist", FNR_VOKSEN_3)

        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/vedtak/P6000-AP-Avslag.xml")
        every { kodeverkClient.finnLandkode(any()) } returns "QX"
        val apijson = dummyApijson( sakid = "22874955", vedtakid = "123123423423", aktoerId = AKTOER_ID, sedType = SedType.P6000, buc = "P_BUC_01")

        val result = mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val validResponse = """
            {
              "sed" : "P6000",
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
                      "identifikator" : "12312312312",
                      "land" : "NO"
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
                    "land" : "NO"
                  }
                }
              },
              "pensjon" : {
                "vedtak" : [ {
                  "type" : "01",
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
                    "institusjonsid" : "NO:noinst002",
                    "institusjonsnavn" : "NOINST002, NO INST002, NO",
                    "institusjonsadresse" : "Postboks 6600 Etterstad TEST",
                    "postnummer" : "0607",
                    "land" : "NO",
                    "poststed" : "Oslo"
                  } ]
                }
              }
            }
        """.trimIndent()

        JSONAssert.assertEquals(response, validResponse, true)
    }


    @Test
    @Throws(Exception::class)
    fun `prefill sed P3000_SE Gjenlevende har med avdod skal returnere en gyldig SED`() {

        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentIdent(IdentType.AktoerId, NorskIdent(FNR_VOKSEN_4))} returns AktoerId(AKTOER_ID_2)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN, AKTOER_ID)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_4)) } returns PersonPDLMock.createWith(true, "Avdød", "Død", FNR_VOKSEN_4, AKTOER_ID_2, true)
        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val apijson = dummyApijson(sakid = "22874955", vedtakid = "9876543211", aktoerId = AKTOER_ID, sedType = SedType.P3000_SE, buc = "P_BUC_10",  fnravdod = FNR_VOKSEN_4)

        val result = mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val mapper = jacksonObjectMapper()
        val sedRootNode = mapper.readTree(response)
        val gjenlevendePIN = finnPin(sedRootNode.at("/pensjon/gjenlevende/person"))
        val annenPersonPIN = finnPin(sedRootNode.at("/nav/annenperson/person"))
        val avdodPIN = finnPin(sedRootNode.at("/nav/bruker"))

        Assertions.assertEquals(FNR_VOKSEN, gjenlevendePIN)
        Assertions.assertEquals(FNR_VOKSEN, annenPersonPIN)
        Assertions.assertEquals(FNR_VOKSEN_4, avdodPIN)

    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P5000 med Gjenlevende og avdod skal returnere en gyldig SED`() {

        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentIdent(IdentType.AktoerId, NorskIdent(FNR_VOKSEN_4))} returns AktoerId(AKTOER_ID_2)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN, AKTOER_ID)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_4)) } returns PersonPDLMock.createWith(true, "Avdød", "Død", FNR_VOKSEN_4, AKTOER_ID_2, true)
        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val apijson = dummyApijson(sakid = "22874955", vedtakid = "9876543211", aktoerId = AKTOER_ID, sedType = SedType.P5000, buc = "P_BUC_10",  fnravdod = FNR_VOKSEN_4)

        val result = mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val mapper = jacksonObjectMapper()
        val sedRootNode = mapper.readTree(response)
        val gjenlevendePIN = finnPin(sedRootNode.at("/pensjon/gjenlevende/person"))
        val avdodPIN = finnPin(sedRootNode.at("/nav/bruker"))

        Assertions.assertEquals(FNR_VOKSEN, gjenlevendePIN)
        Assertions.assertEquals(FNR_VOKSEN_4, avdodPIN)

    }


    @Test
    @Throws(Exception::class)
    fun `prefill sed P4000 med forsikret person skal returnere en gyldig SED`() {

        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN_3)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_3)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN_3)
        every { kodeverkClient.finnLandkode(any()) } returns "QX"
        val apijson = dummyApijson(sakid = "22874955", vedtakid = "9876543211", aktoerId = AKTOER_ID, sedType = SedType.P4000, buc = "P_BUC_05")

        val result = mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val validResponse = """
            {
              "sed" : "P4000",
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
                      "identifikator" : "$FNR_VOKSEN_3",
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

        val mapper = jacksonObjectMapper()
        val sedRootNode = mapper.readTree(response)
        val forsikretPin = finnPin(sedRootNode.at("/nav/bruker"))

        Assertions.assertEquals(FNR_VOKSEN_3, forsikretPin)
        JSONAssert.assertEquals(response, validResponse, true)

    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P2000 alder return valid sedjson`() {

        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2000-AP-UP-21337890.xml")
        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val apijson = dummyApijson(sakid = "21337890", aktoerId = AKTOER_ID)

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

    @Test
    fun `prefill sed P2000 alder med overgang fra ufore med sakstatus Ukjent return valid sedjson`() {

        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2000-AP-UTL-UKJENT-12065212345.xml")
        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val apijson = dummyApijson(sakid = "21841174", aktoerId = AKTOER_ID)


        val result = mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        println(response)
        val expected = """
            {
              "sed" : "P2000",
              "nav" : {
                "eessisak" : [ {
                  "institusjonsid" : "NO:noinst002",
                  "institusjonsnavn" : "NOINST002, NO INST002, NO",
                  "saksnummer" : "21841174",
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
                  "dato" : "2015-11-25"
                }
              },
              "pensjon" : {
                "kravDato" : {
                  "dato" : "2015-11-25"
                }
              },
              "sedGVer" : "4",
              "sedVer" : "2"
            }             
        """.trimIndent()

        JSONAssert.assertEquals(response, expected, false)

    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P2000 alder F_BH_KUN_UTL return valid sedjson`() {
        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/AP_2000_KUN_UTLAND.xml")
        every { kodeverkClient.finnLandkode(any()) } returns "QX"


        val apijson = dummyApijson(sakid = "22932784", aktoerId = AKTOER_ID)

        val validResponse = """
        {
          "sed" : "P2000",
          "sedGVer" : "4",
          "sedVer" : "2",
          "nav" : {
            "eessisak" : [ {
              "institusjonsid" : "NO:noinst002",
              "institusjonsnavn" : "NOINST002, NO INST002, NO",
              "saksnummer" : "22932784",
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
              "dato" : "2021-03-01"
            }
          },
          "pensjon" : {
            "kravDato" : {
              "dato" : "2021-03-01"
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
    fun `prefill sed P2000 alder med AVSL returnerer en valid sedjson`() {

        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2000krav-alderpensjon-avslag.xml")
        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val apijson = dummyApijson(sakid = "22889955", aktoerId = AKTOER_ID)
        val result = mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val validResponse = """
            {
              "sed" : "P2000",
              "sedGVer" : "4",
              "sedVer" : "2",
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
                    "land" : "NO"
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

        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN_3)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_3)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN_3)
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns
                PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/AP_FORSTEG_BH.xml") andThen
                PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/vedtak/P6000-APUtland-301.xml")

        every { kodeverkClient.finnLandkode(any()) } returns "QX"


        val apijson = dummyApijson(sakid = "22580170", aktoerId = AKTOER_ID, vedtakid = "5134513451345")

        val result = mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val validResponse = """
            {
              "sed" : "P2000",
              "sedGVer" : "4",
              "sedVer" : "2",
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
                    "land" : "NO"
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
    fun `prefill sed med kun utland, ikke korrekt sakid skal kaste en Exception`() {
        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2000-AP-KUNUTL-IKKEVIRKNINGTID.xml")

        val apijson = dummyApijson(sakid = "1232123123", aktoerId = AKTOER_ID)
        val expectedError = """Det finnes ingen iverksatte vedtak for førstegangsbehandling kun utland. Vennligst gå til EESSI-Pensjon fra vedtakskontekst.""".trimIndent()

        mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andExpect(status().isBadRequest)
                .andExpect(status().reason(Matchers.containsString(expectedError)))

    }

    @Test
    fun `prefill sed med kravtype førstehangbehandling skal kaste en Exception`() {

        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/AP_FORSTEG_BH.xml")

        val apijson = dummyApijson(sakid = "22580170", aktoerId = AKTOER_ID)

        mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andExpect(status().isBadRequest)
                .andExpect(status().reason(Matchers.containsString("Det er ikke markert for bodd/arbeidet i utlandet. Krav SED P2000 blir ikke opprettet")))

    }

    @Test
    fun `prefill sed med ALDERP uten korrekt kravårsak skal kaste en Exception`() {
        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN_3)
        every { personService.hentIdent(IdentType.AktoerId, NorskIdent(FNR_VOKSEN_4)) } returns AktoerId(AKTOER_ID_2)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_3)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN_3, AKTOER_ID)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_4)) } returns PersonPDLMock.createWith(true, "Avdød", "Død", FNR_VOKSEN_4, AKTOER_ID_2, true)
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2000-AP-LP-RVUR-20541862.xml")

        every { kodeverkClient.finnLandkode(eq("NOR")) } returns "NO"

        val apijson = dummyApijson(sakid = "20541862", aktoerId = AKTOER_ID, sedType = SedType.P2100, buc = "P_BUC_02", fnravdod = FNR_VOKSEN_4)

        mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andExpect(status().isBadRequest)
                .andExpect(status().reason(Matchers.containsString("Ingen gyldig kravårsak funnet for ALDER eller UFØREP for utfylling av en krav SED P2100")))
    }

    @Test
    fun `prefill sed X010 valid sedjson`() {

        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2000-AP-UP-21337890.xml")
        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val apijson = dummyApijson(sakid = "21337890", aktoerId = AKTOER_ID, sedType = SedType.X010)

        val validResponse = """
                {
                  "sed" : "X010",
                  "sedGVer" : "4",
                  "sedVer" : "2",
                  "nav" : {
                    "sak" : {
                      "kontekst" : {
                        "bruker" : {
                          "person" : {
                            "etternavn" : "Testesen",
                            "fornavn" : "Test",
                            "kjoenn" : "M",
                            "foedselsdato" : "1988-07-12"
                          }
                        }
                      },
                      "paaminnelse" : {
                        "svar" : {
                          "informasjon" : {
                            "kommersenere" : [ { 
                                "type": "dokument",
                                "opplysninger": "."
                            } ]
                          }
                        }
                      }
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
    fun `prefill sed X010 med json data fra X009 gir en valid X010`() {

        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2000-AP-UP-21337890.xml")
        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val x009 = SED.fromJsonToConcrete(PrefillTestHelper.readJsonResponse("/json/nav/X009-NAV.json")) as X009

        val apijson = dummyApijson(sakid = "21337890", aktoerId = AKTOER_ID, sedType = SedType.X010, payload = x009.toJson())

        val validResponse = """
            {
              "sed" : "X010",
              "nav" : {
                "sak" : {
                  "kontekst" : {
                    "bruker" : {
                      "person" : {
                        "etternavn" : "Testesen",
                        "fornavn" : "Test",
                        "kjoenn" : "M",
                        "foedselsdato" : "1988-07-12"
                      }
                    }
                  },
                  "paaminnelse" : {
                    "svar" : {
                      "informasjon" : {
                        "ikketilgjengelig" : [ {
                          "type" : "sed",
                          "opplysninger" : "Missing details",
                          "grunn" : {
                            "type" : "annet",
                            "annet" : "Missing details"
                          }
                        } ],
                        "kommersenere" : [ {
                          "type" : "dokument",
                          "opplysninger" : "æøå"
                        }, {
                          "type" : "sed",
                          "opplysninger" : "P5000"
                        } ]
                      }
                    }
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



    @Test
    @Throws(Exception::class)
    fun `prefill sed med uten korrekt kravtype skal kaste en Exception`() {
        every { personService.hentIdent(IdentType.NorskIdent, AktoerId(AKTOER_ID )) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, fnr = FNR_VOKSEN, aktoerid = AKTOER_ID)
        every { pensjonsinformasjonOidcRestTemplate .exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java))} returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2000-AP-MANGLER_BOSATT_UTLAND.xml")

        val apijson = dummyApijson(sakid = "21920707", aktoerId = AKTOER_ID)

        mockMvc.perform(post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
                .andExpect(status().isBadRequest)
                .andExpect(status().reason(Matchers.containsString("Det finnes ingen iverksatte vedtak for førstegangsbehandling kun utland. Vennligst gå til EESSI-Pensjon fra vedtakskontekst.")))
    }

    private fun finnPin(pinNode: JsonNode): String? {
        return pinNode.findValue("pin")
                .filter { pin -> pin.get("land").textValue() == "NO" }
                .map { pin -> pin.get("identifikator").textValue() }
                .lastOrNull()
    }

}

fun dummyApi(
    sakid: String,
    vedtakid: String? = null,
    aktoerId: String,
    sedType: SedType = SedType.P2000,
    buc: String? = "P_BUC_06",
    fnravdod: String? = null,
    kravtype: KravType? = null,
    kravdato: String? = null,
    payload: String? = null
): ApiRequest {
    val subject = if (fnravdod != null) {
        ApiSubject(null, SubjectFnr(fnravdod))
    } else {
        null
    }
    return ApiRequest(
        sakId = sakid,
        vedtakId = vedtakid,
        kravId = null,
        aktoerId = aktoerId,
        sed = sedType.name,
        buc = buc,
        kravType = kravtype,
        kravDato = kravdato,
        euxCaseId = "12345",
        institutions = emptyList(),
        subject = subject,
        payload = payload
    )
}

fun dummyApijson(sakid: String, vedtakid: String? = null, aktoerId: String, sedType: SedType = SedType.P2000, buc: String? = "P_BUC_06", fnravdod: String? = null, kravtype: KravType? = null, kravdato: String? = null, payload: String? = null): String {
    return dummyApi(sakid, vedtakid, aktoerId, sedType, buc, fnravdod, kravtype, kravdato, payload).toJson()
}