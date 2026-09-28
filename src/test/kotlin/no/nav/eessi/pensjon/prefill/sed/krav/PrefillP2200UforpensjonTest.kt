package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.P2200
import no.nav.eessi.pensjon.prefill.BasePrefillNav
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.PersonPDLMock.medFodsel
import no.nav.eessi.pensjon.prefill.PesysService
import no.nav.eessi.pensjon.prefill.models.pensjon.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother.initialPrefillDataModel
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.prefill.sed.krav.PensjonsInformasjonHelper.readJsonResponse
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PrefillP2200UforpensjonTest {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(42)
    private val barn1Fnr = FodselsnummerGenerator.generateFnrForTest(12)
    private val barn2Fnr = FodselsnummerGenerator.generateFnrForTest(17)

    private val pesysService: PesysService = mockk()
    lateinit var prefillData: PrefillDataModel
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var pensjonCollection: PensjonCollection

    @BeforeEach
    fun setup() {
        every { pesysService.hentP2200data(any(), any()) } returns mockk(){
            every { sak } returns P2xxxMeldingOmPensjonDto.Sak(
                sakType = EessiSakType.UFOREP,
                kravHistorikk = listOf(
                    P2xxxMeldingOmPensjonDto.KravHistorikk(
                        mottattDato = LocalDate.of(2015, 11, 25),
                        kravType = EessiFellesDto.EessiKravGjelder.F_BH_KUN_UTL,
                        virkningstidspunkt = LocalDate.of(2015, 11, 25),
                    )
                ),
                ytelsePerMaaned = emptyList(),
                forsteVirkningstidspunkt = LocalDate.of(2025, 12, 12),
                status = EessiFellesDto.EessiSakStatus.TIL_BEHANDLING,
            )
            every { vedtak } returns P2xxxMeldingOmPensjonDto.Vedtak(boddArbeidetUtland = true)
        }

        prefillData = initialPrefillDataModel(SedType.P2200, personFnr, penSaksnummer = "22874955").apply {
            partSedAsJson["PersonInfo"] = readJsonResponse("/json/nav/other/person_informasjon_selvb.json")
        }
        prefillSEDService = BasePrefillNav.createPrefillSEDService()

        val innhentingService = InnhentingService(mockk(), pesysService = pesysService)
        pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)
    }

    @Test
    fun `Testing av komplett utfylling kravsøknad uførepensjon P2200`() {
        val persondataCollection = PersonPDLMock.createEnkeWithBarn(personFnr, barn1Fnr, barn2Fnr)

        val p2200 = prefillSEDService.prefill(prefillData, persondataCollection, pensjonCollection, null)
        val p2200Json = mapAnyToJson(p2200)
        val p2200Sed = mapJsonToAny<P2200>(p2200Json)

        assertEquals(SedType.P2200, p2200Sed.type)
        assertEquals("JESSINE TORDNU", p2200Sed.navP2200?.bruker?.person?.fornavn)
        assertEquals("BOUWMANS", p2200Sed.navP2200?.bruker?.person?.etternavn)
        assertEquals(2, p2200Sed.navP2200?.barn?.size)

        val barn1 = p2200Sed.navP2200?.barn?.first()
        val barn2 = p2200Sed.navP2200?.barn?.last()

        assertEquals("BOUWMANS", barn1?.person?.etternavn)
        assertEquals("TOPPI DOTTO", barn1?.person?.fornavn)
        assertEquals("BOUWMANS", barn2?.person?.etternavn)
        assertEquals("EGIDIJS MASKOT", barn2?.person?.fornavn)

    }

    @Test
    fun `Komplett utfylling P2200 med barn over 18 aar`() {
        val personDataCollection = PersonDataCollection(
            forsikretPerson = PersonPDLMock.createWith(),
            gjenlevendeEllerAvdod = PersonPDLMock.createWith(),
            barnPersonList = listOf(PersonPDLMock.createWith(fornavn = "Barn", etternavn = "Barnesen", fnr = "01010436857")
                .medFodsel(LocalDate.of(2004, 1, 1),)
            )
        )

        val p2200 = prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null)
        assertEquals(SedType.P2200, p2200.type)

        val p2200Json = mapAnyToJson(p2200)
        val p2200Sed = mapJsonToAny<P2200>(p2200Json)

        val barn1 = p2200Sed.navP2200?.barn?.first()
        assertEquals("2004-01-01", barn1?.person?.foedselsdato)

    }

}
