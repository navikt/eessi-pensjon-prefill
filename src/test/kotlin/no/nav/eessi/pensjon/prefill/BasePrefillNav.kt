package no.nav.eessi.pensjon.prefill

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.EessiInformasjonMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLAdresse
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService

object BasePrefillNav {
    internal fun createPrefillNav(prefillPDLAdresse: PrefillPDLAdresse? = null): PrefillPDLNav {
        return PrefillPDLNav(
            prefillAdresse = prefillPDLAdresse ?: mockk {
                every { hentLandkode(null) } returns "NO"
                every { hentLandkode(eq("AUT")) } returns "AU"
                every { hentLandkode(eq("SWE")) } returns "SE"
                every { hentLandkode(eq("USA")) } returns "US"
                every { hentLandkode(eq("NOR")) } returns "NO"
                every { createPersonAdresse(any()) } returns mockk(relaxed = true)
            },
            institutionid = "NO:noinst002",
            institutionnavn = "NOINST002, NO INST002, NO"
        )
    }

    internal fun createPrefillSEDService(prefillPDLNav: PrefillPDLNav? = null): PrefillSEDService {
        return PrefillSEDService(EessiInformasjonMother.standardEessiInfo(), prefillPDLNav ?: createPrefillNav())
    }
}