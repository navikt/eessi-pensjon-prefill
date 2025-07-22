package no.nav.eessi.pensjon.prefill.person

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PensjonsinformasjonService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother.initialPrefillDataModel
import no.nav.eessi.pensjon.prefill.EtterlatteService
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.person.Fodselsnummer
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PrefillSedEnkeTest {

    private lateinit var prefillPDLNav: PrefillPDLNav
    private lateinit var etterlatteService: EtterlatteService
    private lateinit var pensjonsinformasjonService: PensjonsinformasjonService
    private lateinit var pensjonsinformasjonServiceGjen: PensjonsinformasjonService

    private val fnr = FodselsnummerGenerator.generateFnrForTest(67)
    private val b1fnr = FodselsnummerGenerator.generateFnrForTest(37)
    private val b2fnr = FodselsnummerGenerator.generateFnrForTest(17)

    @BeforeEach
    fun setup() {
        pensjonsinformasjonService = PrefillTestHelper.lesPensjonsdataFraFil("/pensjonsinformasjon/krav/KravAlderEllerUfore_AP_UTLAND.xml")
        pensjonsinformasjonServiceGjen = PrefillTestHelper.lesPensjonsdataFraFil("/pensjonsinformasjon/krav/P2100-GL-UTL-INNV.xml")

        etterlatteService = mockk(relaxed = true)
        prefillPDLNav = PrefillPDLNav(
            mockk {
                every { hentLandkode(any()) } returns "NO"
                every { createPersonAdresse(any()) } returns mockk(relaxed = true)
            },
            institutionid = "NO:noinst002",
            institutionnavn = "NOINST002, NO INST002, NO"
        )
    }

    @Test
    fun `forvent utfylling av person data av ENKE fra TPS P2100`() {

        val persondataCollection = PersonPDLMock.createEnkeWithBarn(fnr, b1fnr, b2fnr)

        val prefillData = initialPrefillDataModel(sedType = SedType.P2100, pinId = fnr, avdod = PersonInfo(norskIdent = fnr, aktorId = "212"), vedtakId = "", penSaksnummer = "22875355")


        val response = prefillPDLNav.prefill(
            penSaksnummer = prefillData.penSaksnummer,
            bruker = prefillData.bruker,
            avdod = prefillData.avdod,
            personData = persondataCollection,
            bankOgArbeid = prefillData.getBankOgArbeidFromRequest(),
            krav = null,
            annenPerson = null
        )

        val sed = SED(
            type = SedType.P2100,
            nav = response
        )

        assertEquals("JESSINE TORDNU", sed.nav?.bruker?.person?.fornavn)
        assertEquals("BOUWMANS", sed.nav?.bruker?.person?.etternavn)
        assertEquals("K", sed.nav?.bruker?.person?.kjoenn)

        assertEquals(2, sed.nav?.barn?.size)

        val resultBarn = sed.nav?.barn!!

        val item1 = resultBarn.first()
        assertEquals("BOUWMANS", item1.person?.etternavn)
        assertEquals("TOPPI DOTTO", item1.person?.fornavn)
        val ident1 = item1.person?.pin?.get(0)?.identifikator
        val navfnr1 = Fodselsnummer.fra(ident1!!)
        assertEquals(false, navfnr1?.isUnder18Year())
        assertEquals(37, navfnr1?.getAge())


        val item2 = resultBarn.last()
        assertEquals("BOUWMANS", item2.person?.etternavn)
        assertEquals("EGIDIJS MASKOT", item2.person?.fornavn)
        val ident = item2.person?.pin?.get(0)?.identifikator
        val navfnr = Fodselsnummer.fra(ident!!)
        assertEquals(true, navfnr?.isUnder18Year())
    }

    @Test
    fun `forvent utfylling av person data av ENKE fra PDL P2200`() {

        val prefillData = initialPrefillDataModel(sedType = SedType.P2200, pinId = fnr, vedtakId = "", penSaksnummer = "14915730")
        val personCollection = PersonPDLMock.createEnkeWithBarn(fnr, b2fnr)

        val innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = pensjonsinformasjonService)
        val pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)

        val sed = PrefillSEDService(EessiInformasjon(), prefillPDLnav = prefillPDLNav).prefill(prefillData, personCollection, pensjonCollection, null)

        assertEquals(SedType.P2200, sed.type)

        assertEquals("JESSINE TORDNU", sed.nav?.bruker?.person?.fornavn)
        assertEquals("BOUWMANS", sed.nav?.bruker?.person?.etternavn)
        assertEquals("K", sed.nav?.bruker?.person?.kjoenn)
        assertEquals(1, sed.nav?.barn?.size)

        val barn = sed.nav?.barn?.last()!!
        val ident = barn.person?.pin?.get(0)?.identifikator
        val navfnr = Fodselsnummer.fra(ident!!)

        assertEquals("BOUWMANS", barn.person?.etternavn)
        assertEquals("TOPPI DOTTO", barn.person?.fornavn)
        assertEquals(true, navfnr?.isUnder18Year())

    }


}
