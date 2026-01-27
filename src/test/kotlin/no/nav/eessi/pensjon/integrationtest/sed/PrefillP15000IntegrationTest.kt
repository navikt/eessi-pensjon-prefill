package no.nav.eessi.pensjon.integrationtest.sed

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.eux.model.BucType.P_BUC_10
import no.nav.eessi.pensjon.eux.model.SedType.P15000
import no.nav.eessi.pensjon.eux.model.sed.KravType
import no.nav.eessi.pensjon.integrationtest.IntegrasjonsTestConfig
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.kodeverk.Postnummer
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.model.*
import no.nav.eessi.pensjon.prefill.KrrService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.DigitalKontaktinfo
import no.nav.eessi.pensjon.utils.toJson
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.time.LocalDateTime

private const val SAK_ID = "22915555"

private const val FNR_VOKSEN = "11067122781"    // KRAFTIG VEGGPRYD
private const val FNR_VOKSEN_2 = "22117320034"  // LEALAUS KAKE
private const val FNR_VOKSEN_3 = "12312312312"
private const val FNR_VOKSEN_4 = "9876543210"
private const val NPID = "01220049651"

private const val AKTOER_ID = "0123456789000"
private const val AKTOER_ID_2 = "0009876543210"
private const val AKTOER_ID_AVDOD_MOR = "12312312441"
private const val AKTOER_ID_AVDOD_FAR = "3323332333233323"

@SpringBootTest(classes = [IntegrasjonsTestConfig::class, UnsecuredWebMvcTestLauncher::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("unsecured-webmvctest", "excludeKodeverk")
@AutoConfigureMockMvc
@DirtiesContext
@EmbeddedKafka
class PrefillP15000IntegrationTest {

    @MockkBean
    lateinit var pdlRestTemplate: RestTemplate

    @MockkBean
    lateinit var kodeverkClient: KodeverkClient

//    @MockkBean
//    lateinit var pensjoninformasjonservice: PensjonsinformasjonService

    @MockkBean
    lateinit var personService: PersonService

    @MockkBean
    lateinit var krrService: KrrService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        every { kodeverkClient.finnLandkode(any()) } returns "XQ"
//        every { pensjoninformasjonservice.hentRelevantVedtakHvisFunnet(any()) } returns null
        every { kodeverkClient.hentPostSted(any()) } returns Postnummer("1068", "SØRUMSAND")

        every { personService.hentIdent(IdentGruppe.FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN_3)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_3)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN_3, AKTOER_ID)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_4)) } returns PersonPDLMock.createWith(true, "Avdød", "Død", FNR_VOKSEN_4, AKTOER_ID_2, true)
        every { krrService.hentPersonerFraKrr(eq(FNR_VOKSEN_3)) } returns DigitalKontaktinfo(epostadresse = "melleby12@melby.no", reservert = true, mobiltelefonnummer = "11111111", aktiv = true, personident = FNR_VOKSEN_3)
        every { krrService.hentPersonerFraKrr(eq(FNR_VOKSEN_4)) } returns DigitalKontaktinfo(epostadresse = "melleby12@melby.no", mobiltelefonnummer = "11111111", aktiv = true, personident = FNR_VOKSEN_4)
        every { krrService.hentPersonerFraKrr(any()) } returns DigitalKontaktinfo("melleby11@melby.no", true, true, false, "11111111", FNR_VOKSEN_3)

//        val v1Kravhistorikk = V1KravHistorikk()
//        v1Kravhistorikk.kravArsak = KravArsak.GJNL_SKAL_VURD.name

