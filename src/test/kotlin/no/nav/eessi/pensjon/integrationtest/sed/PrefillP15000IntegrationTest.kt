package no.nav.eessi.pensjon.integrationtest.sed

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.eux.model.BucType.*
import no.nav.eessi.pensjon.eux.model.SedType.*
import no.nav.eessi.pensjon.eux.model.sed.KravType
import no.nav.eessi.pensjon.integrationtest.IntegrasjonsTestConfig
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.pensjonsinformasjon.models.EPSaktype.*
import no.nav.eessi.pensjon.pensjonsinformasjon.models.KravArsak
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.model.*
import no.nav.eessi.pensjon.prefill.PensjonsinformasjonService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
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
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest(classes = [IntegrasjonsTestConfig::class, UnsecuredWebMvcTestLauncher::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("unsecured-webmvctest", "excludeKodeverk")
@AutoConfigureMockMvc
@EmbeddedKafka
class PrefillP15000IntegrationTest {

    @MockkBean
    lateinit var pdlRestTemplate: RestTemplate

    @MockkBean
    lateinit var kodeverkClient: KodeverkClient

    @MockkBean
    lateinit var pensjoninformasjonservice: PensjonsinformasjonService

    @MockkBean
    lateinit var personService: PersonService

    @Autowired
    private lateinit var mockMvc: MockMvc

    private companion object {
        const val FNR_VOKSEN = "11067122781"    // KRAFTIG VEGGPRYD
        const val FNR_VOKSEN_2 = "22117320034"  // LEALAUS KAKE
        const val FNR_VOKSEN_3 = "12312312312"
        const val FNR_VOKSEN_4 = "9876543210"
        const val NPID = "01220049651"

        const val AKTOER_ID = "0123456789000"
        const val AKTOER_ID_2 = "0009876543210"
    }

    @Test
    @Throws(Exception::class)
    fun `prefill P15000 for bruker med npid i P_BUC_10 fra vedtakskontekst hvor saktype er GJENLEV og pensjoninformasjon gir BARNEP med GJENLEV`() {

        every { personService.hentIdent(IdentGruppe.FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN_3)
        every { personService.hentIdent(IdentGruppe.AKTORID, Npid(NPID)) } returns AktoerId(AKTOER_ID_2)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_3)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN_3, AKTOER_ID)
        every { personService.hentPerson(Npid(NPID)) } returns PersonPDLMock.createWith(true, "Avdød", "Død", NPID, AKTOER_ID_2, true)

        val banrepSak = V1Sak()
        banrepSak.sakType = "BARNEP"
        banrepSak.sakId = 22915555L
        banrepSak.status = "INNV"

        every { pensjoninformasjonservice.hentRelevantVedtakHvisFunnet(any()) } returns null
        every { pensjoninformasjonservice.hentRelevantPensjonSak(any(), any()) } returns banrepSak

        val pensjonsinformasjon = Pensjonsinformasjon()
        val avdod = V1Avdod()
        avdod.avdodFar = NPID
        avdod.avdodFarAktorId = "3323332333233323"
        avdod.avdodMor = "12312312441"
        avdod.avdodMorAktorId = "123343242034739845719384257134513"

        pensjonsinformasjon.avdod = avdod
        pensjonsinformasjon.vedtak = V1Vedtak()

        val v1Kravhistorikk = V1KravHistorikk()
        v1Kravhistorikk.kravArsak = KravArsak.GJNL_SKAL_VURD.name

        val sak = V1Sak()
        sak.sakType = BARNEP.toString()
        sak.sakId = 100
        sak.kravHistorikkListe = V1KravHistorikkListe()
        sak.kravHistorikkListe.kravHistorikkListe.add(v1Kravhistorikk)


        every { pensjoninformasjonservice.hentMedVedtak("123123123") } returns pensjonsinformasjon
        every { kodeverkClient.finnLandkode(any()) } returns "XQ"

        val apijson =  dummyApijson(sakid = "22915555", vedtakid = "123123123", aktoerId = AKTOER_ID, sedType = P15000, buc = P_BUC_10, kravtype = KravType.GJENLEV, kravdato = "2020-01-01", fnravdod = NPID)

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
                  "identifikator" : "$NPID",
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
                "land" : "NO"
              }
            }
          }
        }
        """.trimIndent()

        JSONAssert.assertEquals(response, validResponse, true)

    }

    @Test
    @Throws(Exception::class)
    fun `prefill P15000 P_BUC_10 fra vedtakskontekst hvor saktype er GJENLEV og pensjoninformasjon gir BARNEP med GJENLEV`() {

        every { personService.hentIdent(IdentGruppe.FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN_3)
        every { personService.hentIdent(IdentGruppe.AKTORID, NorskIdent(FNR_VOKSEN_4)) } returns AktoerId(AKTOER_ID_2)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_3)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN_3, AKTOER_ID)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_4)) } returns PersonPDLMock.createWith(true, "Avdød", "Død", FNR_VOKSEN_4, AKTOER_ID_2, true)

        val banrepSak = V1Sak()
        banrepSak.sakType = "BARNEP"
        banrepSak.sakId = 22915555L
        banrepSak.status = "INNV"

        every { pensjoninformasjonservice.hentRelevantVedtakHvisFunnet(any()) } returns null
        every { pensjoninformasjonservice.hentRelevantPensjonSak(any(), any()) } returns banrepSak

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
        sak.sakType = BARNEP.toString()
        sak.sakId = 100
        sak.kravHistorikkListe = V1KravHistorikkListe()
        sak.kravHistorikkListe.kravHistorikkListe.add(v1Kravhistorikk)


        every { pensjoninformasjonservice.hentMedVedtak("123123123") } returns pensjonsinformasjon
        every { kodeverkClient.finnLandkode(any()) } returns "XQ"

        val apijson =  dummyApijson(sakid = "22915555", vedtakid = "123123123", aktoerId = AKTOER_ID, sedType = P15000, buc = P_BUC_10, kravtype = KravType.GJENLEV, kravdato = "2020-01-01", fnravdod = "9876543210")

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
                "land" : "NO"
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

        every { personService.hentIdent(IdentGruppe.FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN_3)
        every { personService.hentIdent(IdentGruppe.AKTORID, NorskIdent(FNR_VOKSEN_4)) } returns AktoerId(AKTOER_ID_2)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_3)) } returns PersonPDLMock.createWith(true, "Lever", "Gjenlev", FNR_VOKSEN_3, AKTOER_ID)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_4)) } returns PersonPDLMock.createWith(true, "Avdød", "Død", FNR_VOKSEN_4, AKTOER_ID_2, true)

        val aldersak = V1Sak()
        aldersak.sakType = ALDER.name
        aldersak.sakId = 22915555L
        aldersak.status = "INNV"

        every { pensjoninformasjonservice.hentRelevantPensjonSak(any(), any()) } returns aldersak
        val pensjonsinformasjon = Pensjonsinformasjon()
        val avdod = V1Avdod()
        avdod.avdod = "9876543210"
        avdod.avdodAktorId = "3323332333233323"

        pensjonsinformasjon.avdod = avdod
        pensjonsinformasjon.vedtak = V1Vedtak()

        val v1Kravhistorikk = V1KravHistorikk()
        v1Kravhistorikk.kravArsak = KravArsak.GJNL_SKAL_VURD.name

        val sak = V1Sak()
        sak.sakType = ALDER.toString()
        sak.sakId = 100
        sak.kravHistorikkListe = V1KravHistorikkListe()
        sak.kravHistorikkListe.kravHistorikkListe.add(v1Kravhistorikk)


        every { pensjoninformasjonservice.hentMedVedtak("123123123") } returns pensjonsinformasjon
        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val apijson =  dummyApijson(sakid = "22915555", vedtakid = "123123123", aktoerId = AKTOER_ID, sedType = P15000, buc = P_BUC_10, kravtype = KravType.ALDER, kravdato = "2020-01-01", fnravdod = FNR_VOKSEN_4)

        mockMvc.perform(post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andReturn()
    }

    @Test
    @Throws(Exception::class)
    fun `prefill P15000 P_BUC_10 hvor saktype er ALDER men data fra pensjonsinformasjon gir UFOREP som resulterer i en bad request`() {

        every { personService.hentIdent(IdentGruppe.FOLKEREGISTERIDENT, AktoerId(AKTOER_ID )) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(true, fnr = FNR_VOKSEN, aktoerid = AKTOER_ID)

        val aldersak = V1Sak()
        aldersak.sakType = UFOREP.name
        aldersak.sakId = 22874955
        aldersak.status = "INNV"

        every {pensjoninformasjonservice.hentRelevantPensjonSak(any(), any())  } returns aldersak

        val pensjonsinformasjon = Pensjonsinformasjon()
        pensjonsinformasjon.vedtak = V1Vedtak()
        pensjonsinformasjon.vedtak.vedtakStatus = "INNV"

        every { pensjoninformasjonservice.hentMedVedtak("123123123") } returns pensjonsinformasjon

        val apijson = dummyApijson(sakid = "22874955", vedtakid = "123123123", aktoerId = AKTOER_ID, sedType = P15000, buc = P_BUC_10, kravtype = KravType.ALDER, kravdato = "2020 -01-01")

        mockMvc.perform(post("/sed/prefill")
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

        val aldersak = V1Sak()
        aldersak.sakType = ALDER.name
        aldersak.sakId = 21337890
        aldersak.status = "INNV"

        every {pensjoninformasjonservice.hentRelevantPensjonSak(any(), any()) } returns aldersak

        val pensjonsinformasjon = Pensjonsinformasjon()
        pensjonsinformasjon.vedtak = V1Vedtak()
        pensjonsinformasjon.vedtak.vedtakStatus = "INNV"

        every { pensjoninformasjonservice.hentMedVedtak("123123123") } returns pensjonsinformasjon

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

        val aldersak = V1Sak()
        aldersak.sakType = ALDER.name
        aldersak.sakId = 21337890
        aldersak.status = "INNV"

        every { pensjoninformasjonservice.hentRelevantPensjonSak(any(), any()) } returns aldersak

        val pensjonsinformasjon = Pensjonsinformasjon()
        pensjonsinformasjon.vedtak = V1Vedtak()
        pensjonsinformasjon.vedtak.vedtakStatus = "INNV"

        every { pensjoninformasjonservice.hentMedVedtak("123123123") } returns pensjonsinformasjon
        every { kodeverkClient.finnLandkode(any()) } returns "QX"

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

        val aldersak = V1Sak()
        aldersak.sakType = ALDER.name
        aldersak.sakId = 21337890
        aldersak.status = "INNV"

        every { pensjoninformasjonservice.hentRelevantPensjonSak(any(), any()) } returns aldersak

        val pensjonsinformasjon = Pensjonsinformasjon()
        pensjonsinformasjon.vedtak = V1Vedtak()
        pensjonsinformasjon.vedtak.vedtakStatus = "INNV"

        every { pensjoninformasjonservice.hentMedVedtak("123123123") } returns pensjonsinformasjon
        every {kodeverkClient.finnLandkode(any())  } returns "QX"

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


        val aldersak = V1Sak()
        aldersak.sakType = UFOREP.name
        aldersak.sakId = 22874955
        aldersak.status = "INNV"

        every { pensjoninformasjonservice.hentRelevantPensjonSak(any(), any()) } returns aldersak

        val pensjonsinformasjon = Pensjonsinformasjon()
        pensjonsinformasjon.vedtak = V1Vedtak()
        pensjonsinformasjon.vedtak.vedtakStatus = "INNV"

        every { pensjoninformasjonservice.hentMedVedtak("123123123") } returns pensjonsinformasjon
        every { kodeverkClient.finnLandkode(any()) } returns "QX"

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

        val aldersak = V1Sak()
        aldersak.sakType = UFOREP.name
        aldersak.sakId = 22915550
        aldersak.status = "INNV"

        every { pensjoninformasjonservice.hentRelevantPensjonSak(any(), any()) } returns aldersak
        val pensjonsinformasjon = Pensjonsinformasjon()
        pensjonsinformasjon.vedtak = V1Vedtak()
        pensjonsinformasjon.vedtak.vedtakStatus = "INNV"

        every { pensjoninformasjonservice.hentMedVedtak("123123123") } returns pensjonsinformasjon
        every { kodeverkClient.finnLandkode(any()) } returns "XQ"

        val apijson = dummyApijson(sakid = "22915550", vedtakid = "123123123", aktoerId = AKTOER_ID, sedType = P15000, buc = P_BUC_10, kravtype = KravType.GJENLEV, kravdato = "2020-01-01", fnravdod = FNR_VOKSEN_2)

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
              "sedGVer" : "4",
              "sedVer" : "2",
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
                      "identifikator" : "$FNR_VOKSEN_2",
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
                      "identifikator" : "$FNR_VOKSEN",
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
                    "land" : "NO"
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

        val aldersak = V1Sak()
        aldersak.sakType = UFOREP.name
        aldersak.sakId = 22915555
        aldersak.status = "INNV"

        every { pensjoninformasjonservice.hentRelevantPensjonSak(any(), any()) } returns aldersak

        val pensjonsinformasjon = Pensjonsinformasjon()
        pensjonsinformasjon.vedtak = V1Vedtak()
        pensjonsinformasjon.vedtak.vedtakStatus = "INNV"

        val avdod = V1Avdod()
        avdod.avdodFar = FNR_VOKSEN_2
        avdod.avdodFarAktorId = AKTOER_ID_2
        avdod.avdodMor = "12312312441"
        avdod.avdodMorAktorId = "123343242034739845719384257134513"
        pensjonsinformasjon.avdod = avdod

        every { pensjoninformasjonservice.hentMedVedtak("123123123") } returns pensjonsinformasjon
        every { kodeverkClient.finnLandkode(any()) } returns "XQ"
        every { kodeverkClient.finnLandkode("SWE") } returns "SE"

        val apijson = dummyApijson(sakid = "22915555", vedtakid = "123123123", aktoerId = AKTOER_ID, sedType = P15000, buc = P_BUC_10, kravtype = KravType.GJENLEV, kravdato = "2020-01-01", fnravdod = FNR_VOKSEN_2)

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
                    "land": "SE"
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
                    "land" : "NO"
                  }
                }
              }
            }
        """.trimIndent()

        JSONAssert.assertEquals(validResponse,response, true)
    }
}

