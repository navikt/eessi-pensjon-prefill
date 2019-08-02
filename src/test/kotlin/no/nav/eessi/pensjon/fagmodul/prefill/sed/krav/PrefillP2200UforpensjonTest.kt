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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class PrefillP2200UforpensjonTest : AbstractPrefillIntegrationTestHelper() {

    private val pesysSaksnummer = "14069110"

    @Before
    fun setup() {
        val pensionDataFromPEN = mockPensjonsdataFraPEN("P2000-AP-14069110.xml")
        val prefillPersonDataFromTPS = mockPrefillPersonDataFromTPS(overrideDefaultMockPersonDataTPS())
        onstart(pesysSaksnummer, pensionDataFromPEN, "P2200", prefillPersonDataFromTPS)
    }

    override fun overrideDefaultMockPersonDataTPS(): Set<PersonDataFromTPS.MockTPS> {
        return setOf(
                PersonDataFromTPS.MockTPS("Person-20000.json", PersonDataFromTPS.generateRandomFnr(67), PersonDataFromTPS.MockTPS.TPSType.PERSON),
                PersonDataFromTPS.MockTPS("Person-21000.json", PersonDataFromTPS.generateRandomFnr(43), PersonDataFromTPS.MockTPS.TPSType.BARN),
                PersonDataFromTPS.MockTPS("Person-22000.json", PersonDataFromTPS.generateRandomFnr(17), PersonDataFromTPS.MockTPS.TPSType.BARN)
        )
    }

    override fun createTestClass(prefillNav: PrefillNav, personTPS: PrefillPersonDataFromTPS, pensionDataFromPEN: PensjonsinformasjonHjelper): Prefill<SED> {
        return PrefillP2200(sakHelper, kravHistorikkHelper)
    }

    override fun createPayload(prefillData: PrefillDataModel) {
        prefillData.personNr = PersonDataFromTPS.generateRandomFnr(67)
        prefillData.partSedAsJson["PersonInfo"] = createPersonInfoPayLoad()
    }

    override fun createPersonInfoPayLoad(): String {
        return readJsonResponse("other/person_informasjon_selvb.json")
    }

    override fun createPersonTrygdetidHistorikk(): String {
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
