package no.nav.eessi.pensjon.integrationtest.sed

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.eux.model.BucType.P_BUC_03
import no.nav.eessi.pensjon.eux.model.BucType.P_BUC_06
import no.nav.eessi.pensjon.eux.model.SedType.P2000
import no.nav.eessi.pensjon.eux.model.SedType.P2200
import no.nav.eessi.pensjon.integrationtest.IntegrasjonsTestConfig
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.pensjonsinformasjon.models.EPSaktype.UFOREP
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.model.AktoerId
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentGruppe.FOLKEREGISTERIDENT
import no.nav.eessi.pensjon.personoppslag.pdl.model.NorskIdent
import no.nav.eessi.pensjon.personoppslag.pdl.model.Npid
import no.nav.eessi.pensjon.prefill.PensjonsinformasjonService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.pensjon.v1.kravhistorikk.V1KravHistorikk
import no.nav.pensjon.v1.kravhistorikkliste.V1KravHistorikkListe
import no.nav.pensjon.v1.sak.V1Sak
import no.nav.pensjon.v1.vedtak.V1Vedtak
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.web.client.RestTemplate

@SpringBootTest(classes = [IntegrasjonsTestConfig::class, UnsecuredWebMvcTestLauncher::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("unsecured-webmvctest", "excludeKodeverk")
@AutoConfigureMockMvc
@EmbeddedKafka
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PrefillErrorIntegrationTest {

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
        const val NPID_VOKSEN = "01220049651"
        const val AKTOER_ID = "0123456789000"
    }

    @Test
    fun `prefill sed P2200 som har vedtak, F_BH_BO_UTL men mangler F_BH_MED_UTL i tillegg til at isBoddArbeidetUtland er false så skal det kastes en Exception`() {

        every { kodeverkClient.finnLandkode(eq("NOR"))} returns "NO"
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()

        val sak = V1Sak()
        sak.sakType = UFOREP.toString()
        sak.sakId = 100
        sak.kravHistorikkListe = V1KravHistorikkListe()
        val krav = V1KravHistorikk()
        krav.kravType = "REVURD"
        krav.kravId = "1"
        krav.status = "INNV"
        sak.kravHistorikkListe.kravHistorikkListe.add(krav)

        every { pensjoninformasjonservice.hentRelevantPensjonSak(any(), any()) } returns sak

        val vedtak = V1Vedtak()
        vedtak.isBoddArbeidetUtland = false
        vedtak.kravGjelder = "REVURD"

        every { pensjoninformasjonservice.hentRelevantVedtakHvisFunnet("231231231") } returns vedtak

        val apijson = dummyApijson(sakid = "1232123123", aktoerId = AKTOER_ID, vedtakid = "231231231", sed = P2200.name,  buc = P_BUC_03.name)
        val expectedError = """Du kan ikke opprette krav-SED P2200 hvis ikke "bodd/arbeidet i utlandet" er krysset av""".trimIndent()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.status().reason(Matchers.containsString(expectedError)))

    }

    @Test
    fun `Prefill P2200 som har vedtak med F_BH_BO_UTL men F_BH_MED_UTL mangler i tillegg til at vedtak isBoddArbeidetUtland er false så skal det kastes en Exception`() {

        every { kodeverkClient.finnLandkode(eq("NOR"))} returns "NO"
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns Npid(NPID_VOKSEN)
        every { personService.hentPerson(Npid(NPID_VOKSEN)) } returns PersonPDLMock.createWith()

        val sak = V1Sak()
        sak.sakType = UFOREP.toString()
        sak.sakId = 100
        sak.kravHistorikkListe = V1KravHistorikkListe()
        val krav = V1KravHistorikk()
        krav.kravType = "REVURD"
        krav.kravId = "1"
        krav.status = "INNV"
        sak.kravHistorikkListe.kravHistorikkListe.add(krav)

        every { pensjoninformasjonservice.hentRelevantPensjonSak(any(), any()) } returns sak

        val vedtak = V1Vedtak()
        vedtak.isBoddArbeidetUtland = false
        vedtak.kravGjelder = "REVURD"

        every { pensjoninformasjonservice.hentRelevantVedtakHvisFunnet("231231231") } returns vedtak

        val apijson = dummyApijson(sakid = "1232123123", aktoerId = AKTOER_ID, vedtakid = "231231231", sed = P2200.name,  buc = P_BUC_03.name)
        val expectedError = """Du kan ikke opprette krav-SED P2200 hvis ikke "bodd/arbeidet i utlandet" er krysset av""".trimIndent()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/sed/prefill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(apijson))
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.status().reason(Matchers.containsString(expectedError)))

    }

    private fun dummyApijson(sakid: String, vedtakid: String? = "", aktoerId: String, sed: String? = P2000.name, buc: String? = P_BUC_06.name, subject: String? = null, refperson: String? = null): String {
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

}