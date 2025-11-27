package no.nav.eessi.pensjon.integrationtest.sed

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.MockkBeans
import io.mockk.every
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.eux.model.BucType.P_BUC_03
import no.nav.eessi.pensjon.eux.model.BucType.P_BUC_06
import no.nav.eessi.pensjon.eux.model.SedType.P2000
import no.nav.eessi.pensjon.eux.model.SedType.P2200
import no.nav.eessi.pensjon.eux.model.sed.KravType
import no.nav.eessi.pensjon.integrationtest.IntegrasjonsTestConfig
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.kodeverk.Postnummer
import no.nav.eessi.pensjon.pensjonsinformasjon.models.EPSaktype.UFOREP
import no.nav.eessi.pensjon.pensjonsinformasjon.models.KravArsak
import no.nav.eessi.pensjon.pensjonsinformasjon.models.Sakstatus
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.model.AktoerId
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentGruppe.FOLKEREGISTERIDENT
import no.nav.eessi.pensjon.personoppslag.pdl.model.NorskIdent
import no.nav.eessi.pensjon.personoppslag.pdl.model.Npid
import no.nav.eessi.pensjon.prefill.KrrService
import no.nav.eessi.pensjon.prefill.PensjonsinformasjonService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.DigitalKontaktinfo
import no.nav.eessi.pensjon.prefill.sed.krav.PensjonsInformasjonHelper
import no.nav.pensjon.v1.vedtak.V1Vedtak
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
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

    @MockkBean
    lateinit var krrService: KrrService

    @Autowired
    private lateinit var mockMvc: MockMvc

    private companion object {
        const val FNR_VOKSEN = "11067122781"    // KRAFTIG VEGGPRYD
        const val NPID_VOKSEN = "01220049651"
        const val AKTOER_ID = "0123456789000"
    }

    @BeforeEach
    fun setup() {
        every { kodeverkClient.finnLandkode(any())} returns "NO"
        every { kodeverkClient.hentPostSted(any()) } returns Postnummer("1068", "SØRUMSAND")
        every { krrService.hentPersonerFraKrr(any())  } returns DigitalKontaktinfo( "melleby11@melby.no", true, personident = NPID_VOKSEN)

        val sak = PensjonsInformasjonHelper.createSak(
            PensjonsInformasjonHelper.createKravHistorikk(
                KravArsak.GJNL_SKAL_VURD.name,
                KravType.ALDER.name,
                status = Sakstatus.INNV
            ), sakType = UFOREP.name
        )

        every { pensjoninformasjonservice.hentRelevantPensjonSak(any(), any()) } returns sak

        val vedtak = V1Vedtak()
        vedtak.isBoddArbeidetUtland = false
        vedtak.kravGjelder = "REVURD"

        every { pensjoninformasjonservice.hentRelevantVedtakHvisFunnet("231231231") } returns vedtak

    }

    @Test
    fun `prefill sed P2200 som har vedtak, F_BH_BO_UTL men mangler F_BH_MED_UTL i tillegg til at isBoddArbeidetUtland er false så skal det kastes en Exception`() {
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()

        val apijson = dummyApijson(sakid = "1232123123", aktoerId = AKTOER_ID, vedtakid = "231231231", sed = P2200.name,  buc = P_BUC_03.name)
        val expectedError = """Du kan ikke opprette krav-SED P2200 hvis ikke "bodd/arbeidet i utlandet" er krysset av""".trimIndent()

        mockMvc.perform(
            post("/sed/prefill")
            .contentType(MediaType.APPLICATION_JSON)
            .content(apijson))
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.status().reason(Matchers.containsString(expectedError)))

    }

    @Test
    fun `Prefill P2200 med NPID bruker som har vedtak med F_BH_BO_UTL men F_BH_MED_UTL mangler i tillegg til at vedtak isBoddArbeidetUtland for er false så skal det kastes en Exception`() {
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns Npid(NPID_VOKSEN)
        every { personService.hentPerson(Npid(NPID_VOKSEN)) } returns PersonPDLMock.createWith()

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