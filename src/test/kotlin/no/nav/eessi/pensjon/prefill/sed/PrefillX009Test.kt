package no.nav.eessi.pensjon.prefill.sed

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.*
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.shared.api.BankOgArbeid
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PrefillX009Test {
    private val personFnr = FodselsnummerGenerator.generateFnrForTest(68)
    private val avdodFnr = FodselsnummerGenerator.generateFnrForTest(70)
    private lateinit var prefillX009: PrefillX009
    private lateinit var prefillNav: PrefillPDLNav
    private lateinit var persondataCollection: PersonDataCollection

    @BeforeEach
    fun setup() {
        prefillNav = mockk()
        prefillX009 = PrefillX009(prefillNav)
        persondataCollection = PersonPDLMock.createEnkelFamilie(personFnr, avdodFnr)
    }

    @Test
    fun `prefill bruker gjenlevende aar avdod er gitt`() {
        val gjenlevendePerson = Person(fornavn = "Gjen", etternavn = "Levende", foedselsdato = "1960-01-01", kjoenn = "K")
        val navsedPerson = Person(fornavn = "Nav", etternavn = "Bruker", foedselsdato = "1950-01-01", kjoenn = "M")

        val gjenlevende = Bruker(person = gjenlevendePerson)
        val navsed = Nav(bruker = Bruker(person = navsedPerson))

        every { prefillNav.prefill(any(), any(), any(), any()) } returns navsed
        every { prefillNav.createGjenlevende(any(), any()) } returns gjenlevende

        val result = prefillX009.prefill(
            penSaksnummer = "123",
            bruker = PersonInfo("12345", personFnr),
            avdod = PersonInfo("67890", avdodFnr),
            brukerinformasjon = null,
            personData = persondataCollection
        )

        assertEquals(gjenlevendePerson.fornavn, result.xnav?.sak?.kontekst?.bruker?.person?.fornavn)
        assertEquals(gjenlevendePerson.etternavn, result.xnav?.sak?.kontekst?.bruker?.person?.etternavn)
    }

    @Test
    fun `prefill bruker navsed nar avdod mangler`() {
        val navsedPerson = Person(fornavn = "Nav", etternavn = "Bruker", foedselsdato = "1950-01-01", kjoenn = "M")
        val navsed = Nav(bruker = Bruker(person = navsedPerson))

        every { prefillNav.prefill(any(), any(), any(), any()) } returns navsed
        every { prefillNav.createGjenlevende(any(), any()) } returns null

        val result = prefillX009.prefill(
            penSaksnummer = "123",
            bruker = PersonInfo("12345", personFnr),
            avdod = null,
            brukerinformasjon = null,
            personData = persondataCollection
        )

        assertEquals(navsedPerson.fornavn, result.xnav?.sak?.kontekst?.bruker?.person?.fornavn)
        assertEquals(navsedPerson.etternavn, result.xnav?.sak?.kontekst?.bruker?.person?.etternavn)
    }

    @Test
    fun `prefill henter info fra persondetaljer`() {
        val expectedPerson = Person(
            fornavn = "Per",
            etternavn = "Søker",
            foedselsdato = "1960-05-15",
            kjoenn = "M"
        )
        val navsed = Nav(bruker = Bruker(person = expectedPerson))

        every { prefillNav.prefill(any(), any(), any(), any()) } returns navsed
        every { prefillNav.createGjenlevende(any(), any()) } returns null

        val result = prefillX009.prefill(
            penSaksnummer = "456",
            bruker = PersonInfo("12345", personFnr),
            avdod = null,
            brukerinformasjon = null,
            personData = persondataCollection
        )

        assertEquals(expectedPerson.fornavn, result.xnav?.sak?.kontekst?.bruker?.person?.fornavn)
        assertEquals(expectedPerson.etternavn, result.xnav?.sak?.kontekst?.bruker?.person?.etternavn)
        assertEquals(expectedPerson.foedselsdato, result.xnav?.sak?.kontekst?.bruker?.person?.foedselsdato)
        assertEquals(expectedPerson.kjoenn, result.xnav?.sak?.kontekst?.bruker?.person?.kjoenn)
    }

    @Test
    fun `prefill handterer person med null detaljer`() {
        val navsed = Nav(bruker = Bruker(person = Person(fornavn = null, etternavn = null, foedselsdato = null, kjoenn = null)))

        every { prefillNav.prefill(any(), any(), any(), any()) } returns navsed
        every { prefillNav.createGjenlevende(any(), any()) } returns null

        val result = prefillX009.prefill(
            penSaksnummer = "789",
            bruker = PersonInfo("12345", personFnr),
            avdod = null,
            brukerinformasjon = null,
            personData = persondataCollection
        )

        assertNotNull(result.xnav?.sak?.kontekst?.bruker?.person)
        assertNull(result.xnav?.sak?.kontekst?.bruker?.person?.fornavn)
        assertNull(result.xnav?.sak?.kontekst?.bruker?.person?.etternavn)
    }

    @Test
    fun `prefill oppretter X009 med korrekt struktur`() {
        val navsed = Nav(bruker = Bruker(person = Person(fornavn = "Struct", etternavn = "Test", foedselsdato = "1970-01-01", kjoenn = "K")))

        every { prefillNav.prefill(any(), any(), any(), any()) } returns navsed
        every { prefillNav.createGjenlevende(any(), any()) } returns null

        val result = prefillX009.prefill(
            penSaksnummer = "222",
            bruker = PersonInfo("12345", personFnr),
            avdod = null,
            brukerinformasjon = null,
            personData = persondataCollection
        )

        assertNotNull(result.xnav)
        assertNotNull(result.xnav?.sak)
        assertNotNull(result.xnav?.sak?.kontekst)
        assertNotNull(result.xnav?.sak?.kontekst?.bruker)
        assertNotNull(result.xnav?.sak?.kontekst?.bruker?.person)
        assertEquals(SedType.X009, result.type)
    }

    @Test
    fun `prefill kaller prefill Nav med gitte parametre`() {
        val penSaksnummer = "333"
        val bruker = PersonInfo("12345", personFnr)
        val brukerinformasjon = mockk<BankOgArbeid>()
        val navsed = Nav(bruker = Bruker(person = Person(fornavn = "Test", etternavn = "Person", foedselsdato = null, kjoenn = null)))

        every { prefillNav.prefill(any(), any(), any(), any()) } returns navsed
        every { prefillNav.createGjenlevende(any(), any()) } returns null

        prefillX009.prefill(
            penSaksnummer = penSaksnummer,
            bruker = bruker,
            avdod = null,
            brukerinformasjon = brukerinformasjon,
            personData = persondataCollection
        )

        verify {
            prefillNav.prefill(
                penSaksnummer = penSaksnummer,
                bruker = bruker,
                personData = persondataCollection,
                bankOgArbeid = brukerinformasjon
            )
        }
    }

    @Test
    fun `prefill foretrekker gjenlevende over navsed`() {
        val gjenlevendePerson = Person(fornavn = "Gjen", etternavn = "Levende", foedselsdato = "1960-01-01", kjoenn = "K")
        val navsedPerson = Person(fornavn = "Nav", etternavn = "Bruker", foedselsdato = "1950-01-01", kjoenn = "M")

        val gjenlevende = Bruker(person = gjenlevendePerson)
        val navsed = Nav(bruker = Bruker(person = navsedPerson))

        every { prefillNav.prefill(any(), any(), any(), any()) } returns navsed
        every { prefillNav.createGjenlevende(any(), any()) } returns gjenlevende

        val result = prefillX009.prefill(
            penSaksnummer = "555",
            bruker = PersonInfo("12345", personFnr),
            avdod = PersonInfo("67890", avdodFnr),
            brukerinformasjon = null,
            personData = persondataCollection
        )

        assertNotEquals(navsedPerson.fornavn, result.xnav?.sak?.kontekst?.bruker?.person?.fornavn)
        assertEquals(gjenlevendePerson.fornavn, result.xnav?.sak?.kontekst?.bruker?.person?.fornavn)
    }
}

