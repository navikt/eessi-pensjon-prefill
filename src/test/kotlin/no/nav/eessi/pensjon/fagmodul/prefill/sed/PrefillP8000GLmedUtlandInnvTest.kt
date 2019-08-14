package no.nav.eessi.pensjon.fagmodul.prefill.sed

import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModelMother
import no.nav.eessi.pensjon.fagmodul.prefill.person.PersonDataFromTPS
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillPensjon
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillPerson
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper.setupPersondataFraTPS
import no.nav.eessi.pensjon.fagmodul.prefill.tps.FodselsnummerMother.generateRandomFnr
import no.nav.eessi.pensjon.fagmodul.prefill.tps.NavFodselsnummer
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PrefillP8000GLmedUtlandInnvTest {

    private val personFnr = generateRandomFnr(65)
    private val avdodPersonFnr = generateRandomFnr(75)
    private val pesysSaksnummer = "22875355"

    lateinit var prefillData: PrefillDataModel

    lateinit var prefill: Prefill<SED>
    lateinit var prefillNav: PrefillNav

    @Before
    fun setup() {
        val persondataFraTPS = setupPersondataFraTPS(setOf(
                PersonDataFromTPS.MockTPS("Person-30000.json", personFnr, PersonDataFromTPS.MockTPS.TPSType.PERSON),
                PersonDataFromTPS.MockTPS("Person-31000.json", avdodPersonFnr, PersonDataFromTPS.MockTPS.TPSType.PERSON)
        ))
        prefillNav = PrefillNav(
                preutfyllingPersonFraTPS = persondataFraTPS,
                institutionid = "NO:noinst002", institutionnavn = "NOINST002, NO INST002, NO")

        val prefillPensjon = PrefillPensjon(persondataFraTPS)
        val prefillPerson = PrefillPerson(prefillNav, prefillPensjon)

        prefill = PrefillP8000(prefillPerson)

        prefillData = PrefillDataModelMother.initialPrefillDataModel("P8000", personFnr).apply {
            skipSedkey = listOf("PENSED")
            penSaksnummer = pesysSaksnummer
            avdodAktorID = "112233445566"
            avdod = avdodPersonFnr
        }
    }

    @Test
    fun `forventet korrekt utfylt P8000 gjenlevende med mockdata fra testfiler`() {
        val p8000 = prefill.prefill(prefillData)

        assertEquals("BAMSE LUR", p8000.nav?.bruker?.person?.fornavn)
        assertEquals("MOMBALO", p8000.nav?.bruker?.person?.etternavn)
        val navfnr1 = NavFodselsnummer(p8000.nav?.bruker?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(75, navfnr1.getAge())
        assertEquals("M", p8000.nav?.bruker?.person?.kjoenn)
        //assertEquals("02", p8000.nav?.bruker?.person?.sivilstand?.first()?.status)

        assertNotNull(p8000.nav?.bruker?.person?.pin)
        val pinlist = p8000.nav?.bruker?.person?.pin
        val pinitem = pinlist?.get(0)
        assertEquals(null, pinitem?.sektor)
        assertEquals(avdodPersonFnr, pinitem?.identifikator)

        assertEquals("01", p8000.nav?.annenperson?.person?.rolle)
        assertEquals("BAMSE ULUR", p8000.nav?.annenperson?.person?.fornavn)
        assertEquals("DOLLY", p8000.nav?.annenperson?.person?.etternavn)
        val navfnr2 = NavFodselsnummer(p8000.nav?.annenperson?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(65, navfnr2.getAge())
        assertEquals("K", p8000.nav?.annenperson?.person?.kjoenn)

        assertEquals(null, p8000.pensjon)
    }

}

