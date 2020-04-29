package no.nav.eessi.pensjon.fagmodul.prefill.sed

import com.nhaarman.mockitokotlin2.mock
import no.nav.eessi.pensjon.fagmodul.prefill.model.PersonId
import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModelMother
import no.nav.eessi.pensjon.fagmodul.prefill.person.PersonDataFromTPS
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillGjenlevende
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillSed
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper.setupPersondataFraTPS
import no.nav.eessi.pensjon.fagmodul.prefill.tps.FodselsnummerMother.generateRandomFnr
import no.nav.eessi.pensjon.fagmodul.prefill.tps.NavFodselsnummer
import no.nav.eessi.pensjon.fagmodul.prefill.tps.PrefillAdresse
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PrefillP9000GLmedUtlandInnvTest {

    private val personFnr = generateRandomFnr(65)
    private val avdodPersonFnr = generateRandomFnr(75)
    private val pesysSaksnummer = "22875355"

    lateinit var prefillData: PrefillDataModel

    lateinit var prefill: Prefill<SED>
    lateinit var prefillNav: PrefillNav

    @BeforeEach
    fun setup() {
        val persondataFraTPS = setupPersondataFraTPS(setOf(
                PersonDataFromTPS.MockTPS("Person-30000.json", personFnr, PersonDataFromTPS.MockTPS.TPSType.PERSON),
                PersonDataFromTPS.MockTPS("Person-31000.json", avdodPersonFnr, PersonDataFromTPS.MockTPS.TPSType.PERSON)
        ))
        prefillNav = PrefillNav(
                brukerFromTPS = persondataFraTPS,
                prefillAdresse = mock<PrefillAdresse>(),
                institutionid = "NO:noinst002", institutionnavn = "NOINST002, NO INST002, NO")

        val prefillGjenlevende = PrefillGjenlevende(persondataFraTPS, prefillNav)
        val prefillPerson = PrefillSed(prefillNav, prefillGjenlevende)

        prefill = PrefillDefaultSED(prefillPerson)

        prefillData = PrefillDataModelMother.initialPrefillDataModel("P9000", personFnr, penSaksnummer = pesysSaksnummer, avdod = PersonId(avdodPersonFnr, "112233445566")).apply {
            skipSedkey = listOf("PENSED")
        }
    }

    @Test
    fun `forventet korrekt utfylt P9000 med mockdata fra testfiler`() {
        val p9000 = prefill.prefill(prefillData)

        assertEquals("BAMSE LUR", p9000.nav?.bruker?.person?.fornavn)
        assertEquals("MOMBALO", p9000.nav?.bruker?.person?.etternavn)
        val navfnr1 = NavFodselsnummer(p9000.nav?.bruker?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(75, navfnr1.getAge())
        assertEquals("M", p9000.nav?.bruker?.person?.kjoenn)

        assertNotNull(p9000.nav?.bruker?.person?.pin)
        val pinlist = p9000.nav?.bruker?.person?.pin
        val pinitem = pinlist?.get(0)
        assertEquals(null, pinitem?.sektor)
        assertEquals(avdodPersonFnr, pinitem?.identifikator)

        assertEquals("01", p9000.nav?.annenperson?.person?.rolle)
        assertEquals("BAMSE ULUR", p9000.nav?.annenperson?.person?.fornavn)
        assertEquals("DOLLY", p9000.nav?.annenperson?.person?.etternavn)
        val navfnr2 = NavFodselsnummer(p9000.nav?.annenperson?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(65, navfnr2.getAge())
        assertEquals("K", p9000.nav?.annenperson?.person?.kjoenn)

        assertNotNull(p9000.pensjon)
        assertNotNull(p9000.pensjon?.gjenlevende)

    }

}

