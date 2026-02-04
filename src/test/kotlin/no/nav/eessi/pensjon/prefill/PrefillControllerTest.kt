
package no.nav.eessi.pensjon.prefill

import io.mockk.every
import io.mockk.impl.annotations.SpyK
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.BucType.P_BUC_06
import no.nav.eessi.pensjon.eux.model.SedType.P6000
import no.nav.eessi.pensjon.eux.model.sed.*
import no.nav.eessi.pensjon.logging.AuditLogger
import no.nav.eessi.pensjon.personoppslag.pdl.model.NorskIdent
import no.nav.eessi.pensjon.prefill.PersonDataServiceTest.Companion.FNR_VOKSEN
import no.nav.eessi.pensjon.prefill.etterlatte.EtterlatteService
import no.nav.eessi.pensjon.prefill.models.DigitalKontaktinfo
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiKravGjelder.F_BH_MED_UTL
import no.nav.eessi.pensjon.prefill.models.pensjon.P6000MeldingOmVedtakDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P6000MeldingOmVedtakDto.Trygdeavtale
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.shared.api.ApiRequest
import no.nav.eessi.pensjon.shared.api.InstitusjonItem
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.statistikk.AutomatiseringStatistikkService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate


class PrefillControllerTest {

    @SpyK
    var auditLogger: AuditLogger = AuditLogger()
    var mockPrefillSEDService: PrefillSEDService = mockk()
    var personDataService: PersonDataService = mockk()
    var krrService: KrrService = mockk()
    var pesysService: PesysService = mockk()
    val automatiseringStatistikkService: AutomatiseringStatistikkService = mockk(relaxed = true)
    val etterlatteService: EtterlatteService = mockk(relaxed = true)
    private lateinit var prefillNav: PrefillPDLNav

    private lateinit var prefillController: PrefillController

    @BeforeEach
    fun before() {
        prefillNav = BasePrefillNav.createPrefillNav()

        every { mockPrefillSEDService.prefill(any(), any(), any(), any(),) } returns SED(type = P6000)
        val innhentingService = InnhentingService(personDataService, pesysService = pesysService)
        val prefillService = PrefillService(krrService, mockPrefillSEDService, innhentingService, automatiseringStatistikkService =automatiseringStatistikkService, etterlatteService = etterlatteService, prefillPdlNav = prefillNav)

        prefillController = PrefillController(prefillService, auditLogger)
    }

    @Test
    fun `confirm document`() {
        val mockData = ApiRequest(
            subjectArea = "Pensjon",
            sakId = "EESSI-PEN-123",
            vedtakId = "1234567",
            institutions = listOf(InstitusjonItem("NO", "DUMMY")),
            euxCaseId = "1234567890",
            sed = P6000,
            buc = P_BUC_06,
            aktoerId = "0105094340092"
        )

        every { pesysService.hentP6000data(any()) } returns P6000MeldingOmVedtakDto(
            sakAlder = P6000MeldingOmVedtakDto.SakAlder(EessiFellesDto.EessiSakType.ALDER),
            avdod = P6000MeldingOmVedtakDto.Avdod(null, null, null, null),
            trygdeavtale = Trygdeavtale(erArt10BruktGP = true, erArt10BruktTP = true),
            trygdetidListe = emptyList(),
            vedtak = P6000MeldingOmVedtakDto.Vedtak(
                LocalDate.now(),
                F_BH_MED_UTL.name,
                true,
                true,
                LocalDate.now(),
            ),
            vilkarsvurderingListe = emptyList(),
            ytelsePerMaanedListe = emptyList()
        )

        every {personDataService.hentFnrEllerNpidFraAktoerService(any())} returns "12345"

        val utfyllMock = ApiRequest.buildPrefillDataModelOnExisting(mockData, PersonInfo(NorskIdent("12345").id, mockData.aktoerId!!), null)

        every{
            personDataService.hentPersonData(any())
        } returns( PersonDataCollection(PersonPDLMock.createWith(), PersonPDLMock.createWith()))

        every { krrService.hentPersonerFraKrr(any()) } returns DigitalKontaktinfo(epostadresse = "melleby11@melby.no", true, true, false, "11111111", FNR_VOKSEN)
        val nav = Nav(bruker = Bruker(person = Person(fornavn = "Dummy", etternavn = "Dummy", foedselsdato = "1900-10-11", kjoenn = "K")), krav = Krav("1937-12-11"))
        val mockSed = SED(
            type = utfyllMock.sedType,
            nav = nav
        )

        every{ mockPrefillSEDService.prefill(any(), any(), any(), any(),)} returns mockSed

        val response = prefillController.prefillDocument(mockData)
        Assertions.assertNotNull(response)

        val sed = SED.fromJson(response)

        assertEquals(P6000, sed.type)
        assertEquals("Dummy", sed.nav?.bruker?.person?.fornavn)
        assertEquals("Dummy", sed.nav?.bruker?.person?.etternavn)
    }

}