//        val barnepSak = v1Sak(BARNEP.name)
//        barnepSak.kravHistorikkListe = V1KravHistorikkListe()
//        barnepSak.kravHistorikkListe.kravHistorikkListe.add(v1Kravhistorikk)
//
//        every { pensjoninformasjonservice.hentRelevantPensjonSak(any(), any()) } returns barnepSak

    }

    @Test
    @Throws(Exception::class)
    fun `prefill P15000 for bruker med npid i P_BUC_10 fra vedtakskontekst hvor saktype er GJENLEV og pensjoninformasjon gir BARNEP med GJENLEV`() {
        every { personService.hentIdent(IdentGruppe.AKTORID, Npid(NPID)) } returns AktoerId(AKTOER_ID_2)
        every { personService.hentPerson(Npid(NPID)) } returns PersonPDLMock.createWith(true, "Avdød", "Død", NPID, AKTOER_ID_2, true)

//        val avdod = avdod(NPID)

//        val pensjonsinformasjon = Pensjonsinformasjon()
//        pensjonsInformasjon(pensjonsinformasjon)
//        pensjonsinformasjon.avdod = avdod
//        pensjonsinformasjon.vedtak = V1Vedtak()

        val apijson =  dummyApijson(sakid = SAK_ID, vedtakid = "123123123", aktoerId = AKTOER_ID, sedType = P15000, buc = P_BUC_10, kravtype = KravType.GJENLEV, kravdato = "2020-01-01", fnravdod = NPID)

        val result = mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))
        val validResponse = gyldigResponse(NPID, FNR_VOKSEN_3, true)

        JSONAssert.assertEquals(validResponse, response,  true)

    }

    @Test
    @Throws(Exception::class)
    fun `prefill P15000 P_BUC_10 fra vedtakskontekst hvor saktype er GJENLEV og pensjoninformasjon gir BARNEP med GJENLEV`() {
        every { personService.hentIdent(IdentGruppe.AKTORID, NorskIdent(FNR_VOKSEN_4)) } returns AktoerId(AKTOER_ID_2)

//        val avdod = avdod(FNR_VOKSEN_4)

//        val pensjonsinformasjon = Pensjonsinformasjon()
//        pensjonsInformasjon(pensjonsinformasjon)
//        pensjonsinformasjon.avdod = avdod
//        pensjonsinformasjon.vedtak = V1Vedtak()

        val apijson =  dummyApijson(sakid = SAK_ID, vedtakid = "123123123", aktoerId = AKTOER_ID, sedType = P15000, buc = P_BUC_10, kravtype = KravType.GJENLEV, kravdato = "2020-01-01", fnravdod = FNR_VOKSEN_4)

        val result = mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))
        val validResponse = gyldigResponse(FNR_VOKSEN_4, FNR_VOKSEN_3, true)

        assertTrue(response.contains("kontakt"))
        JSONAssert.assertEquals(validResponse, response, true)

    }

    @Test
    @Throws(Exception::class)
    fun `prefill P15000 P_BUC_10 fra vedtakskontekst hvor saktype er GJENLEV og pensjoninformasjon gir BARNEP med GJENLEV men kontakt fylles ikke ut siden krr har registrert reservasjon`() {
        every { krrService.hentPersonerFraKrr(eq(FNR_VOKSEN_3)) } returns DigitalKontaktinfo(
            epostadresse = "melleby12@melby.no", mobiltelefonnummer = "11111111", aktiv = true, personident = FNR_VOKSEN_4, reservert = true
        )
        every { personService.hentIdent(IdentGruppe.AKTORID, NorskIdent(FNR_VOKSEN_4)) } returns AktoerId(AKTOER_ID_2)
        every { personService.hentIdent(IdentGruppe.AKTORID, NorskIdent(FNR_VOKSEN_4)) } returns AktoerId(AKTOER_ID_2)

//        val avdod = avdod(FNR_VOKSEN_4)

//        val pensjonsinformasjon = Pensjonsinformasjon()
//        pensjonsInformasjon(pensjonsinformasjon)
//        pensjonsinformasjon.avdod = avdod
//        pensjonsinformasjon.vedtak = V1Vedtak()

        val apijson =  dummyApijson(sakid = SAK_ID, vedtakid = "123123123", aktoerId = AKTOER_ID, sedType = P15000, buc = P_BUC_10, kravtype = KravType.GJENLEV, kravdato = "2020-01-01", fnravdod = FNR_VOKSEN_4)

        val result = mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        assertTrue(response.toJson().contains("adresse"))
        assertFalse(response.toJson().contains("kontakt"))
        assertFalse(response.contains("epost"))

    }

    @Test
    @Throws(Exception::class)
    fun `prefill P15000 P_BUC_10 fra vedtakskontekst hvor saktype er ALDER og pensjoninformasjon returnerer ALDER med GJENLEV`() {
        every { personService.hentIdent(IdentGruppe.AKTORID, NorskIdent(FNR_VOKSEN_4)) } returns AktoerId(AKTOER_ID_2)
//        every { pensjoninformasjonservice.hentMedVedtak(any())} returns Pensjonsinformasjon()

//        v1Sak(ALDER.toString())

        val apijson =  dummyApijson(sakid = SAK_ID, vedtakid = "123123123", aktoerId = AKTOER_ID, sedType = P15000, buc = P_BUC_10, kravtype = KravType.ALDER, kravdato = "2020-01-01", fnravdod = FNR_VOKSEN_4)

        val result = mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val validResponse = """
            {
              "sed" : "P15000",
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
                      "identifikator" : "$FNR_VOKSEN_3",
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
                    "land" : "NO"
                  }
                },
                "krav" : {
                  "dato" : "2020-01-01",
                  "type" : "01"
                }
              },
              "pensjon" : { },
              "sedGVer" : "4",
              "sedVer" : "2"
            }
        """.trimIndent()

        JSONAssert.assertEquals(validResponse, response, true)
    }

    @Test
    @Throws(Exception::class)
    fun `prefill P15000 P_BUC_10 hvor saktype er ALDER men data fra pensjonsinformasjon gir UFOREP som resulterer i en bad request`() {
        every { personService.hentIdent(IdentGruppe.FOLKEREGISTERIDENT, AktoerId(AKTOER_ID )) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, fnr = FNR_VOKSEN, aktoerid = AKTOER_ID)

//        v1Sak(UFOREP.name)

//        val pensjonsinformasjon = Pensjonsinformasjon()
//        pensjonsInformasjon(pensjonsinformasjon)

        val apijson = dummyApijson(sakid = "22874955", vedtakid = "123123123", aktoerId = AKTOER_ID, sedType = P15000, buc = P_BUC_10, kravtype = KravType.ALDER, kravdato = "2020 -01-01")

        val result = mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isBadRequest)
            .andExpect(status().reason(Matchers.containsString("Du kan ikke opprette alderspensjonskrav i en uføretrygdsak (PESYS-saksnr: 22874955 har sakstype UFOREP)")))

    }

    @Test
    @Throws(Exception::class)
    fun `prefill P15000 P_BUC_10 hvor saktype er UFOREP men data fra pensjonsinformasjon gir ALDER som resulterer i en bad request`() {
        every { personService.hentIdent(IdentGruppe.FOLKEREGISTERIDENT, AktoerId(AKTOER_ID )) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, fnr = FNR_VOKSEN, aktoerid = AKTOER_ID)

//        v1Sak(ALDER.name)
//
//        val pensjonsinformasjon = Pensjonsinformasjon()
//        pensjonsInformasjon(pensjonsinformasjon)

        val apijson = dummyApijson(sakid = "21337890", vedtakid = "123123123" , aktoerId = AKTOER_ID, sedType = P15000, buc = P_BUC_10, kravtype = KravType.UFOREP, kravdato = "01-01-2020")

        mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isBadRequest)
            .andExpect(status().reason(Matchers.containsString("Du kan ikke opprette uføretrygdkrav i en alderspensjonssak (PESYS-saksnr: 21337890 har sakstype ALDER)")))
    }

    @Test
    @Throws(Exception::class)
    fun `prefill P15000 P_BUC_10 hvor saktype er ALDER`() {

        every { personService.hentIdent(IdentGruppe.FOLKEREGISTERIDENT, AktoerId(AKTOER_ID )) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", fnr = FNR_VOKSEN, aktoerid = AKTOER_ID)

//        v1Sak(ALDER.name)
//
//        val pensjonsinformasjon = Pensjonsinformasjon()
//        pensjonsInformasjon(pensjonsinformasjon)

        val apijson = dummyApijson(sakid = "21337890", vedtakid = "123123123" , aktoerId = AKTOER_ID, sedType = P15000, buc = P_BUC_10, kravtype = KravType.ALDER, kravdato = "2020-01-01")

        mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()
    }

    @Test
    @Throws(Exception::class)
    fun `prefill P15000 P_BUC_10 hvor saktype er ALDER med feil dato`() {

        every {personService.hentIdent(IdentGruppe.FOLKEREGISTERIDENT, AktoerId(AKTOER_ID ))  } returns (NorskIdent(FNR_VOKSEN))
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", fnr = FNR_VOKSEN, aktoerid = AKTOER_ID)

//        v1Sak(ALDER.name)
//
//        val pensjonsinformasjon = Pensjonsinformasjon()
//        pensjonsInformasjon(pensjonsinformasjon)

        val apijson = dummyApijson(sakid = "21337890", vedtakid = "123123123" , aktoerId = AKTOER_ID, sedType = P15000, buc = P_BUC_10, kravtype = KravType.ALDER, kravdato = "01-01- 2020")
        val expectedError = "Ugyldig datoformat"

        mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isBadRequest)
            .andExpect(status().reason(Matchers.containsString(expectedError)))
    }

    @Test
    @Throws(Exception::class)
    fun `prefill P15000 P_BUC_10 hvor saktype er UFOREP`() {
        every { personService.hentIdent(IdentGruppe.FOLKEREGISTERIDENT, AktoerId(AKTOER_ID )) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", fnr = FNR_VOKSEN, aktoerid = AKTOER_ID)

//        v1Sak(UFOREP.name)
//
//        val pensjonsinformasjon = Pensjonsinformasjon()
//        pensjonsInformasjon(pensjonsinformasjon)

        val apijson = dummyApijson(
            sakid = "22874955", vedtakid = "123123123" ,
            aktoerId = AKTOER_ID, sedType = P15000, buc = P_BUC_10, kravtype = KravType.UFOREP, kravdato = "2020-01-01")

        mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()
    }

    @Test
    @Throws(Exception::class)
    fun `prefill P15000 P_BUC_10 hvor saktype er GJENLEV og pensjoninformasjon gir UFOREP med GJENLEV`() {
        every { personService.hentIdent(IdentGruppe.FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentIdent(IdentGruppe.AKTORID, NorskIdent(FNR_VOKSEN_2)) } returns AktoerId(AKTOER_ID_2)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN, AKTOER_ID)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_2)) } returns PersonPDLMock.createWith(true, "Avdød", "Død", FNR_VOKSEN_2, AKTOER_ID_2, true)

