package no.nav.eessi.pensjon.prefill.sed

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.P5000
import no.nav.eessi.pensjon.personoppslag.Fodselsnummer
import no.nav.eessi.pensjon.personoppslag.FodselsnummerGenerator
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PersonId
import no.nav.eessi.pensjon.prefill.models.PrefillDataModel
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PrefillP5000GLTest {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(65)
    private val avdodPersonFnr = FodselsnummerGenerator.generateFnrForTest(75)
    private val pesysSaksnummer = "21975717"

    lateinit var prefillData: PrefillDataModel

    lateinit var prefill: PrefillP5000
    lateinit var prefillNav: PrefillPDLNav
    lateinit var personDataCollection: PersonDataCollection

    lateinit var p5000: P5000
    lateinit var prefillSEDService: PrefillSEDService

    @BeforeEach
    fun setup() {
        personDataCollection = PersonPDLMock.createAvdodFamilie(personFnr, avdodPersonFnr)

        prefillNav = PrefillPDLNav(
                prefillAdresse = mockk(){
                    every { hentLandkode(any()) } returns "NO"
                    every { createPersonAdresse(any()) } returns mockk(relaxed = true)
                },
                institutionid = "NO:noinst002",
                institutionnavn = "NOINST002, NO INST002, NO")

        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P5000, personFnr, penSaksnummer = pesysSaksnummer, avdod = PersonId(avdodPersonFnr, "112233445566"))

        val pensjonInformasjonService = PrefillTestHelper.lesPensjonsdataFraFil("/pensjonsinformasjon/krav/KravAlderEllerUfore_AP_UTLAND.xml")
        val innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = pensjonInformasjonService)

        val pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)

        prefillSEDService = PrefillSEDService(EessiInformasjon(), prefillNav)
        p5000 = prefillSEDService.prefill(prefillData, personDataCollection,pensjonCollection) as P5000
    }

    @Test
    fun `forventet korrekt utfylt P8000 gjenlevende med mockdata fra testfiler`() {

        assertEquals("BAMSE LUR", p5000.nav?.bruker?.person?.fornavn)
        assertEquals("MOMBALO", p5000.nav?.bruker?.person?.etternavn)
        val navfnr1 = Fodselsnummer.fra(p5000.nav?.bruker?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(75, navfnr1?.getAge())
        assertEquals("M", p5000.nav?.bruker?.person?.kjoenn)

        assertNotNull(p5000.nav?.bruker?.person?.pin)
        val pinlist = p5000.nav?.bruker?.person?.pin
        val pinitem = pinlist?.get(0)
        assertEquals(null, pinitem?.sektor)
        assertEquals(avdodPersonFnr, pinitem?.identifikator)

        assertEquals("BAMSE ULUR", p5000.p5000Pensjon?.gjenlevende?.person?.fornavn)
        assertEquals("DOLLY", p5000.p5000Pensjon?.gjenlevende?.person?.etternavn)
        val navfnr2 = Fodselsnummer.fra(p5000.p5000Pensjon?.gjenlevende?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(65, navfnr2?.getAge())
        assertEquals("K", p5000.p5000Pensjon?.gjenlevende?.person?.kjoenn)

        assertNull( p5000.pensjon)
    }

}

