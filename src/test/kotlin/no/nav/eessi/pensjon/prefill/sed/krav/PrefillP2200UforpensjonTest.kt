package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.prefill.*
import no.nav.eessi.pensjon.prefill.PersonPDLMock.medFodsel
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother.initialPrefillDataModel
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper.lesPensjonsdataFraFil
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper.readJsonResponse
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PrefillP2200UforpensjonTest {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(42)
    private val barn1Fnr = FodselsnummerGenerator.generateFnrForTest(12)
    private val barn2Fnr = FodselsnummerGenerator.generateFnrForTest(17)

    lateinit var prefillData: PrefillDataModel
    lateinit var etterlatteService: EtterlatteService
    lateinit var dataFromPEN: PensjonsinformasjonService
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var pensjonCollection: PensjonCollection

    @BeforeEach
    fun setup() {
        etterlatteService = mockk()
        dataFromPEN = lesPensjonsdataFraFil("/pensjonsinformasjon/krav/P2200-UP-INNV.xml")

        prefillData = initialPrefillDataModel(SedType.P2200, personFnr, penSaksnummer = "22874955").apply {
            partSedAsJson["PersonInfo"] = readJsonResponse("/json/nav/other/person_informasjon_selvb.json")
        }
        prefillSEDService = BasePrefillNav.createPrefillSEDService()

        val innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = dataFromPEN)
        pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)


    }

    @Test
    fun `Testing av komplett utfylling kravsøknad uførepensjon P2200`() {
        val pendata: Pensjonsinformasjon = dataFromPEN.hentPensjonInformasjon(prefillData.bruker.norskIdent, prefillData.bruker.aktorId)
        val persondataCollection = PersonPDLMock.createEnkeWithBarn(personFnr, barn1Fnr, barn2Fnr)

        assertNotNull(pendata.brukersSakerListe)

        val P2200 = prefillSEDService.prefill(prefillData, persondataCollection, pensjonCollection, null)
        val p2200Actual = P2200.toJsonSkipEmpty()
        assertNotNull(p2200Actual)
        assertEquals(SedType.P2200, P2200.type)
        assertEquals("JESSINE TORDNU", P2200.nav?.bruker?.person?.fornavn)
        assertEquals("BOUWMANS", P2200.nav?.bruker?.person?.etternavn)
        assertEquals(2, P2200.nav?.barn?.size)

        val barn1 = P2200.nav?.barn?.first()
        val barn2 = P2200.nav?.barn?.last()

        assertEquals("BOUWMANS", barn1?.person?.etternavn)
        assertEquals("TOPPI DOTTO", barn1?.person?.fornavn)
        assertEquals("BOUWMANS", barn2?.person?.etternavn)
        assertEquals("EGIDIJS MASKOT", barn2?.person?.fornavn)

    }

    @Test
    fun `Komplett utfylling P2200 med barn over 18 aar`() {
        val pendata: Pensjonsinformasjon = dataFromPEN.hentPensjonInformasjon(prefillData.bruker.norskIdent, prefillData.bruker.aktorId)

        val personDataCollection = PersonDataCollection(
            forsikretPerson = PersonPDLMock.createWith(),
            gjenlevendeEllerAvdod = PersonPDLMock.createWith(),
            barnPersonList = listOf(PersonPDLMock.createWith(fornavn = "Barn", etternavn = "Barnesen", fnr = "01010436857")
                .medFodsel(LocalDate.of(2004, 1, 1),)
            )
        )

        assertNotNull(pendata.brukersSakerListe)

        val p2200 = prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null)
        assertEquals(SedType.P2200, p2200.type)

        val barn1 = p2200.nav?.barn?.first()

        println("*********$p2200 *********")
        assertEquals("2004-01-01", barn1?.person?.foedselsdato)

    }

}