//        val aldersak = v1Sak(UFOREP.name)
//
//        every { pensjoninformasjonservice.hentRelevantPensjonSak(any(), any()) } returns aldersak
//
//        val pensjonsinformasjon = Pensjonsinformasjon()
//        pensjonsInformasjon(pensjonsinformasjon)

        val apijson = dummyApijson(sakid = SAK_ID, vedtakid = "123123123", aktoerId = AKTOER_ID, sedType = P15000, buc = P_BUC_10, kravtype = KravType.GJENLEV, kravdato = "2020-01-01", fnravdod = FNR_VOKSEN_2)

        val result = mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))
        val validResponse = gyldigResponse(FNR_VOKSEN_2, FNR_VOKSEN, false)

        JSONAssert.assertEquals(validResponse, response,  true)

    }

    @Test
    @Throws(Exception::class)
    fun `prefill P15000 P_BUC_10 hvor saktype er GJENLEV og pensjoninformasjon gir BARNEP med GJENLEV`() {
        every { personService.hentIdent(IdentGruppe.FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentIdent(IdentGruppe.AKTORID, NorskIdent(FNR_VOKSEN_2)) } returns AktoerId(AKTOER_ID_2)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN, AKTOER_ID)

        val avdodperson = PersonPDLMock.createWith(true, "Avdød", "Død", FNR_VOKSEN_2, AKTOER_ID_2, true)
            .copy(bostedsadresse = null, oppholdsadresse = null, kontaktadresse = null, kontaktinformasjonForDoedsbo = KontaktinformasjonForDoedsbo(
                personSomKontakt = KontaktinformasjonForDoedsboPersonSomKontakt(
                    personnavn = Personnavn(fornavn = "Fru", etternavn = "Pettimeter")
                ),
                adresse = KontaktinformasjonForDoedsboAdresse(
                    "dødsboadresse1",
                    "adresselinje2",
                    "SWE",
                    "2312",
                    "Langegatan 121"
                ),
                attestutstedelsesdato = LocalDate.of(2010, 10,1 ),
                folkeregistermetadata = Folkeregistermetadata(LocalDateTime.of(2010, 10, 1, 10, 1, 2)),
                metadata = Metadata(
                    emptyList(),
                    false,
                    "DOLLY",
                    "123123-123123-12--312312-312-31-23-123-1-31-23-123-12-31-23-123-12-3-123"
                ),
                skifteform = KontaktinformasjonForDoedsboSkifteform.ANNET
            ))
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_2)) } returns avdodperson

