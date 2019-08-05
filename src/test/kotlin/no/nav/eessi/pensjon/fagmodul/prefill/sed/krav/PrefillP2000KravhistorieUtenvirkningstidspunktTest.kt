package no.nav.eessi.pensjon.fagmodul.prefill.sed.krav

import no.nav.eessi.pensjon.fagmodul.prefill.tps.FodselsnummerMother.generateRandomFnr
import no.nav.eessi.pensjon.fagmodul.sedmodel.Nav
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper.setupPersondataFraTPS
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper.lesPensjonsdataFraFil
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper.readJsonResponse
import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModelMother
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.person.PersonDataFromTPS
import no.nav.eessi.pensjon.fagmodul.prefill.sed.krav.SakHelper.Companion.addRelasjonerBarnOgAvdod
import no.nav.eessi.pensjon.fagmodul.prefill.tps.NavFodselsnummer
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class PrefillP2000KravhistorieUtenvirkningstidspunktTest {

    private val personFnr = generateRandomFnr(67)

    lateinit var prefillData: PrefillDataModel
    lateinit var sakHelper: SakHelper
    lateinit var prefill: Prefill<SED>
    lateinit var prefillNav: PrefillNav

    @Before
    fun setup() {
        val persondataFraTPS = setupPersondataFraTPS(setOf(
                PersonDataFromTPS.MockTPS("Person-11000-GIFT.json", personFnr, PersonDataFromTPS.MockTPS.TPSType.PERSON),
                PersonDataFromTPS.MockTPS("Person-12000-EKTE.json", generateRandomFnr(69), PersonDataFromTPS.MockTPS.TPSType.EKTE)
        ))
        prefillNav = PrefillNav(
                preutfyllingPersonFraTPS = persondataFraTPS,
                institutionid = "NO:noinst002", institutionnavn = "NOINST002, NO INST002, NO")

        sakHelper = SakHelper(
                preutfyllingPersonFraTPS = persondataFraTPS,
                dataFromPEN = lesPensjonsdataFraFil("P2000-AP-KUNUTL-IKKEVIRKNINGTID.xml"))

        prefill = PrefillP2000(prefillNav, sakHelper)

        prefillData = PrefillDataModelMother.initialPrefillDataModel("P2000", personFnr).apply {
            penSaksnummer = "21920707"
            partSedAsJson = mutableMapOf(
                    "PersonInfo" to readJsonResponse("other/person_informasjon_selvb.json"),
                    "P4000" to readJsonResponse("other/p4000_trygdetid_part.json"))
        }
    }

    @Test
    fun `Sjekk av kravsøknad alderpensjon P2000`() {
        val pendata: Pensjonsinformasjon = sakHelper.hentPensjoninformasjonMedAktoerId(prefillData.aktoerID)

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
