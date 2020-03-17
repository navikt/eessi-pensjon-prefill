package no.nav.eessi.pensjon.fagmodul.prefill.person

import com.nhaarman.mockitokotlin2.mock
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModelMother.initialPrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.tps.FodselsnummerMother.generateRandomFnr
import no.nav.eessi.pensjon.fagmodul.prefill.tps.NavFodselsnummer
import no.nav.eessi.pensjon.fagmodul.prefill.tps.PrefillAdresse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PrefillPersonEnkeTest {

    private lateinit var personDataFromTPS: PersonDataFromTPS

    @BeforeEach
    fun setup() {
        personDataFromTPS = PersonDataFromTPS(
                setOf(
                        PersonDataFromTPS.MockTPS("Person-20000.json", generateRandomFnr(67), PersonDataFromTPS.MockTPS.TPSType.PERSON),
                        PersonDataFromTPS.MockTPS("Person-21000.json", generateRandomFnr(37), PersonDataFromTPS.MockTPS.TPSType.BARN),
                        PersonDataFromTPS.MockTPS("Person-22000.json", generateRandomFnr(17), PersonDataFromTPS.MockTPS.TPSType.BARN)
                ))
    }

    @Test
    fun `forvent utfylling av person data av ENKE fra TPS P2000`() {
        val preutfyllingTPS = personDataFromTPS.mockBrukerFromTPS()
        val prefillNav = PrefillNav(preutfyllingTPS, mock<PrefillAdresse>(), institutionid = "NO:noinst002", institutionnavn = "NOINST002, NO INST002, NO")

        val fnr = personDataFromTPS.getRandomNavFodselsnummer() ?: "02345678901"
        val prefillData = initialPrefillDataModel(sedType = "P2000", pinId = fnr, vedtakId = "")

        val response = prefillNav.prefill(prefillData, fyllUtBarnListe = true)

        val sed = prefillData.sed
        sed.nav = response

        assertEquals("JESSINE TORDNU", sed.nav?.bruker?.person?.fornavn)
        assertEquals("BOUWMANS", sed.nav?.bruker?.person?.etternavn)
        assertEquals("K", sed.nav?.bruker?.person?.kjoenn)
        assertEquals(2, sed.nav?.barn?.size)

    }

    @Test
    fun `forvent utfylling av person data av ENKE fra TPS P2100`() {
        val preutfyllingTPS = personDataFromTPS.mockBrukerFromTPS()
        val prefillNav = PrefillNav(preutfyllingTPS, mock<PrefillAdresse>(), institutionid = "NO:noinst002", institutionnavn = "NOINST002, NO INST002, NO")

        val fnr = personDataFromTPS.getRandomNavFodselsnummer() ?: "02345678901"
        val prefillData = initialPrefillDataModel(sedType = "P2100", pinId = fnr, vedtakId = "")
        val response = prefillNav.prefill(prefillData, fyllUtBarnListe = true)

        val sed = prefillData.sed
        sed.nav = response

        assertEquals("JESSINE TORDNU", sed.nav?.bruker?.person?.fornavn)
        assertEquals("BOUWMANS", sed.nav?.bruker?.person?.etternavn)
        assertEquals("K", sed.nav?.bruker?.person?.kjoenn)

        assertEquals(2, sed.nav?.barn?.size)

        val resultBarn = sed.nav?.barn

        val item1 = resultBarn.orEmpty().get(0)
        assertEquals("BOUWMANS", item1.person?.etternavn)
        assertEquals("TOPPI DOTTO", item1.person?.fornavn)
        val ident1 = item1.person?.pin?.get(0)?.identifikator
        val navfnr1 = NavFodselsnummer(ident1!!)
        assertEquals(false, navfnr1.isUnder18Year())
        assertEquals(37, navfnr1.getAge())


        val item2 = resultBarn.orEmpty().get(1)
        assertEquals("BOUWMANS", item2.person?.etternavn)
        assertEquals("EGIDIJS MASKOT", item2.person?.fornavn)
        val ident = item2.person?.pin?.get(0)?.identifikator
        val navfnr = NavFodselsnummer(ident!!)
        assertEquals(true, navfnr.isUnder18Year())
    }

    @Test
    fun `forvent utfylling av person data av ENKE fra TPS P2200`() {
        val preutfyllingTPS = personDataFromTPS.mockBrukerFromTPS()
        val prefillNav = PrefillNav(preutfyllingTPS, mock<PrefillAdresse>(), institutionid = "NO:noinst002", institutionnavn = "NOINST002, NO INST002, NO")

        val fnr = personDataFromTPS.getRandomNavFodselsnummer() ?: "02345678901"
        val prefillData = initialPrefillDataModel(sedType = "P2200", pinId = fnr, vedtakId = "")
        val response = prefillNav.prefill(prefillData, fyllUtBarnListe = true)

        val sed = prefillData.sed
        sed.nav = response

        assertEquals("JESSINE TORDNU", sed.nav?.bruker?.person?.fornavn)
        assertEquals("BOUWMANS", sed.nav?.bruker?.person?.etternavn)
        assertEquals("K", sed.nav?.bruker?.person?.kjoenn)
        assertEquals(2, sed.nav?.barn?.size)

        assertEquals("P2200", sed.sed)

    }

}
