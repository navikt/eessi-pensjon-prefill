package no.nav.eessi.pensjon.fagmodul.prefill.sed

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import no.nav.eessi.pensjon.fagmodul.prefill.LagTPSPerson.Companion.lagTPSBruker
import no.nav.eessi.pensjon.fagmodul.prefill.LagTPSPerson.Companion.medAdresse
import no.nav.eessi.pensjon.fagmodul.prefill.model.PersonData
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModelMother
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillSed
import no.nav.eessi.pensjon.fagmodul.prefill.tps.FodselsnummerMother.generateRandomFnr
import no.nav.eessi.pensjon.fagmodul.prefill.tps.PrefillAdresse
import no.nav.eessi.pensjon.personoppslag.personv3.PersonV3Service
import no.nav.eessi.pensjon.services.geo.PostnummerService
import no.nav.eessi.pensjon.services.kodeverk.KodeverkClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension


@ExtendWith(MockitoExtension::class)
class PrefillP8000P_BUC_05Test {

    private val personFnr = generateRandomFnr(68)
    private val pesysSaksnummer = "14398627"

    lateinit var prefillData: PrefillDataModel
    lateinit var personV3Service: PersonV3Service
    lateinit var prefill: PrefillP8000
    lateinit var prefillNav: PrefillNav
    lateinit var personData: PersonData

    @Mock
    lateinit var kodeverkClient: KodeverkClient

    lateinit var prefillAdresse: PrefillAdresse

    @BeforeEach
    fun setup() {

        prefillAdresse = PrefillAdresse(PostnummerService(), kodeverkClient)
        prefillNav = PrefillNav(
                prefillAdresse = prefillAdresse,
                institutionid = "NO:noinst002",
                institutionnavn = "NOINST002, NO INST002, NO")

        val prefillSed = PrefillSed(prefillNav, null)

        prefill = PrefillP8000(prefillSed)
        prefillData = PrefillDataModelMother.initialPrefillDataModel("P8000", personFnr, penSaksnummer = pesysSaksnummer)

    }

    @Test
    fun `Forventer korrekt utfylt P8000 med adresse`() {
        val fnr = generateRandomFnr(68)
        val forsikretPerson = lagTPSBruker(fnr, "Christopher", "Robin")
                .medAdresse("Gate",  "SWE")

        personData = PersonData(forsikretPerson = forsikretPerson, ekteTypeValue = "", ektefelleBruker = null, gjenlevendeEllerAvdod = forsikretPerson, barnBrukereFraTPS = listOf())

        doReturn("NO").whenever(kodeverkClient).finnLandkode2("NOR")
        doReturn("SE").whenever(kodeverkClient).finnLandkode2("SWE")
        val sed = prefill.prefill(prefillData, personData)

        assertEquals("Christopher", sed.nav?.bruker?.person?.fornavn)
        assertEquals("Gate 12", sed.nav?.bruker?.adresse?.gate)
        assertEquals("SE", sed.nav?.bruker?.adresse?.land)
        assertEquals(pesysSaksnummer, sed.nav?.eessisak?.firstOrNull()?.saksnummer)
        assertEquals("Robin", sed.nav?.bruker?.person?.etternavn)
        assertEquals(fnr, sed.nav?.bruker?.person?.pin?.firstOrNull()?.identifikator)

    }

}

