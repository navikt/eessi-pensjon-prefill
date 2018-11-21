package no.nav.eessi.eessifagmodul.prefill.krav

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.InstitusjonItem
import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.prefill.PensjonsinformasjonHjelper
import no.nav.eessi.eessifagmodul.prefill.Prefill
import no.nav.eessi.eessifagmodul.prefill.PrefillDataModel
import no.nav.eessi.eessifagmodul.prefill.nav.PrefillNav
import no.nav.eessi.eessifagmodul.prefill.nav.PrefillPersonDataFromTPS
import no.nav.eessi.eessifagmodul.prefill.person.PersonDataFromTPS
import no.nav.eessi.eessifagmodul.services.pensjonsinformasjon.PensjonsinformasjonService
import no.nav.eessi.eessifagmodul.services.pensjonsinformasjon.RequestBuilder
import no.nav.eessi.eessifagmodul.services.personv3.PersonV3Service
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.junit.Before
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.ResourceUtils
import org.springframework.web.client.RestTemplate
import java.time.LocalDate

abstract class AbstractMockKravPensionHelper(private val sedId: String, private val mockKravXMLfil: String) {

    @Mock
    protected lateinit var pensjonsinformasjonRestTemplate: RestTemplate

    @Mock
    protected lateinit var mockPersonV3Service: PersonV3Service

    //løsning for å laste in abstract mockTPStestklasse
    protected class DataFromTPS(mocktps: Set<MockTPS>) : PersonDataFromTPS(mocktps)

    protected lateinit var prefillData: PrefillDataModel

    protected lateinit var pendata: Pensjonsinformasjon

    protected lateinit var kravdata: KravDataFromPEN

    private lateinit var pensionDataFromPEN: PensjonsinformasjonHjelper

    private lateinit var prefillNav: PrefillNav

    private lateinit var personTPS: PrefillPersonDataFromTPS

    protected lateinit var prefill: Prefill<SED>

    @Before
    fun onStart() {
        //mock prefillDataModel
        prefillData = generatePrefillData(sedId, "02345678901")
        createPayload(prefillData)

        //mock TPS data
        personTPS = initMockPrefillPersonDataFromTPS()
        //mock prefillNav data
        prefillNav = PrefillNav(personTPS)
        prefillNav.institutionid = "NO:noinst002"
        prefillNav.institutionnavn = "NOINST002, NO INST002, NO"

        //mock pensjonData
        pensionDataFromPEN = mockPrefillPensionDataFromPEN(mockKravXMLfil)

        //mock kravData
        kravdata = mockKravDataFromPEN(pensionDataFromPEN)

        //mock PrefillP2x00 class
        prefill = createTestClass(prefillNav, personTPS, pensionDataFromPEN)
    }

    abstract fun createTestClass(prefillNav: PrefillNav, personTPS: PrefillPersonDataFromTPS, pensionDataFromPEN: PensjonsinformasjonHjelper): Prefill<SED>

    abstract fun createPayload(prefillData: PrefillDataModel)

    abstract fun createPersonInfoPayLoad(): String

    abstract fun createPersonTrygdetidHistorikk(): String

    //alle tester med aamme personlist for tiden. MOCK TPS
    private fun initMockPrefillPersonDataFromTPS(): PrefillPersonDataFromTPS {
        val datatps = DataFromTPS(
                setOf(
                        PersonDataFromTPS.MockTPS("Person-20000.json", "02345678901"),
                        PersonDataFromTPS.MockTPS("Person-21000.json", "22345678901"),
                        PersonDataFromTPS.MockTPS("Person-22000.json", "12345678901")
                ))
        datatps.mockPersonV3Service = mockPersonV3Service
        return datatps.mockPrefillPersonDataFromTPS()
    }


    private fun readXMLresponse(file: String): ResponseEntity<String> {
        val resource = ResourceUtils.getFile("classpath:pensjonsinformasjon/krav/$file").readText()
        return ResponseEntity(resource, HttpStatus.OK)
    }

    protected fun readJsonResponse(file: String): String {
        return ResourceUtils.getFile("classpath:json/$file").readText()
    }

    private fun mockPrefillPensionDataFromPEN(responseXMLfilename: String): PensjonsinformasjonHjelper {
        whenever(pensjonsinformasjonRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), ArgumentMatchers.eq(String::class.java))).thenReturn(readXMLresponse(responseXMLfilename))

        val pensjonsinformasjonService1 = PensjonsinformasjonService(pensjonsinformasjonRestTemplate, RequestBuilder())

        return PensjonsinformasjonHjelper(pensjonsinformasjonService1)
    }

    private fun mockKravDataFromPEN(prefillPensionDataFromPEN: PensjonsinformasjonHjelper): KravDataFromPEN {
        return KravDataFromPEN(prefillPensionDataFromPEN)
    }


    private fun generatePrefillData(sedId: String, fnr: String? = null, subtractYear: Int? = null): PrefillDataModel {
        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))

        val year = subtractYear ?: 68
        return PrefillDataModel().apply {
            rinaSubject = "Pensjon"
            sed = SED.create(sedId)
            penSaksnummer = "12345"
            vedtakId = "12312312"
            buc = "P_BUC_99"
            aktoerID = "123456789"
            personNr = fnr ?: generateRandomFnr(year)
            institution = items
        }
    }

    private fun generateRandomFnr(yearsToSubtract: Int): String {
        val fnrdate = LocalDate.now().minusYears(yearsToSubtract.toLong())
        val y = fnrdate.year.toString()
        val day = fixDigits(fnrdate.dayOfMonth.toString())
        val month = fixDigits(fnrdate.month.value.toString())
        val fixedyear = y.substring(2, y.length)
        val fnr = day + month + fixedyear + 43352
        return fnr
    }

    private fun fixDigits(str: String): String {
        if (str.length == 1) {
            return "0$str"
        }
        return str
    }


}