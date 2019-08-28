package no.nav.eessi.pensjon.fagmodul.prefill.sed.krav

import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModelMother
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonHjelper
import no.nav.eessi.pensjon.fagmodul.prefill.person.PersonDataFromTPS
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper.lesPensjonsdataFraFil
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper.readJsonResponse
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper.setupPersondataFraTPS
import no.nav.eessi.pensjon.fagmodul.prefill.tps.FodselsnummerMother.generateRandomFnr
import no.nav.eessi.pensjon.fagmodul.prefill.tps.NavFodselsnummer
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness


@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PrefillP2000UtenKravhistorieTest {

    private val personFnr = generateRandomFnr(67)

    lateinit var prefillData: PrefillDataModel
    lateinit var prefill: Prefill<SED>
    lateinit var prefillNav: PrefillNav
    lateinit var dataFromPEN: PensjonsinformasjonHjelper

    @BeforeEach
    fun setup() {
        val persondataFraTPS = setupPersondataFraTPS(setOf(
                PersonDataFromTPS.MockTPS("Person-20000.json", personFnr, PersonDataFromTPS.MockTPS.TPSType.PERSON),
                PersonDataFromTPS.MockTPS("Person-21000.json", generateRandomFnr(43), PersonDataFromTPS.MockTPS.TPSType.BARN),
                PersonDataFromTPS.MockTPS("Person-22000.json", generateRandomFnr(17), PersonDataFromTPS.MockTPS.TPSType.BARN)
        ))
        prefillNav = PrefillNav(
                preutfyllingPersonFraTPS = persondataFraTPS,
                institutionid = "NO:noinst002", institutionnavn = "NOINST002, NO INST002, NO")

        dataFromPEN = lesPensjonsdataFraFil("P2000-AP-14069110.xml")

        prefill = PrefillP2000(prefillNav, dataFromPEN, persondataFraTPS)

        prefillData = PrefillDataModelMother.initialPrefillDataModel("P2000", personFnr).apply {
            penSaksnummer = "14069110"
            partSedAsJson = mutableMapOf(
                    "PersonInfo" to readJsonResponse("other/person_informasjon_selvb.json"),
                    "P4000" to readJsonResponse("other/p4000_trygdetid_part.json"))
        }
    }

    @Test
    fun `Sjekk av kravsøknad alderpensjon P2000`() {
        val pendata: Pensjonsinformasjon = dataFromPEN.hentPersonInformasjonMedAktoerId(prefillData.aktoerID)

        assertNotNull(PensjonsinformasjonHjelper.finnSak(prefillData.penSaksnummer, pendata))

        assertNotNull(pendata.brukersSakerListe)
        assertEquals("ALDER", PensjonsinformasjonHjelper.finnSak(prefillData.penSaksnummer, pendata).sakType)

    }

    @Test
    fun `Testing av komplett utfylling kravsøknad alderpensjon ENKW med 2 barn P2000`() {
        val p2000 = prefill.prefill(prefillData)

        val ex = assertThrows<Exception> { // TODO why is this expected?
            prefill.validate(p2000)
        }
        assertEquals("Kravdato mangler", ex.message)

        assertEquals(2, p2000.nav?.barn?.size)

        assertEquals("BOUWMANS", p2000.nav?.barn?.get(0)?.person?.etternavn)
        assertEquals("TOPPI DOTTO", p2000.nav?.barn?.get(0)?.person?.fornavn)

        val navfnr1 = NavFodselsnummer(p2000.nav?.barn?.get(0)?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(43, navfnr1.getAge())

        assertEquals("BOUWMANS", p2000.nav?.barn?.get(1)?.person?.etternavn)
        assertEquals("EGIDIJS MASKOT", p2000.nav?.barn?.get(1)?.person?.fornavn)

        val navfnr2 = NavFodselsnummer(p2000.nav?.barn?.get(1)?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(17, navfnr2.getAge())

        assertNotNull(p2000.nav?.bruker?.person?.pin)
        val pinlist = p2000.nav?.bruker?.person?.pin
        val pinitem = pinlist?.get(0)
        assertEquals(null, pinitem?.sektor)
        assertEquals("NOINST002, NO INST002, NO", pinitem?.institusjonsnavn)
        assertEquals("NO:noinst002", pinitem?.institusjonsid)
        assertEquals(personFnr, pinitem?.identifikator)

        assertEquals("", p2000.nav?.bruker?.arbeidsforhold?.get(0)?.yrke)
        assertEquals("2018-11-12", p2000.nav?.bruker?.arbeidsforhold?.get(0)?.planlagtstartdato)
        assertEquals("2018-11-14", p2000.nav?.bruker?.arbeidsforhold?.get(0)?.planlagtpensjoneringsdato)
        assertEquals("07", p2000.nav?.bruker?.arbeidsforhold?.get(0)?.type)

        assertEquals("foo", p2000.nav?.bruker?.bank?.navn)
        assertEquals("bar", p2000.nav?.bruker?.bank?.konto?.sepa?.iban)
        assertEquals("baz", p2000.nav?.bruker?.bank?.konto?.sepa?.swift)

        assertEquals(1, p2000.pensjon?.ytelser?.size)
    }

    @Test
    fun `utfylling av barn`() {
        val P2000 = prefill.prefill(prefillData)
        assertEquals(2, P2000.nav?.barn?.size)

        assertEquals("BOUWMANS", P2000.nav?.barn?.get(0)?.person?.etternavn)
        assertEquals("TOPPI DOTTO", P2000.nav?.barn?.get(0)?.person?.fornavn)

        val navfnr1 = NavFodselsnummer(P2000.nav?.barn?.get(0)?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(43, navfnr1.getAge())

        assertEquals("BOUWMANS", P2000.nav?.barn?.get(1)?.person?.etternavn)
        assertEquals("EGIDIJS MASKOT", P2000.nav?.barn?.get(1)?.person?.fornavn)

        val navfnr2 = NavFodselsnummer(P2000.nav?.barn?.get(1)?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(17, navfnr2.getAge())
    }

    @Test
    fun `utfulling og test på ektefelle samboer partner`() {
        val P2000 = prefill.prefill(prefillData)

        val result = P2000.nav?.ektefelle
        // TODO Why?
        assertNull(result)
    }

    @Test
    fun `Utfulling og test på verge vil allid være ull`() {
        val P2000 = prefill.prefill(prefillData)

        val result = P2000.nav?.verge
        assertNull(result)
    }

    @Test
    fun `Utfylling alderpensjon ENKKE med uten kravhistorikk (nær blank P2000)`() {
        val P2000 = prefill.prefill(prefillData)

        val navfnr = NavFodselsnummer(P2000.pensjon?.ytelser?.get(0)?.pin?.identifikator!!)
        assertEquals(67, navfnr.getAge())
    }

}
