package no.nav.eessi.pensjon.prefill.sed

import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.P5000
import no.nav.eessi.pensjon.prefill.BasePrefillNav
import no.nav.eessi.pensjon.prefill.etterlatte.EtterlatteService
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.Fodselsnummer
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import no.nav.eessi.pensjon.utils.toJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PrefillP5000GjennyUtenAvdodTest {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(65)
    private val avdodPersonFnr = null
    private val pesysSaksnummer = "21975717"

    lateinit var prefillData: PrefillDataModel

    lateinit var prefill: PrefillP5000
    lateinit var prefillNav: PrefillPDLNav
    lateinit var personDataCollection: PersonDataCollection

    lateinit var p5000: P5000
    lateinit var prefillSEDService: PrefillSEDService
    lateinit var etterlatteService: EtterlatteService

    @Test
    fun `forventer korrekt utfylt P5000 med gjenlevende med uten avdod for gjenny sak uten avdod`() {

        personDataCollection = PersonPDLMock.createFamilieUtenKjentAvdod(personFnr)
        prefillNav = BasePrefillNav.createPrefillNav()

        etterlatteService = EtterlatteService(mockk())
        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P5000, personFnr, penSaksnummer = pesysSaksnummer, avdod = PersonInfo(null, null))

        val pensjonInformasjonService = PrefillTestHelper.lesPensjonsdataFraFil("/pensjonsinformasjon/krav/KravAlderEllerUfore_AP_UTLAND.xml")
        val innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = pensjonInformasjonService)

        val pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)

        println("Pensjonscollection: ${pensjonCollection.toJson()}")

        prefillSEDService = BasePrefillNav.createPrefillSEDService()

        p5000 = prefillSEDService.prefill(prefillData, personDataCollection,pensjonCollection, null) as P5000
        println("@@@P5000: ${p5000.toJson()}")

        assertEquals("Gjenlevende", p5000.pensjon?.gjenlevende?.person?.fornavn)
        assertEquals("Gjenlevende etternavn", p5000.pensjon?.gjenlevende?.person?.etternavn)
        assertEquals("BAMSE LUR", p5000.nav?.bruker?.person?.fornavn)
        assertEquals("MOMBALO", p5000.nav?.bruker?.person?.etternavn)



//        assertEquals("MOMBALO", p5000.nav?.bruker?.person?.etternavn)
//        val navfnr1 = Fodselsnummer.fra(p5000.nav?.bruker?.person?.pin?.get(0)?.identifikator!!)
//        assertEquals(75, navfnr1?.getAge())
//        assertEquals("M", p5000.nav?.bruker?.person?.kjoenn)
//
//        assertNotNull(p5000.nav?.bruker?.person?.pin)
//        val pinlist = p5000.nav?.bruker?.person?.pin
//        val pinitem = pinlist?.get(0)
//        assertEquals(null, pinitem?.sektor)
//        assertEquals(avdodPersonFnr, pinitem?.identifikator)
//
//        assertEquals("DOLLY", p5000.pensjon?.gjenlevende?.person?.etternavn)
//        val navfnr2 = Fodselsnummer.fra(p5000.pensjon?.gjenlevende?.person?.pin?.get(0)?.identifikator!!)
//        assertEquals(65, navfnr2?.getAge())
//        assertEquals("K", p5000.pensjon?.gjenlevende?.person?.kjoenn)
//
//        assertNotNull( p5000.pensjon)
    }
}

