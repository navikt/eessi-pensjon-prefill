package no.nav.eessi.pensjon.prefill.sed

import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.prefill.BasePrefillNav
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.Fodselsnummer
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Daniel

class PrefillP9000GLmedUtlandInnvTest {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(65)
    private val avdodPersonFnr = FodselsnummerGenerator.generateFnrForTest(75)

    private val pesysSaksnummer = "22875355"
    lateinit var prefillNav: PrefillPDLNav
    lateinit var prefillData: PrefillDataModel
    lateinit var prefillSEDService: PrefillSEDService
    private lateinit var pensjonCollection: PensjonCollection
    private lateinit var personDataCollection: PersonDataCollection

    @BeforeEach
    fun setup() {
        prefillNav = BasePrefillNav.createPrefillNav()
        personDataCollection = PersonPDLMock.createAvdodFamilie(personFnr, avdodPersonFnr)
        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P9000, personFnr, penSaksnummer = pesysSaksnummer, avdod = PersonInfo(avdodPersonFnr, "112233445566"))

//        val pensjonInformasjonService = PrefillTestHelper.lesPensjonsdataFraFil("/pensjonsinformasjon/krav/KravAlderEllerUfore_AP_UTLAND.xml")
//        val innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = pensjonInformasjonService)

//        pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)
        prefillSEDService = BasePrefillNav.createPrefillSEDService()

    }

    @Test
    fun `forventet korrekt utfylt P9000 med mockdata fra testfiler`() {
        val p9000 = prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null,)

        assertEquals("BAMSE LUR", p9000.nav?.bruker?.person?.fornavn)
        assertEquals("MOMBALO", p9000.nav?.bruker?.person?.etternavn)
        val navfnr1 = Fodselsnummer.fra(p9000.nav?.bruker?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(75, navfnr1?.getAge())
        assertEquals("M", p9000.nav?.bruker?.person?.kjoenn)

        assertNotNull(p9000.nav?.bruker?.person?.pin)
        val pinlist = p9000.nav?.bruker?.person?.pin
        val pinitem = pinlist?.get(0)
        assertEquals(null, pinitem?.sektor)
        assertEquals(avdodPersonFnr, pinitem?.identifikator)

        assertEquals("01", p9000.nav?.annenperson?.person?.rolle)
        assertEquals("BAMSE ULUR", p9000.nav?.annenperson?.person?.fornavn)
        assertEquals("DOLLY", p9000.nav?.annenperson?.person?.etternavn)
        val navfnr2 = Fodselsnummer.fra(p9000.nav?.annenperson?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(65, navfnr2?.getAge())
        assertEquals("K", p9000.nav?.annenperson?.person?.kjoenn)

        assertNotNull(p9000.pensjon)
        assertNotNull(p9000.pensjon?.gjenlevende)
    }
}

