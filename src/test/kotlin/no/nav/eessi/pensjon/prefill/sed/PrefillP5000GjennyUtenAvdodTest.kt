package no.nav.eessi.pensjon.prefill.sed

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.BucType.P_BUC_02
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.SedType.*
import no.nav.eessi.pensjon.eux.model.sed.P5000
import no.nav.eessi.pensjon.eux.model.sed.P6000
import no.nav.eessi.pensjon.eux.model.sed.P8000
import no.nav.eessi.pensjon.integrationtest.sed.SedPrefillPDLIntegrationSpringTest
import no.nav.eessi.pensjon.integrationtest.sed.SedPrefillPDLIntegrationSpringTest.Companion.AKTOER_ID
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.model.AktoerId
import no.nav.eessi.pensjon.prefill.*
import no.nav.eessi.pensjon.prefill.etterlatte.EtterlatteService
import no.nav.eessi.pensjon.prefill.models.DigitalKontaktinfo
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.EessiInformasjonMother
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.shared.api.ApiRequest
import no.nav.eessi.pensjon.shared.api.InstitusjonItem
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import no.nav.eessi.pensjon.statistikk.AutomatiseringStatistikkService
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.toJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

private const val AKTOERID = "0105094340092"

class PrefillP5000GjennyUtenAvdodTest {
    private val avdodPersonFnr = FodselsnummerGenerator.generateFnrForTest(63)
    private val personFnr = "04016143397"
    private val pesysSaksnummer = "21975717"
    private val institutionid = "111111"
    lateinit var prefillData: PrefillDataModel
    lateinit var prefill: PrefillP5000
    lateinit var prefillNav: PrefillPDLNav
    lateinit var innhentingService: InnhentingService
    lateinit var etterlatteService: EtterlatteService
    lateinit var personDataService : PersonDataService
    lateinit var prefillGjennyService: PrefillGjennyService
    lateinit var prefillSEDService: PrefillSEDService
    private lateinit var dataFromPEN: PensjonsinformasjonService

    var eessiInformasjon = mockk<EessiInformasjon>(relaxed = true)
    var krrService = mockk<KrrService>(relaxed = true)
    var automatiseringStatistikkService = mockk<AutomatiseringStatistikkService>()
    var personservice = mockk<PersonService>()

    @BeforeEach
    fun setup() {
        prefillNav = BasePrefillNav.createPrefillNav()
        personDataService = PersonDataService(personservice)
        etterlatteService = EtterlatteService(mockk())
        innhentingService = InnhentingService(personDataService, pensjonsinformasjonService = mockk())
        prefillSEDService = PrefillSEDService(eessiInformasjon, mockk())
        prefillGjennyService = PrefillGjennyService(krrService, innhentingService, etterlatteService, automatiseringStatistikkService, prefillNav, eessiInformasjon, prefillSEDService)

        every { eessiInformasjon.institutionid } returns institutionid
        justRun { automatiseringStatistikkService.genererAutomatiseringStatistikk(any(), any()) }
    }

    @Disabled
    @Test
    fun `Forventer korrekt preutfylt P5000 med gjenlevende uten kjent avdod for gjenny`() {
        mockPersonReponse(personFnr)
        val apiReq = apiRequest(P5000)

        val p5000 = mapJsonToAny<P5000>(prefillGjennyService.prefillGjennySedtoJson(apiReq))

        assertEquals(personFnr, p5000.pensjon?.gjenlevende?.person?.pin?.firstOrNull()?.identifikator)
        assertEquals(null, p5000.nav?.bruker?.person?.fornavn)
        assertEquals(null, p5000.nav?.bruker?.person?.etternavn)
    }

    @Disabled
    @Test
    fun `Forventer korrekt utfylt P6000 med gjenlevende med avdod for gjenny`() {

        val apiReq = apiRequest(P6000)
        val p6000 = mapJsonToAny<P6000>(prefillGjennyService.prefillGjennySedtoJson(apiReq))

        assertEquals(null, p6000.pensjon?.gjenlevende?.person?.pin?.firstOrNull()?.identifikator)
        assertEquals(null, p6000.pensjon?.gjenlevende?.person?.pin?.get(1)?.identifikator)
//        assertEquals("BEVISST", p6000.pensjon?.gjenlevende?.person?.fornavn)
//        assertEquals("GAUPE", p6000.pensjon?.gjenlevende?.person?.etternavn)
        assertEquals("Avdød", p6000.nav?.bruker?.person?.fornavn)

    }

