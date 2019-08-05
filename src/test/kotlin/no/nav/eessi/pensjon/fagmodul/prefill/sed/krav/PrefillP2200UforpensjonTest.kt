package no.nav.eessi.pensjon.fagmodul.prefill.sed.krav

import no.nav.eessi.pensjon.fagmodul.prefill.tps.FodselsnummerMother.generateRandomFnr
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper.setupPersondataFraTPS
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper.lesPensjonsdataFraFil
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper.readJsonResponse
import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModelMother.initialPrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.person.PersonDataFromTPS
import no.nav.eessi.pensjon.fagmodul.prefill.sed.krav.SakHelper.Companion.addRelasjonerBarnOgAvdod
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class PrefillP2200UforpensjonTest {

    private val personFnr = generateRandomFnr(67)

    lateinit var prefillData: PrefillDataModel
    lateinit var sakHelper: SakHelper
    lateinit var prefill: Prefill<SED>
    lateinit var prefillNav: PrefillNav

    @Before
    fun setup() {
        val persondataFraTPS = setupPersondataFraTPS(setOf(
                PersonDataFromTPS.MockTPS("Person-20000.json", personFnr, PersonDataFromTPS.MockTPS.TPSType.PERSON),
                PersonDataFromTPS.MockTPS("Person-21000.json", generateRandomFnr(43), PersonDataFromTPS.MockTPS.TPSType.BARN),
                PersonDataFromTPS.MockTPS("Person-22000.json", generateRandomFnr(17), PersonDataFromTPS.MockTPS.TPSType.BARN)
        ))
        prefillNav = PrefillNav(
                preutfyllingPersonFraTPS = persondataFraTPS,
                institutionid = "NO:noinst002", institutionnavn = "NOINST002, NO INST002, NO")

        sakHelper = SakHelper(
                preutfyllingPersonFraTPS = persondataFraTPS,
                dataFromPEN = lesPensjonsdataFraFil("P2000-AP-14069110.xml"))

        prefill = PrefillP2200(prefillNav, sakHelper)

        prefillData = initialPrefillDataModel("P2200", personFnr).apply {
            penSaksnummer = "14069110"
            partSedAsJson = mutableMapOf("PersonInfo" to readJsonResponse("other/person_informasjon_selvb.json"))
        }
    }

    @Test
    fun `Testing av komplett utfylling kravsøknad uførepensjon P2200`() {
        val pendata: Pensjonsinformasjon = sakHelper.hentPensjoninformasjonMedAktoerId(prefillData.aktoerID)

        assertNotNull(pendata)

        val pensak = sakHelper.getPensjonSak(prefillData, pendata)
        assertNotNull(pendata.brukersSakerListe)

        val P2200 = prefill.prefill(prefillData)
        assertNotNull(mapAnyToJson(P2200))
    }

}
