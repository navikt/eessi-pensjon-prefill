package no.nav.eessi.pensjon.prefill

import io.mockk.*
import io.mockk.junit5.MockKExtension
import no.nav.eessi.pensjon.eux.model.BucType
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.prefill.PersonDataServiceTest.Companion.FNR_VOKSEN
import no.nav.eessi.pensjon.prefill.models.DigitalKontaktinfo
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.shared.api.ApiRequest
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import no.nav.eessi.pensjon.statistikk.AutomatiseringStatistikkService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@ExtendWith(MockKExtension::class)
class PrefillServiceTest{

    var krrService: KrrService = mockk()

    var prefillSedService: PrefillSEDService = mockk()
    var etterlatteService: EtterlatteService = mockk()
    var innhentingService: InnhentingService = mockk()
    var automatiseringStatistikkService: AutomatiseringStatistikkService = mockk()

    private lateinit var personcollection: PersonDataCollection
    lateinit var prefillService: PrefillService
    private val personFnr = FodselsnummerGenerator.generateFnrForTest(57)
    private val avdodPersonFnr = FodselsnummerGenerator.generateFnrForTest(63)
    val requestSlot = slot<PrefillDataModel>()
    val request = mockk<ApiRequest>(relaxed = true)

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        personcollection = PersonDataCollection(null, null)

        prefillService = PrefillService(
            krrService,
            prefillSedService,
            innhentingService,
            etterlatteService,
            automatiseringStatistikkService,
            mockk(relaxed = true),
            MetricsHelper.ForTest()
        )
        every { innhentingService.getAvdodAktoerIdPDL(any())} returns avdodPersonFnr
        every { innhentingService.hentFnrEllerNpidFraAktoerService(any()) } returns personFnr
        every { innhentingService.hentPersonData(any()) } returns personcollection
        every { innhentingService.hentPensjoninformasjonCollection(any()) } returns mockk(relaxed = true)

        justRun { automatiseringStatistikkService.genererAutomatiseringStatistikk(any(), any()) }

        every { request.sed } returns SedType.P2000
        every { request.aktoerId } returns "112233"
        every { request.buc } returns BucType.P_BUC_01
    }


    @ParameterizedTest
    @CsvSource(
        "4.1, test_1@example.com, null",
        "4.2, test_1@example.com, null",
        "4.2, test@example.com, test@example.com",
        "4.3, test@example.com, test@example.com",
        "4.3, test_1@example.com, test_1@example.com"
    )
    fun `epost fra krr skal valideres ihht versjon`(versjon: String, epost: String, forventetEpost: String) {
        every { request.processDefinitionVersion } returns versjon

        val krrPerson = DigitalKontaktinfo(epostadresse = epost, true, true, false, "11111111", FNR_VOKSEN)
        every { krrService.hentPersonerFraKrr(any()) } returns krrPerson
        every { prefillSedService.prefill(capture(requestSlot), any(), any(), any()) } returns SED(SedType.P2000, "sedVer")

        prefillService.prefillSedtoJson(request)
        val capture = requestSlot.captured

        if(forventetEpost.contains("null")) assertNull(capture.bruker.epostKrr)
        else assertEquals(forventetEpost, capture.bruker.epostKrr)
    }
}