    @Disabled
    @Test
    fun `Forventer korrekt utfylt P6000 med gjenlevende uten avdod for gjenny`() {
        mockPersonReponse(personFnr)

        val apiReq = apiRequest(P6000)
        val p6000 = mapJsonToAny<P6000>(prefillGjennyService.prefillGjennySedtoJson(apiReq))

        assertEquals("null", p6000.pensjon?.gjenlevende?.person?.pin?.firstOrNull()?.identifikator)
        assertEquals("Avdød", p6000.pensjon?.gjenlevende?.person?.fornavn)
        assertEquals("Død", p6000.pensjon?.gjenlevende?.person?.etternavn)
        assertEquals(null, p6000.nav?.bruker?.person?.fornavn)
        assertEquals(null, p6000.nav?.bruker?.person?.etternavn)
    }

    @Test
    fun `Forventer korrekt preutfylt P8000 med gjenlevende uten avdod for gjenny`() {
        mockPersonReponse(personFnr)

        val apiReq = apiRequest(P8000)
        val p8000 = mapJsonToAny<P8000>(prefillGjennyService.prefillGjennySedtoJson(apiReq))
        println("@@@P8000: ${p8000.toJson()}")

        println("@@@AVDØD: ${p8000.nav?.bruker?.person?.pin?.firstOrNull()?.identifikator}")
        println("@@@Gjenlevende: ${p8000.pensjon?.gjenlevende?.person?.toJson()}")

        assertEquals(personFnr, p8000.nav?.annenperson?.person?.pin?.firstOrNull()?.identifikator)
        assertEquals(institutionid, p8000.nav?.eessisak?.firstOrNull()?.institusjonsid)
        assertEquals("01", p8000.nav?.annenperson?.person?.rolle)
        //AVDØD/FORSIKRET
        assertEquals(null, p8000.nav?.bruker?.person?.pin?.firstOrNull()?.identifikator)
        assertEquals(null, p8000.nav?.bruker?.person?.fornavn)
        assertEquals(null, p8000.nav?.bruker?.person?.etternavn)
    }

    @Test
    fun `En p6000 uten vedtak skal gi en delvis utfylt sed`(){
        dataFromPEN = PrefillTestHelper.lesPensjonsdataVedtakFraFil("/pensjonsinformasjon/vedtak/P6000-GP-401.xml")
        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P6000, personFnr, penSaksnummer = "22580170", vedtakId = "12312312", avdod = PersonInfo(avdodPersonFnr, "1234567891234"))
        prefillSEDService = PrefillSEDService(EessiInformasjonMother.standardEessiInfo(), prefillNav)

        every { personservice.hentIdent(any(), any()) } returns AktoerId(AKTOERID)
        every { personservice.hentPerson( any()) } returns PersonPDLMock.createWith(true, fnr = avdodPersonFnr, aktoerid = AKTOER_ID)

        val prefill = mapJsonToAny<P6000>(prefillGjennyService.prefillGjennySedtoJson(apiRequest(SedType.P6000).copy(avdodfnr = avdodPersonFnr)))

        assertNotNull(prefill.nav?.bruker?.person?.pin)
        assertEquals(avdodPersonFnr, prefill.nav?.bruker?.person?.pin?.firstOrNull()?.identifikator)
        assertEquals(SedType.P6000, prefill.type)
    }

    fun mockPersonReponse(personFnr: String) {
        every { personservice.hentIdent(any(), any()) } returns AktoerId(personFnr)
        every { personservice.hentPerson(any()) } returns PersonPDLMock.createWith(true, "BEVISST", "GAUPE", personFnr, AKTOERID, false) //Gjenlev
        every { krrService.hentPersonerFraKrr(eq(personFnr), any()) } returns DigitalKontaktinfo(
            aktiv = true,
            personident = personFnr
        )
    }


    private fun apiRequest(sedType: SedType): ApiRequest = ApiRequest(
        subjectArea = "Pensjon",
        sakId = pesysSaksnummer,
        institutions = listOf(InstitusjonItem("NO", "Institutt", "InstNavn")),
        euxCaseId = "123456",
        sed = sedType,
        buc = P_BUC_02,
        aktoerId = AKTOERID,
        avdodfnr = null,
        gjenny = true

    )

}