//        v1Sak(UFOREP.name)

//        val avdod = V1Avdod()
//        avdod.avdodFar = FNR_VOKSEN_2
//        avdod.avdodFarAktorId = AKTOER_ID_2
//        avdod.avdodMor = AKTOER_ID_AVDOD_MOR

//        val pensjonsinformasjon = Pensjonsinformasjon()
//        pensjonsInformasjon(pensjonsinformasjon)
//        pensjonsinformasjon.vedtak = V1Vedtak()
//        pensjonsinformasjon.vedtak.vedtakStatus = "INNV"
//        pensjonsinformasjon.avdod = avdod

        every { kodeverkClient.finnLandkode("SWE") } returns "SE"

        val apijson = dummyApijson(sakid = SAK_ID, vedtakid = "123123123", aktoerId = AKTOER_ID, sedType = P15000, buc = P_BUC_10, kravtype = KravType.GJENLEV, kravdato = "2020-01-01", fnravdod = FNR_VOKSEN_2)

        val result = mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()

        val response = result.response.getContentAsString(charset("UTF-8"))

        val validResponse = """
            {
              "sed" : "P15000",
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
                      "identifikator" : "$FNR_VOKSEN_2",
                      "land" : "NO"
                    } ],
                    "etternavn" : "Død",
                    "fornavn" : "Avdød",
                    "kjoenn" : "M",
                    "foedselsdato" : "1921-07-12"
                  },
                  "adresse" : {
                    "gate" : "Dødsbo v/Fru Pettimeter, dødsboadresse1",
                    "bygning" : "adresselinje2",
                    "by" : "Langegatan 121",
                    "postnummer" : "2312",
                    "land" : "SE"
                  }
                },
                "krav" : {
                  "dato" : "2020-01-01",
                  "type" : "02"
                }
              },
              "pensjon" : {
                "gjenlevende" : {
                  "person" : {
                    "pin" : [ {
                      "institusjonsnavn" : "NOINST002, NO INST002, NO",
                      "institusjonsid" : "NO:noinst002",
                      "identifikator" : "$FNR_VOKSEN",
                      "land" : "NO"
                    }, {
                      "identifikator" : "123123123",
                      "land" : "SE"
                    } ],
                    "statsborgerskap" : [ {
                      "land" : "XQ"
                    }, {
                      "land" : "SE"
                    } ],
                    "etternavn" : "Gjenlev",
                    "fornavn" : "Lever",
                    "kjoenn" : "M",
                    "foedselsdato" : "1988-07-12",
                    "sivilstand" : [ {
                      "fradato" : "2000-10-01",
                      "status" : "enslig"
                    } ],
                    "relasjontilavdod" : {
                      "relasjon" : "06"
                    },
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
              "sedGVer" : "4",
              "sedVer" : "2"
            }
        """.trimIndent()

        JSONAssert.assertEquals(validResponse, response, true)
    }

    private fun gyldigResponse(bruker: String, gjenlevendeFnr: String, fyllesUt: Boolean): String = """
            {
              "sed" : "P15000",
              "sedGVer" : "4",
              "sedVer" : "2",
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
                      "identifikator" : "$bruker",
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
                "krav" : {
                  "dato" : "2020-01-01",
                  "type" : "02"
                }
              },
              "pensjon" : {
                "gjenlevende" : {
                  "person" : {
                    "pin" : [ {
                      "institusjonsnavn" : "NOINST002, NO INST002, NO",
                      "institusjonsid" : "NO:noinst002",
                      "identifikator" : "$gjenlevendeFnr",
                      "land" : "NO"
                    }, {
                      "identifikator" : "123123123",
                      "land" : "XQ"
                    } ],
                    "statsborgerskap" : [ {
                      "land" : "XQ"
                    } ],
                    "etternavn" : "Gjenlev",
                    "fornavn" : "Lever",
                    "kjoenn" : "M",
                    "foedselsdato" : "1988-07-12",
                    "sivilstand" : [ {
                      "fradato" : "2000-10-01",
                      "status" : "enslig"
                    } ],
                    ${if (fyllesUt) relasjonTilAvdod() else ""}
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
              "sedGVer" : "4",
              "sedVer" : "2"
            }
            """.trimIndent()

//    private fun avdod(fnr: String): V1Avdod {
//        val avdod = V1Avdod()
//        avdod.avdodFar = fnr
//        avdod.avdodFarAktorId = AKTOER_ID_AVDOD_FAR
//        avdod.avdodMor = AKTOER_ID_AVDOD_MOR
//        return avdod
//    }

//    private fun pensjonsInformasjon(pensjonsinformasjon: Pensjonsinformasjon) : Pensjonsinformasjon {
//        pensjonsinformasjon.vedtak = V1Vedtak()
//        pensjonsinformasjon.vedtak.vedtakStatus = "INNV"
//
//        every { pensjoninformasjonservice.hentMedVedtak("123123123") } returns pensjonsinformasjon
//        return pensjonsinformasjon
//    }

//    private fun v1Sak(sakType: String): V1Sak {
//        val sak = V1Sak()
//        sak.sakType = sakType
//        sak.sakId = 22915555L
//        sak.status = "INNV"
//
//        every { pensjoninformasjonservice.hentRelevantPensjonSak(any(), any()) } returns sak
//        return sak
//    }

    private fun relasjonTilAvdod(): String = """"relasjontilavdod" : { "relasjon" : "06" },""" }

