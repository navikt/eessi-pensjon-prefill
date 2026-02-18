package no.nav.eessi.pensjon.integrationtest.sed

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.eux.model.BucType
import no.nav.eessi.pensjon.eux.model.BucType.*
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.SedType.*
import no.nav.eessi.pensjon.eux.model.sed.*
import no.nav.eessi.pensjon.integrationtest.IntegrasjonsTestConfig
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.kodeverk.Postnummer
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.model.AktoerId
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentGruppe.AKTORID
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentGruppe.FOLKEREGISTERIDENT
import no.nav.eessi.pensjon.personoppslag.pdl.model.NorskIdent
import no.nav.eessi.pensjon.personoppslag.pdl.model.PdlPerson
import no.nav.eessi.pensjon.prefill.KrrService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.DigitalKontaktinfo
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper
import no.nav.eessi.pensjon.shared.api.ApiRequest
import no.nav.eessi.pensjon.shared.api.ApiSubject
import no.nav.eessi.pensjon.shared.api.SubjectFnr
import no.nav.eessi.pensjon.utils.toJson
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestTemplate

@SpringBootTest(classes = [IntegrasjonsTestConfig::class, UnsecuredWebMvcTestLauncher::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("unsecured-webmvctest", "excludeKodeverk")
@AutoConfigureMockMvc
@DirtiesContext
@EmbeddedKafka
class SedPrefillIntegrationSpringTest {

    @MockkBean
    private lateinit var pensjonsinformasjonOidcRestTemplate: RestTemplate
    @MockkBean
    private lateinit var kodeverkClient: KodeverkClient
    @MockkBean
    private lateinit var personService: PersonService
    @MockkBean
    private lateinit var krrService: KrrService
    @Autowired
    private lateinit var mockMvc: MockMvc

    private companion object {
        const val FNR_VOKSEN   = "11067122781"    // KRAFTIG VEGGPRYD
        const val FNR_VOKSEN_3 = "12312312312"
        const val FNR_VOKSEN_4 = "9876543210"

        const val AKTOER_ID   = "0123456789000"
        const val AKTOER_ID_2 = "0009876543210"
    }

    @BeforeEach
    fun setup(){
        every { krrService.hentPersonerFraKrr(any()) } returns DigitalKontaktinfo(epostadresse = "melleby11@melby.no", mobiltelefonnummer = "11111111", aktiv = true, personident = FNR_VOKSEN)
        every { krrService.hentPersonerFraKrr(eq(FNR_VOKSEN_4)) } returns DigitalKontaktinfo(epostadresse = "melleby11@melby.no", mobiltelefonnummer = "11111111", aktiv = true, personident = FNR_VOKSEN_4)
        every { kodeverkClient.hentPostSted(any()) } returns Postnummer("1068", "SØRUMSAND")
        every { kodeverkClient.finnLandkode(any()) } returns "QX"
    }

    @ParameterizedTest(name = "for verdier for sakId:{0}, vedtak:{1}, sedType:{2}, og feilmelding:{3}")
    @CsvSource(
        value = [
            "123, null, P6000, Mangler vedtakID",
            "null, 12121, P2000, Mangler sakId"],
        nullValues = ["null"]
    )
    @Throws(Exception::class)
    fun `Validering av prefill kaster exception`(sakId: String?, vedtakid: String?, sedType: String, expectedErrorMessage: String) {
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID )) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, fnr = FNR_VOKSEN, aktoerid = AKTOER_ID)

        val apijson = dummyApijson(sakid = sakId ?: "", vedtakid = vedtakid, aktoerId = AKTOER_ID, sedType = SedType.valueOf(sedType))

        mockMvcSedPrefill(apijson, expectedErrorMessage)
    }

    fun mockPersonService(fnr: String, aktoerId: String, fornavn: String? = null, etternavn: String? = null): PdlPerson {
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(aktoerId)) } returns NorskIdent(fnr)
        PersonPDLMock.createWith(true, fornavn = fornavn ?: "", fnr = fnr, aktoerid = aktoerId, etternavn = etternavn ?: "").also {
            every { personService.hentPerson(NorskIdent(fnr)) } returns PersonPDLMock.createWith(true, fornavn = fornavn ?: "", fnr = fnr, aktoerid = aktoerId, etternavn = etternavn ?: "")
            return it
        }
    }

    @ParameterizedTest(name = "{0} skal gi feilmelding:{2}")
    @CsvSource(
        value = [
            "med alder with uføre pensjondata throw error bad request, /pensjonsinformasjon/krav/P2200-UP-INNV.xml, " +
                    "Du kan ikke opprette alderspensjonskrav i en uføretrygdsak (PESYS-saksnr: 22874955 har sakstype UFOREP",
            "med sak fra GJENLEV feiler, /pensjonsinformasjon/krav/GJ_P2000_BH_MED_UTL.xml, " +
                    "Det finnes ingen iverksatte vedtak for førstegangsbehandling kun utland, eller sluttbehandling. Vennligst gå til EESSI-Pensjon fra vedtakskontekst."],
        nullValues = ["null"]
    )
    @Throws(Exception::class)
    fun `prefill sed `(testInfo: String, xmlResponse: String, feilmelding: String) {

        mockPersonService(FNR_VOKSEN, AKTOER_ID)

        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse(xmlResponse)

        val apijson = dummyApijson(sakid = "22874955", aktoerId = AKTOER_ID)

        mockMvcSedPrefill(apijson, feilmelding)

    }


    @Test
    @Throws(Exception::class)
    fun `prefill sed P6000 P_BUC_02 Gjenlevende har med avdod skal returnere en gyldig SED`() {
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentIdent(AKTORID, NorskIdent(FNR_VOKSEN_4)) } returns AktoerId(AKTOER_ID_2)

        every { personService.hentPerson(NorskIdent(FNR_VOKSEN))  } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN, AKTOER_ID)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_4)) } returns PersonPDLMock.createWith(true, "Avdød", "Død", FNR_VOKSEN_4, AKTOER_ID_2, true)

        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/vedtak/P6000-BARNEP-GJENLEV.xml")

        val apijson = dummyApijson(sakid = "22874955", vedtakid = "987654321122355466", aktoerId = AKTOER_ID, sedType = P6000, buc = P_BUC_02, fnravdod = FNR_VOKSEN_4)

        val result = mockMvcPrefill(apijson)

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
    fun `prefill sed P6000 P_BUC_01 Alderpensjon med avslag skal returnere en gyldig SED`() {

        val person = mockPersonService(FNR_VOKSEN_3, AKTOER_ID, fornavn = "Alder", etternavn = "Pensjonist")
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/vedtak/P6000-AP-Avslag.xml")
        val apijson = dummyApijson( sakid = "22874955", vedtakid = "123123423423", aktoerId = AKTOER_ID, sedType = P6000, buc = P_BUC_01)

        val result = mockMvcPrefill(apijson)

        val response = result.response.getContentAsString(charset("UTF-8"))

        val validResponse = SedBuilder.ValidResponseBuilder().apply {
            sed = P6000
            pensjon = SedBuilder.P6000PensjonBuilder().build()
            nav {
                bruker {
                    person {
                        fornavn = "Alder"
                        etternavn = "Pensjonist"
                    }
                }
            }
        }.build().toJsonSkipEmpty()
        JSONAssert.assertEquals(response, validResponse, true)
    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P3000_SE Gjenlevende har med avdod skal returnere en gyldig SED`() {

        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentIdent(AKTORID, NorskIdent(FNR_VOKSEN_4))} returns AktoerId(AKTOER_ID_2)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN, AKTOER_ID)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_4)) } returns PersonPDLMock.createWith(true, "Avdød", "Død", FNR_VOKSEN_4, AKTOER_ID_2, true)

        val apijson = dummyApijson(sakid = "22874955", vedtakid = "9876543211", aktoerId = AKTOER_ID, sedType = P3000_SE, buc = P_BUC_10,  fnravdod = FNR_VOKSEN_4)

        val result = mockMvcPrefill(apijson)

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

        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentIdent(AKTORID, NorskIdent(FNR_VOKSEN_4))} returns AktoerId(AKTOER_ID_2)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN, AKTOER_ID)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_4)) } returns PersonPDLMock.createWith(true, "Avdød", "Død", FNR_VOKSEN_4, AKTOER_ID_2, true)

        val apijson = dummyApijson(sakid = "22874955", vedtakid = "9876543211", aktoerId = AKTOER_ID, sedType = P5000, buc = P_BUC_10,  fnravdod = FNR_VOKSEN_4)

        val result = mockMvcPrefill(apijson)

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
        mockPersonService(fnr = FNR_VOKSEN_3, aktoerId = AKTOER_ID, fornavn = "Lever", etternavn = "Gjenlev")
        val apijson = dummyApijson(sakid = "22874955", vedtakid = "9876543211", aktoerId = AKTOER_ID, sedType = P4000, buc = P_BUC_05)

        val result = mockMvcPrefill(apijson)

        val response = result.response.getContentAsString(charset("UTF-8"))
        val validResponse = SedBuilder.ValidResponseBuilder().apply {
            sed = P4000
            nav {
                krav = null
            }
        }.build().toJsonSkipEmpty()

        val mapper = jacksonObjectMapper()
        val sedRootNode = mapper.readTree(response)
        val forsikretPin = finnPin(sedRootNode.at("/nav/bruker"))

        Assertions.assertEquals(FNR_VOKSEN_3, forsikretPin)
        JSONAssert.assertEquals(response, validResponse, true)
    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P2000 alder return valid sedjson`() {
        val person = PersonPDLMock.createWith()
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns person
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2000-AP-UP-21337890.xml")

        val apijson = dummyApijson(sakid = "21337890", aktoerId = AKTOER_ID)
        val validResponse = SedBuilder.ValidResponseBuilder().apply {
            sed = P2000
            pensjon = SedBuilder.P2000PensjonBuilder().apply {
                ytelser = SedBuilder.YtelserBuilder(
                    belop = null
                ).build()
            }.build()
            nav {
                eessisak[0].saksnummer = "21337890"
                bruker {
                    person {
                        fornavn = "Test"
                        etternavn = "Testesen"
                        pinList[0].identifikator = "3123"
                        pinList[1].identifikator = "123123123"
                    }
                }
            }
        }.build().toJsonSkipEmpty()

        val response = prefillFraRestOgVerifiserResultet(apijson)
        JSONAssert.assertEquals(validResponse, response, true)

    }

    @Test
    fun `prefill sed P2000 alder med overgang fra ufore med sakstatus Ukjent return valid sedjson`() {
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2000-AP-UTL-UKJENT-12065212345.xml")

        val apijson = dummyApijson(sakid = "21841174", aktoerId = AKTOER_ID)

        val response = prefillFraRestOgVerifiserResultet(apijson)

        val validResponse = SedBuilder.ValidResponseBuilder().apply {
            sed = P2000
            pensjon = SedBuilder.P2000PensjonBuilder().apply {
                kravDato = Krav("2015-11-25")
                forespurtstartdato = "2016-03-01"
                ytelser = SedBuilder.YtelserBuilder(
                    status = "01",
                    mottasbasertpaa = null,
                    startdatoutbetaling = "2016-03-01",
                    startdatoretttilytelse = "2016-03-01",
                    totalbruttobeloeparbeidsbasert = "9638",
                    totalbruttobeloepbostedsbasert = "4936",
                    belop = SedBuilder.BelopBuilder("14574", Betalingshyppighet.maaned_12_per_aar, "2016-03-01").build()
                ).build()
            } .build()
            nav {
                eessisak[0].saksnummer = "21841174"
                bruker {
                    person {
                        fornavn = "Test"
                        etternavn = "Testesen"
                        pinList[0].identifikator = "3123"
                        pinList[1].identifikator = "123123123"
                    }
                }
                krav = SedBuilder.KravBuilder("2015-11-25")
            }
        }.build().toJsonSkipEmpty()
        JSONAssert.assertEquals(validResponse, response, true)

    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P2000 alder F_BH_KUN_UTL return valid sedjson`() {
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/AP_2000_KUN_UTLAND.xml")

        val apijson = dummyApijson(sakid = "22932784", aktoerId = AKTOER_ID)
        val validResponse = SedBuilder.ValidResponseBuilder().apply {
            sed = P2000
            pensjon = SedBuilder.P2000PensjonBuilder().apply {
                kravDato = Krav("2021-03-01")
                ytelser = SedBuilder.YtelserBuilder(
                    belop = null
                ).build()
            } .build()
            nav {
                eessisak[0].saksnummer = "22932784"
                bruker {
                    person {
                        fornavn = "Test"
                        etternavn = "Testesen"
                        pinList[0].identifikator = "3123"
                        pinList[1].identifikator = "123123123"
                    }
                }
                krav = SedBuilder.KravBuilder("2021-03-01")
            }
        }.build().toJsonSkipEmpty()
        val response = prefillFraRestOgVerifiserResultet(apijson)
        JSONAssert.assertEquals(validResponse, response, true)
    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P2000 alder med AVSL returnerer en valid sedjson`() {

        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2000krav-alderpensjon-avslag.xml")

        val apijson = dummyApijson(sakid = "22889955", aktoerId = AKTOER_ID)
        val response = prefillFraRestOgVerifiserResultet(apijson)
        val validResponse = SedBuilder.ValidResponseBuilder().apply {
            sed = P2000
            pensjon = SedBuilder.P2000PensjonBuilder().apply {
                kravDato = Krav("2019-04-30")
                ytelser = SedBuilder.YtelserBuilder("03", belop = null).build()
            } .build()
            nav {
                eessisak[0].saksnummer = "22889955"
                bruker {
                    person {
                        fornavn = "Test"
                        etternavn = "Testesen"
                        pinList[0].identifikator = "3123"
                        pinList[1].identifikator = "123123123"
                    }
                }
                krav = SedBuilder.KravBuilder("2019-04-30")
            }
        }.build().toJsonSkipEmpty()
        JSONAssert.assertEquals(validResponse, response,true)
    }

    @Test
    fun `prefill sed med kravtype førstehangbehandling norge men med vedtak bodsatt utland skal prefylle sed`() {

        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN_3)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_3)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN_3)
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns
                PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/F_BH_MED_UTL.xml") andThen
                PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/vedtak/P6000-APUtland-301.xml")

        val apijson = dummyApijson(sakid = "22580170", aktoerId = AKTOER_ID, vedtakid = "5134513451345")

        val response = prefillFraRestOgVerifiserResultet(apijson)

        val validResponse = SedBuilder.ValidResponseBuilder().apply {
            sed = P2000
            pensjon = SedBuilder.P2000PensjonBuilder().apply {
                kravDato = Krav("2018-05-31")
                forespurtstartdato = "2018-08-01"
                ytelser = SedBuilder.YtelserBuilder(
                    status = "02",
                    mottasbasertpaa = null,
                    startdatoutbetaling = "2018-08-01",
                    startdatoretttilytelse = "2018-08-01",
                    totalbruttobeloeparbeidsbasert = "14198",
                    totalbruttobeloepbostedsbasert = "7034",
                    belop = SedBuilder.BelopBuilder("21232", Betalingshyppighet.maaned_12_per_aar).build()
                ).build()
            } .build()
            nav {
                eessisak[0].saksnummer = "22580170"
                bruker {
                    person {
                        fornavn = "Lever"
                        etternavn = "Gjenlev"
                        pinList[0].identifikator = "12312312312"
                        pinList[1].identifikator = "123123123"
                    }
                }
                krav = SedBuilder.KravBuilder("2018-05-31")
            }
        }.build().toJsonSkipEmpty()
        JSONAssert.assertEquals(validResponse, response, true)

    }


    /** test på validering av pensjoninformasjon krav **/
    @Test
    fun `prefill sed med kun utland, ikke korrekt sakid skal kaste en Exception`() {
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2000-AP-KUNUTL-IKKEVIRKNINGTID.xml")

        val apijson = dummyApijson(sakid = "1232123123", aktoerId = AKTOER_ID)
        val expectedError = """Det finnes ingen iverksatte vedtak for førstegangsbehandling kun utland, eller sluttbehandling. Vennligst gå til EESSI-Pensjon fra vedtakskontekst.""".trimIndent()

        mockMvcSedPrefill(apijson, expectedError)

    }

    @Test
    fun `prefill sed med kravtype førstehangbehandling skal kaste en Exception`() {

        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/AP_FORSTEG_BH.xml")

        val apijson = dummyApijson(sakid = "22580170", aktoerId = AKTOER_ID)

        mockMvcSedPrefill(apijson, "Det er ikke markert for bodd/arbeidet i utlandet. Krav SED P2000 blir ikke opprettet")

    }

    @Test
    fun `prefill sed med ALDERP uten korrekt kravårsak skal kaste en Exception`() {
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN_3)
        every { personService.hentIdent(AKTORID, NorskIdent(FNR_VOKSEN_4)) } returns AktoerId(AKTOER_ID_2)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_3)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN_3, AKTOER_ID)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_4)) } returns PersonPDLMock.createWith(true, "Avdød", "Død", FNR_VOKSEN_4, AKTOER_ID_2, true)
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2000-AP-LP-RVUR-20541862.xml")

        every { kodeverkClient.finnLandkode(any()) } returns "NO"

        val apijson = dummyApijson(sakid = "20541862", aktoerId = AKTOER_ID, sedType = P2100, buc = P_BUC_02, fnravdod = FNR_VOKSEN_4)

        mockMvcSedPrefill(apijson, "Ingen gyldig kravårsak funnet for ALDER eller UFØREP for utfylling av en krav SED P2100")
    }

    @Test
    fun `prefill sed X010 valid sedjson`() {

        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2000-AP-UP-21337890.xml")

        val apijson = dummyApijson(sakid = "21337890", aktoerId = AKTOER_ID, sedType = X010)

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

        val response = prefillFraRestOgVerifiserResultet(apijson)
        JSONAssert.assertEquals(response, validResponse, false)

    }

    @Test
    fun `prefill sed X010 med json data fra X009 gir en valid X010`() {

        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()
        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2000-AP-UP-21337890.xml")

        val x009 = SED.fromJsonToConcrete(PrefillTestHelper.readJsonResponse("/json/nav/X009-NAV.json")) as X009

        val apijson = dummyApijson(sakid = "21337890", aktoerId = AKTOER_ID, sedType = X010, payload = x009.toJson())

        val validResponse = XSedBuilder.ValidResponseBuilderXSEd().build().toJsonSkipEmpty()
        val response = prefillFraRestOgVerifiserResultet(apijson)
        JSONAssert.assertEquals(response, validResponse, false)

    }

    private fun prefillFraRestOgVerifiserResultet(apijson: String): String {
        val result = mockMvcPrefill(apijson)

        val response = result.response.getContentAsString(charset("UTF-8"))
        return response
    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed med uten korrekt kravtype skal kaste en Exception`() {
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID )) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, fnr = FNR_VOKSEN, aktoerid = AKTOER_ID)
        every { pensjonsinformasjonOidcRestTemplate .exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java))} returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2000-AP-MANGLER_BOSATT_UTLAND.xml")

        val apijson = dummyApijson(sakid = "21920707", aktoerId = AKTOER_ID)

        val melding = "Det finnes ingen iverksatte vedtak for førstegangsbehandling kun utland, eller sluttbehandling. Vennligst gå til EESSI-Pensjon fra vedtakskontekst."
        mockMvcSedPrefill(apijson, melding)
    }

    private val PREFILL_URL = "/sed/prefill"

    private fun performPrefillRequest(apijson: String) =
        mockMvc.perform(
            post(PREFILL_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson)
        )

    private fun mockMvcSedPrefill(apijson: String, melding: String) {
        performPrefillRequest(apijson)
            .andExpect(status().isBadRequest)
            .andExpect(status().reason(Matchers.containsString(melding)))
    }

    private fun mockMvcPrefill(apijson: String): MvcResult =
        performPrefillRequest(apijson)
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

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
    sedType: SedType = P2000,
    buc: BucType? = P_BUC_06,
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
        sed = sedType,
        buc = buc,
        kravType = kravtype,
        kravDato = kravdato,
        euxCaseId = "12345",
        institutions = emptyList(),
        subject = subject,
        payload = payload
    )
}

fun dummyApijson(sakid: String, vedtakid: String? = null, aktoerId: String, sedType: SedType = P2000, buc: BucType? = P_BUC_06, fnravdod: String? = null, kravtype: KravType? = null, kravdato: String? = null, payload: String? = null): String {
    return dummyApi(sakid, vedtakid, aktoerId, sedType, buc, fnravdod, kravtype, kravdato, payload).toJson()
}