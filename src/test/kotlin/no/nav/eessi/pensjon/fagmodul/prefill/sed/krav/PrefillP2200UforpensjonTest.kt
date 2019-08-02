package no.nav.eessi.pensjon.fagmodul.prefill.sed.krav

import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.fagmodul.prefill.sed.AbstractPrefillIntegrationTestHelper
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonHjelper
import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.tps.PrefillPersonDataFromTPS
import no.nav.eessi.pensjon.fagmodul.prefill.person.PersonDataFromTPS
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class PrefillP2200UforpensjonTest : AbstractPrefillIntegrationTestHelper() {

    private val pesysSaksnummer = "14069110"

    lateinit var prefillData: PrefillDataModel
    lateinit var pendata: Pensjonsinformasjon
    lateinit var sakHelper: SakHelper
    var kravHistorikkHelper = KravHistorikkHelper()
    lateinit var prefill: Prefill<SED>

    @Before
    fun setup() {
        val pensionDataFromPEN = mockPensjonsdataFraPEN("P2000-AP-14069110.xml")
        val prefillPersonDataFromTPS = mockPrefillPersonDataFromTPS(mockPersonDataFromTPS())
        prefillData = generatePrefillData("P2200", "02345678901", sakId = pesysSaksnummer)
        createPayload(prefillData)
        val prefillNav = PrefillNav(prefillPersonDataFromTPS, institutionid = "NO:noinst002", institutionnavn = "NOINST002, NO INST002, NO")
        sakHelper = SakHelper(prefillNav, prefillPersonDataFromTPS, pensionDataFromPEN, kravHistorikkHelper)
        prefill = createTestClass(prefillNav, prefillPersonDataFromTPS, pensionDataFromPEN)
    }

    fun mockPersonDataFromTPS(): Set<PersonDataFromTPS.MockTPS> {
        return setOf(
                PersonDataFromTPS.MockTPS("Person-20000.json", PersonDataFromTPS.generateRandomFnr(67), PersonDataFromTPS.MockTPS.TPSType.PERSON),
                PersonDataFromTPS.MockTPS("Person-21000.json", PersonDataFromTPS.generateRandomFnr(43), PersonDataFromTPS.MockTPS.TPSType.BARN),
                PersonDataFromTPS.MockTPS("Person-22000.json", PersonDataFromTPS.generateRandomFnr(17), PersonDataFromTPS.MockTPS.TPSType.BARN)
        )
    }

    fun createTestClass(prefillNav: PrefillNav, personTPS: PrefillPersonDataFromTPS, pensionDataFromPEN: PensjonsinformasjonHjelper): Prefill<SED> {
        return PrefillP2200(sakHelper, kravHistorikkHelper)
    }

    fun createPayload(prefillData: PrefillDataModel) {
        prefillData.personNr = PersonDataFromTPS.generateRandomFnr(67)
        prefillData.partSedAsJson["PersonInfo"] = createPersonInfoPayLoad()
    }

    fun createPersonInfoPayLoad(): String {
        return readJsonResponse("other/person_informasjon_selvb.json")
    }

    fun createPersonTrygdetidHistorikk(): String {
        return ""
    }


    @Test
    fun `Testing av komplett utfylling kravsøknad uførepensjon P2200`() {

        pendata = sakHelper.getPensjoninformasjonFraSak(prefillData)
        assertNotNull(pendata)

        val pensak = sakHelper.getPensjonSak(prefillData, pendata)
        assertNotNull(pendata.brukersSakerListe)

        val P2200 = prefill.prefill(prefillData)
        assertNotNull(mapAnyToJson(P2200))
    }

}
