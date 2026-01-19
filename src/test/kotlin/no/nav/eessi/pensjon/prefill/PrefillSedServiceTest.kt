package no.nav.eessi.pensjon.prefill

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.BucType.P_BUC_02
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.AndreinstitusjonerItem
import no.nav.eessi.pensjon.eux.model.sed.P6000
import no.nav.eessi.pensjon.prefill.etterlatte.EtterlatteService
import no.nav.eessi.pensjon.prefill.etterlatte.EtterlatteVedtakResponseData
import no.nav.eessi.pensjon.prefill.models.DigitalKontaktinfo
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.shared.api.ApiRequest
import no.nav.eessi.pensjon.shared.api.InstitusjonItem
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import no.nav.eessi.pensjon.statistikk.AutomatiseringStatistikkService
import no.nav.eessi.pensjon.utils.mapJsonToAny
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PrefillSedServiceTest {
    private var eessiInformasjon: EessiInformasjon = mockk(relaxed = true)
    private val personFnr = FodselsnummerGenerator.generateFnrForTest(57)
    private val AKTOR_ID = "321654987321"
    private val avdodPersonFnr = FodselsnummerGenerator.generateFnrForTest(63)

    private val mockPrefillSEDService: PrefillSEDService = mockk()
    private val innhentingService: InnhentingService = mockk()
    private val krrService: KrrService = mockk()
    private val etterlatteService: EtterlatteService = mockk()
    private val automatiseringStatistikkService: AutomatiseringStatistikkService = mockk()

    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var prefillService: PrefillService
    private lateinit var personDataCollection: PersonDataCollection
    private lateinit var prefillNav: PrefillPDLNav
    private lateinit var prefillGjennyService: PrefillGjennyService

    @BeforeEach
    fun setup() {

        prefillNav = BasePrefillNav.createPrefillNav()
        prefillService = PrefillService(
            krrService,
            mockPrefillSEDService,
            innhentingService,
            etterlatteService,
            automatiseringStatistikkService,
            prefillNav
        )

        prefillSEDService = PrefillSEDService(eessiInformasjon, prefillNav)
        val personDataCollectionFamilie = PersonPDLMock.createEnkelFamilie(personFnr, avdodPersonFnr)
        personDataCollection = PersonDataCollection(gjenlevendeEllerAvdod = personDataCollectionFamilie.ektefellePerson, forsikretPerson = personDataCollectionFamilie.forsikretPerson )
        prefillGjennyService = PrefillGjennyService(krrService, innhentingService, etterlatteService, automatiseringStatistikkService, prefillNav, eessiInformasjon, prefillSEDService)

        every { eessiInformasjon.asAndreinstitusjonerItem() } returns AndreinstitusjonerItem(institusjonsnavn = "InstitusjonNavn")
        every { automatiseringStatistikkService.genererAutomatiseringStatistikk(any(), any()) } returns Unit
    }

    @Test
    fun `En p6000 uten vedtak skal gi en delvis utfylt sed`(){
        val request = apiRequest(SedType.P6000)

        every { innhentingService.hentFnrEllerNpidFraAktoerService(eq(AKTOR_ID)) } returns personFnr
        every { krrService.hentPersonerFraKrr(personFnr) } returns DigitalKontaktinfo("bla.bla@.bla.com", aktiv = false, personident = personFnr)
        every { innhentingService.getAvdodAktoerIdPDL(eq(request)) } returns "321654987321"
        every { innhentingService.hentPersonData(any()) } returns personDataCollection
        every { etterlatteService.hentGjennyVedtak(any()) } returns Result.success(EtterlatteVedtakResponseData(vedtak = emptyList()))

        val prefill = mapJsonToAny<P6000>(prefillGjennyService.prefillGjennySedtoJson(request))

        assertNotNull(prefill.nav?.bruker?.person?.pin)
        assertEquals(avdodPersonFnr, prefill.nav?.bruker?.person?.pin?.firstOrNull()?.identifikator)
        assertEquals(SedType.P6000, prefill.type)
    }

//    @Disabled
//    @Test
//    fun `prefillGjenny skal defaulte til prefill når den kalles fra gjenny uten P2100 eller P6000`() {
//        prefillSEDService = mockk()
//        prefillData = PrefillDataModelMother.initialPrefillDataModel(
//            SedType.P6000,
//            personFnr,
//            penSaksnummer = "22580170",
//            vedtakId = "12312312",
//            avdod = PersonInfo(avdodPersonFnr, "1234567891234")
//        )
//
//        val personDataCollection = mockk<PersonDataCollection>()
//        val etterlatteRespData = mockk<EtterlatteVedtakResponseData>()
//        val expectedSED = mockk<SED>()
//
//        every { prefillGjennyService.prefillGjennySedtoJson(any()) } returns expectedSED
//        every { prefillSEDService.prefill(prefillData, personDataCollection, null, etterlatteRespData) } returns expectedSED
//
//        val result = prefillGjennyService.prefillGjennySedtoJson(apiRequest((SedType.P6000)))
//
//        assertNotNull(result)
//        assertEquals(expectedSED, result)
//    }

    private fun apiRequest(sedType: SedType): ApiRequest = ApiRequest(
        subjectArea = "Pensjon",
        sakId = "132546",
        institutions = listOf(InstitusjonItem("NO", "Institutt", "InstNavn")),
        euxCaseId = "123456",
        sed = sedType,
        buc = P_BUC_02,
        aktoerId = AKTOR_ID,
        avdodfnr = avdodPersonFnr,
        gjenny = true

    )
}
