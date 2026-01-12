package no.nav.eessi.pensjon.prefill

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.kodeverk.Postnummer
import no.nav.eessi.pensjon.prefill.models.EessiInformasjonMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLAdresse
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService

object BasePrefillNav {
    internal fun createPrefillNav(prefillPDLAdresse: PrefillPDLAdresse? = null): PrefillPDLNav {
        val kodeverkClient = mockk<KodeverkClient>().apply {
                every { finnLandkode("") } returns "NO"
                every { finnLandkode(eq("AUT")) } returns "AU"
                every { finnLandkode(eq("SWE")) } returns "SE"
                every { finnLandkode(eq("USA")) } returns "US"
                every { finnLandkode(eq("NOR")) } returns "NO"
                every { finnLandkode(eq("GRD")) } returns "UK"
                every { finnLandkode(eq("HRV")) } returns "CR"

            every { hentPostSted(any()) } returns Postnummer("1068", "OSLO")
            }
        return PrefillPDLNav(
            prefillAdresse = prefillPDLAdresse ?: PrefillPDLAdresse(kodeverkClient, mockk()),
            institutionid = "NO:noinst002",
            institutionnavn = "NOINST002, NO INST002, NO"
        )
    }

    internal fun createPrefillSEDService(prefillPDLNav: PrefillPDLNav? = null): PrefillSEDService {
        return PrefillSEDService(EessiInformasjonMother.standardEessiInfo(), prefillPDLNav ?: createPrefillNav())
    }
}