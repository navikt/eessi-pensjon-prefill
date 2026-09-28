package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.Nav
import no.nav.eessi.pensjon.eux.model.sed.NavP2200
import no.nav.eessi.pensjon.eux.model.sed.P2200
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.prefill.BasePrefillNav
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.PesysService
import no.nav.eessi.pensjon.prefill.models.pensjon.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiKravGjelder
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.prefill.sed.krav.PensjonsInformasjonHelper.readJsonResponse
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.Fodselsnummer
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.mapJsonToAny
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PrefillP2200UPUtlandInnvTest {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(68)
    private val ekteFnr = FodselsnummerGenerator.generateFnrForTest(70)
    private val pesysSaksnummer = "22874955"
    private val pesysService : PesysService = mockk()

    lateinit var prefillData: PrefillDataModel

    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var pensjonCollection: PensjonCollection
    private lateinit var personDataCollection: PersonDataCollection

    @BeforeEach
    fun setup() {
        every { pesysService.hentP2200data(any(), any()) } returns mockk(){
            every { sak } returns P2xxxMeldingOmPensjonDto.Sak(
                sakType = EessiSakType.UFOREP,
                kravHistorikk = listOf(
                    P2xxxMeldingOmPensjonDto.KravHistorikk(
                        mottattDato = LocalDate.of(2019, 7, 15),
                        kravType = EessiKravGjelder.F_BH_KUN_UTL,
                        virkningstidspunkt = LocalDate.of(2015, 11, 25),
                    )
                ),
                ytelsePerMaaned = emptyList(),
                forsteVirkningstidspunkt = LocalDate.of(2025, 12, 12),
                status = EessiFellesDto.EessiSakStatus.TIL_BEHANDLING,
            )
            every { vedtak } returns P2xxxMeldingOmPensjonDto.Vedtak(boddArbeidetUtland = true)
        }

        personDataCollection = PersonPDLMock.createEnkelFamilie(personFnr, ekteFnr)

        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P2200, personFnr, penSaksnummer = pesysSaksnummer).apply {
            partSedAsJson["PersonInfo"] = readJsonResponse("/json/nav/other/person_informasjon_selvb.json")
            partSedAsJson["P4000"] = readJsonResponse("/json/nav/other/p4000_trygdetid_part.json")
        }

        val innhentingService = InnhentingService(mockk(), pesysService = pesysService)

        pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)
        prefillSEDService = BasePrefillNav.createPrefillSEDService()
    }

    @Test
    fun `forventet korrekt utfylt P2200 uforepensjon med kap4 og 9`() {
        val p2200 = prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null) as P2200

        val p2200ufor = SED(
                type = SedType.P2200,
                pensjon = p2200.pensjon,
                nav = Nav(krav = p2200.navP2200?.krav)
        )

        val p2200UfoerJson = mapAnyToJson(p2200ufor)
        val p2200UfoerSED = mapJsonToAny<P2200>(p2200UfoerJson)


        assertNotNull(p2200UfoerSED.navP2200?.krav)
        assertEquals("2019-07-15", p2200UfoerSED.navP2200?.krav?.dato)

    }

    @Test
    fun `forventet korrekt utfylt P2200 uforepensjon med mockdata fra testfiler`() {
        val p2200 = prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null) as P2200

        assertEquals(null, p2200.navP2200?.barn)

        val p2200Bruker = p2200.navP2200?.bruker
        assertEquals("ODIN ETTØYE", p2200Bruker?.person?.fornavn)
        assertEquals("BALDER", p2200Bruker?.person?.etternavn)
        val navfnr1 = Fodselsnummer.fra(p2200Bruker?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(68, navfnr1?.getAge())

        val arbeidsforhold = p2200Bruker.arbeidsforhold?.get(0)
        assertEquals("", arbeidsforhold?.yrke)
        assertEquals("2018-11-11", arbeidsforhold?.planlagtstartdato)
        assertEquals("2018-11-13", arbeidsforhold?.planlagtpensjoneringsdato)
        assertEquals("07", arbeidsforhold?.type)

        val bank = p2200Bruker.bank
        assertEquals("foo", bank?.navn)
        assertEquals("bar", bank?.konto?.sepa?.iban)
        assertEquals("baz", bank?.konto?.sepa?.swift)


        val pinlist = p2200Bruker.person?.pin
        assertNotNull(pinlist)
        val pinitem = pinlist?.get(0)

        assertEquals(null, pinitem?.sektor)
        assertEquals("NOINST002, NO INST002, NO", pinitem?.institusjonsnavn)
        assertEquals("NO:noinst002", pinitem?.institusjonsid)
        assertEquals(personFnr, pinitem?.identifikator)

        val ektefelle = p2200.navP2200?.ektefelle?.person
        assertEquals("THOR-DOPAPIR", ektefelle?.fornavn)
        assertEquals("RAGNAROK", ektefelle?.etternavn)

        val navfnr = Fodselsnummer.fra(ektefelle?.pin?.get(0)?.identifikator!!)
        assertEquals(70, navfnr?.getAge())
    }

}

