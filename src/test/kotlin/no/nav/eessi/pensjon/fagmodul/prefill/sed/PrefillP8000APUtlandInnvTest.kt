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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PrefillP8000APUtlandInnvTest {

    private val personFnr = generateRandomFnr(68)
    private val pesysSaksnummer = "14398627"

    lateinit var prefillData: PrefillDataModel

    lateinit var prefill: Prefill<SED>
    lateinit var prefillNav: PrefillNav

    @BeforeEach
    fun setup() {
        val persondataFraTPS = setupPersondataFraTPS(setOf(
                PersonDataFromTPS.MockTPS("Person-11000-GIFT.json", personFnr, PersonDataFromTPS.MockTPS.TPSType.PERSON),
                PersonDataFromTPS.MockTPS("Person-12000-EKTE.json", generateRandomFnr(70), PersonDataFromTPS.MockTPS.TPSType.EKTE)
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
        }
    }

    @Test
    fun `forventet korrekt utfylt P8000 alderperson med mockdata fra testfiler`() {
        val p8000 = prefill.prefill(prefillData)

        assertEquals("HASNAWI-MASK", p8000.nav?.bruker?.person?.fornavn)
        assertEquals("OKOULOV", p8000.nav?.bruker?.person?.etternavn)
        val navfnr1 = NavFodselsnummer(p8000.nav?.bruker?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(68, navfnr1.getAge())

        assertNotNull(p8000.nav?.bruker?.person?.pin)
        val pinlist = p8000.nav?.bruker?.person?.pin
        val pinitem = pinlist?.get(0)
        assertEquals(null, pinitem?.sektor)
        assertEquals(personFnr, pinitem?.identifikator)

        assertNull(p8000.nav?.annenperson)
        assertNull(p8000.pensjon)

    }

}
