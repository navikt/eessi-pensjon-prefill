package no.nav.eessi.pensjon.eux.model

import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.eux.model.sed.X005
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SedX005Test {

    @Test
    fun `compare SED X005 to X005 from json datafile`() {

        val x005json = getTestJsonFile("X005-NAV.json")
        val x005sed = SED.fromJsonToConcrete(x005json) as X005

        assertEquals("Duck", x005sed.xnav?.sak?.kontekst?.bruker?.person?.etternavn)
        assertEquals("Dummy", x005sed.xnav?.sak?.kontekst?.bruker?.person?.fornavn)
        assertEquals("1958-02-01", x005sed.xnav?.sak?.kontekst?.bruker?.person?.foedselsdato)

        assertEquals("NO:NAVT002", x005sed.xnav?.sak?.leggtilinstitusjon?.institusjon?.id)
        assertEquals("NAVT002", x005sed.xnav?.sak?.leggtilinstitusjon?.institusjon?.navn)

        x005sed.toJsonSkipEmpty()

        val xprefill005json = getTestJsonFile("PrefillX005-NAV.json")
        val xprefill005sed = SED.fromJsonToConcrete(xprefill005json) as X005

        assertEquals("POTET", xprefill005sed.xnav?.sak?.kontekst?.bruker?.person?.etternavn)
        assertEquals("KRIMINELL", xprefill005sed.xnav?.sak?.kontekst?.bruker?.person?.fornavn)
        assertEquals("1944-12-25", xprefill005sed.xnav?.sak?.kontekst?.bruker?.person?.foedselsdato)

        assertEquals("NO:NAVT007", xprefill005sed.xnav?.sak?.leggtilinstitusjon?.institusjon?.id)
        assertEquals("NAVT007", xprefill005sed.xnav?.sak?.leggtilinstitusjon?.institusjon?.navn)
    }
}
