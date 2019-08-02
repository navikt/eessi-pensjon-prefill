package no.nav.eessi.pensjon.fagmodul.prefill.sed.krav

import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper.createInitialPrefillData
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper.mockPrefillPersonDataFromTPS
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper.pensjonsDataFraPEN
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper.readJsonResponse
import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.person.PersonDataFromTPS
import no.nav.eessi.pensjon.utils.mapAnyToJson
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class PrefillP2200UforpensjonTest {

    private val personFnr = PersonDataFromTPS.generateRandomFnr(67)

    private val pesysSaksnummer = "14069110"

    lateinit var prefillData: PrefillDataModel
    lateinit var sakHelper: SakHelper
    var kravHistorikkHelper = KravHistorikkHelper()
    lateinit var prefill: Prefill<SED>

    @Before
    fun setup() {
        val pensionDataFromPEN = pensjonsDataFraPEN("P2000-AP-14069110.xml")
        val prefillPersonDataFromTPS = mockPrefillPersonDataFromTPS(setOf(
                PersonDataFromTPS.MockTPS("Person-20000.json", personFnr, PersonDataFromTPS.MockTPS.TPSType.PERSON),
                PersonDataFromTPS.MockTPS("Person-21000.json", PersonDataFromTPS.generateRandomFnr(43), PersonDataFromTPS.MockTPS.TPSType.BARN),
                PersonDataFromTPS.MockTPS("Person-22000.json", PersonDataFromTPS.generateRandomFnr(17), PersonDataFromTPS.MockTPS.TPSType.BARN)
        ))

        prefillData = createInitialPrefillData("P2200", personFnr, sakId = pesysSaksnummer)
        prefillData.partSedAsJson["PersonInfo"] = readJsonResponse("other/person_informasjon_selvb.json")

        val prefillNav = PrefillNav(prefillPersonDataFromTPS, institutionid = "NO:noinst002", institutionnavn = "NOINST002, NO INST002, NO")
        sakHelper = SakHelper(prefillNav, prefillPersonDataFromTPS, pensionDataFromPEN, kravHistorikkHelper)
        prefill = PrefillP2200(sakHelper, kravHistorikkHelper)
    }

    @Test
    fun `Testing av komplett utfylling kravsøknad uførepensjon P2200`() {
        val pendata = sakHelper.getPensjoninformasjonFraSak(prefillData)

        assertNotNull(pendata)

        val pensak = sakHelper.getPensjonSak(prefillData, pendata)
        assertNotNull(pendata.brukersSakerListe)

        val P2200 = prefill.prefill(prefillData)
        assertNotNull(mapAnyToJson(P2200))
    }

}
