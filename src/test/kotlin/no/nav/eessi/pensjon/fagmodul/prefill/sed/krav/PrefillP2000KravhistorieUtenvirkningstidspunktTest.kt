package no.nav.eessi.pensjon.fagmodul.prefill.sed.krav

import no.nav.eessi.pensjon.fagmodul.sedmodel.Nav
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.fagmodul.prefill.sed.AbstractPrefillIntegrationTestHelper
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonHjelper
import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.tps.PrefillPersonDataFromTPS
import no.nav.eessi.pensjon.fagmodul.prefill.person.PersonDataFromTPS
import no.nav.eessi.pensjon.fagmodul.prefill.tps.NavFodselsnummer
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner


@RunWith(MockitoJUnitRunner::class)
class PrefillP2000KravhistorieUtenvirkningstidspunktTest : AbstractPrefillIntegrationTestHelper() {

    private val fakeFnr = PersonDataFromTPS.generateRandomFnr(67)

    private val pesysSaksnummer = "21920707"

    lateinit var prefillData: PrefillDataModel
    lateinit var pendata: Pensjonsinformasjon
    lateinit var sakHelper: SakHelper
    var kravHistorikkHelper = KravHistorikkHelper()
    lateinit var prefill: Prefill<SED>

    @Before
    fun setup() {
        val pensionDataFromPEN = mockPensjonsdataFraPEN("P2000-AP-KUNUTL-IKKEVIRKNINGTID.xml")
        val prefillPersonDataFromTPS = mockPrefillPersonDataFromTPS(mockPersonDataFromTPS())
        prefillData = generatePrefillData("P2000", "02345678901", sakId = pesysSaksnummer)
        createPayload(prefillData)
        val prefillNav = PrefillNav(prefillPersonDataFromTPS, institutionid = "NO:noinst002", institutionnavn = "NOINST002, NO INST002, NO")
        sakHelper = SakHelper(prefillNav, prefillPersonDataFromTPS, pensionDataFromPEN, kravHistorikkHelper)
        prefill = createTestClass(prefillNav, prefillPersonDataFromTPS, pensionDataFromPEN)
    }

    fun mockPersonDataFromTPS(): Set<PersonDataFromTPS.MockTPS> {
        return setOf(
                PersonDataFromTPS.MockTPS("Person-11000-GIFT.json", fakeFnr, PersonDataFromTPS.MockTPS.TPSType.PERSON),
                PersonDataFromTPS.MockTPS("Person-12000-EKTE.json", PersonDataFromTPS.generateRandomFnr(69), PersonDataFromTPS.MockTPS.TPSType.EKTE)
        )
    }

    fun createTestClass(prefillNav: PrefillNav, personTPS: PrefillPersonDataFromTPS, pensionDataFromPEN: PensjonsinformasjonHjelper): Prefill<SED> {
        return PrefillP2000(sakHelper, kravHistorikkHelper)
    }

    fun createPayload(prefillData: PrefillDataModel) {
        prefillData.personNr = fakeFnr
        prefillData.partSedAsJson["PersonInfo"] = createPersonInfoPayLoad()
        prefillData.partSedAsJson["P4000"] = createPersonTrygdetidHistorikk()
    }

    fun createPersonInfoPayLoad(): String {
        return readJsonResponse("other/person_informasjon_selvb.json")
    }

    fun createPersonTrygdetidHistorikk(): String {
        return readJsonResponse("other/p4000_trygdetid_part.json")
    }

    @Test
    fun `Sjekk av kravsøknad alderpensjon P2000`() {
        pendata = sakHelper.getPensjoninformasjonFraSak(prefillData)

        assertNotNull(pendata)
        val pensak = sakHelper.getPensjonSak(prefillData, pendata)
        assertNotNull(pensak)

        assertNotNull(pendata.brukersSakerListe)
        assertEquals("ALDER", pensak.sakType)
    }

    @Test
    fun `Utfylling alderpensjon uten kravhistorikk Kunutland uten virkningstidspunkt`() {
        val P2000 = prefill.prefill(prefillData)

        val P2000pensjon = SED("P2000")
        P2000pensjon.pensjon = P2000.pensjon
        P2000pensjon.nav = Nav(
                krav = P2000.nav?.krav
        )

        val sed = P2000pensjon

        val navfnr = NavFodselsnummer(sed.pensjon?.ytelser?.get(0)?.pin?.identifikator!!)
        assertEquals(67, navfnr.getAge())

    }

}
