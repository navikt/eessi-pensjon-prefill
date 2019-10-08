package no.nav.eessi.pensjon.fagmodul.prefill.person

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.nav.eessi.pensjon.fagmodul.prefill.model.BrukerInformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.tps.BrukerFromTPS
import no.nav.eessi.pensjon.fagmodul.prefill.tps.FodselsnummerMother
import no.nav.eessi.pensjon.fagmodul.prefill.tps.PrefillAdresse
import no.nav.eessi.pensjon.fagmodul.sedmodel.*
import no.nav.eessi.pensjon.fagmodul.sedmodel.Bruker
import no.nav.eessi.pensjon.fagmodul.sedmodel.Person
import no.nav.eessi.pensjon.services.geo.LandkodeService
import no.nav.eessi.pensjon.services.geo.PostnummerService
import no.nav.eessi.pensjon.utils.createXMLCalendarFromString
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PrefillNavTest {

    val mockBrukerFromTPS = mock<BrukerFromTPS>()
    val someInstitutionId = "enInstId"
    val someIntitutionNavn = "instNavn"
    val prefillNav = PrefillNav(mockBrukerFromTPS, PrefillAdresse(PostnummerService(), LandkodeService()), someInstitutionId, someIntitutionNavn)
    val somePenSaksnr = "somePenSaksnr"

    @Test
    fun `prefill med tom respons fra TPS`() {
        val personensPin = "somePersonNr"
        val prefillData = PrefillDataModel().apply {
            penSaksnummer = somePenSaksnr
            personNr = personensPin
        }

        val actual = prefillNav.prefill(prefillData, true)
        val expected = Nav(
                eessisak = listOf(EessisakItem(someInstitutionId, someIntitutionNavn, somePenSaksnr, "NO")),
                krav = Krav(LocalDate.now().toString()))
        assertEquals(expected, actual)
    }

    @Test
    fun `minimal prefill med barn`() {
        val foreldersPin = "somePersonNr"
        val prefillData = PrefillDataModel().apply {
            penSaksnummer = somePenSaksnr
            personNr = foreldersPin
        }
        val barnetsPin = "12345678901"

        val forelder = lagTPSBruker(foreldersPin, "Christopher", "Robin").medBarn(barnetsPin)
        val barn = lagTPSBruker(barnetsPin, "Ole", "Brum")

        whenever(mockBrukerFromTPS.hentBrukerFraTPS(foreldersPin)).thenReturn(forelder)
        whenever(mockBrukerFromTPS.hentBrukerFraTPS(barnetsPin)).thenReturn(barn)

        val actual = prefillNav.prefill(prefillData, true)

        val expected = Nav(
                eessisak = listOf(EessisakItem(someInstitutionId, someIntitutionNavn, somePenSaksnr, "NO")),
                krav = Krav(LocalDate.now().toString()),
                bruker = Bruker(
                        person = lagPerson(foreldersPin, "Christopher", "Robin"),
                        adresse = lagTomAdresse()),
                barn = listOf(BarnItem(
                        person = lagPerson(barnetsPin, "Ole", "Brum"),
                        relasjontilbruker = "BARN")))

        assertEquals(expected, actual)
    }


    @Test
    fun `prefill med barn og relasjon Far`() {
        val somePersonNr = FodselsnummerMother.generateRandomFnr(57).toString()
        val someBarnPersonNr = FodselsnummerMother.generateRandomFnr(17).toString()

        val prefillData = PrefillDataModel().apply {
            penSaksnummer = somePenSaksnr
            personNr = somePersonNr
        }

        val barn = lagTPSBruker(someBarnPersonNr, "Nasse", "Nøff")
        val far = lagTPSBruker(somePersonNr, "Ole", "Brum")
                .withHarFraRolleI(Familierelasjon().withTilRolle(Familierelasjoner().withValue("BARN")).withTilPerson(barn))

        barn.withHarFraRolleI(Familierelasjon().withTilRolle(Familierelasjoner().withValue("FARA")).withTilPerson(far))

        whenever(mockBrukerFromTPS.hentBrukerFraTPS(someBarnPersonNr)).thenReturn(barn)
        whenever(mockBrukerFromTPS.hentBrukerFraTPS(somePersonNr)).thenReturn(far)

        val actual = prefillNav.prefill(prefillData, true)
        val expected = Nav(
                eessisak = listOf(EessisakItem(someInstitutionId, someIntitutionNavn, somePenSaksnr, "NO")),
                krav = Krav(LocalDate.now().toString()),
                bruker = Bruker(
                        person = lagPerson(somePersonNr, "Ole", "Brum"),
                        adresse = lagTomAdresse()),
                barn = listOf(BarnItem(
                        mor = null,
                        far = Foreldre(Person(
                                fornavn = "Ole",
                                etternavnvedfoedsel = "Brum",
                                pin = listOf( PinItem(identifikator = somePersonNr, land = "NO", institusjonsid = "enInstId", institusjonsnavn = "instNavn")))),
                        person = lagPerson(someBarnPersonNr, "Nasse", "Nøff"), relasjontilbruker = "BARN")))

        assertEquals(expected, actual)
    }


    @Test
    fun `minimal prefill med brukerinfo på request`() {
        val brukerensPin = "somePersonNr"
        val prefillData = PrefillDataModel().apply {
            penSaksnummer = somePenSaksnr
            personNr = brukerensPin
            partSedAsJson["PersonInfo"] = mapAnyToJson(
                    BrukerInformasjon(
                            null,
                            null,
                            null,
                            null,
                            null,
                            "Nordnb",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null))
        }

        val brukeren = lagTPSBruker(brukerensPin, "Ole", "Brum")

        whenever(mockBrukerFromTPS.hentBrukerFraTPS(brukerensPin)).thenReturn(brukeren)

        val actual = prefillNav.prefill(prefillData)

        val expected = Nav(
                eessisak = listOf(EessisakItem(someInstitutionId, someIntitutionNavn, somePenSaksnr, "NO")),
                krav = Krav(LocalDate.now().toString()),
                bruker = Bruker(person = lagPerson(brukerensPin, "Ole", "Brum"),
                        arbeidsforhold = listOf(ArbeidsforholdItem(
                                planlagtstartdato = "",
                                arbeidstimerperuke = "",
                                planlagtpensjoneringsdato = "",
                                yrke = "",
                                type = "",
                                sluttdato = "")),
                        bank = Bank(
                                navn = "Nordnb",
                                konto = Konto(
                                        sepa = Sepa(),
                                        innehaver = Innehaver(
                                                rolle = "01",
                                                navn = "Nordnb")),
                                adresse = Adresse()),
                        adresse = lagTomAdresse()))

        assertEquals(expected, actual)
    }

    private fun lagPerson(foreldersPin: String, fornavn: String, etternavn: String) =
            Person(
                    pin = listOf(PinItem(
                            institusjonsnavn = someIntitutionNavn,
                            institusjonsid = someInstitutionId,
                            identifikator = foreldersPin,
                            land = "NO")),
                    statsborgerskap = listOf(StatsborgerskapItem(land = "NO")),
                    etternavn = etternavn,
                    fornavn = fornavn,
                    kjoenn = "M",
                    fornavnvedfoedsel = fornavn)

    private fun lagTPSBruker(foreldersPin: String, fornavn: String, etternavn: String) =
            no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker()
                    .withPersonnavn(Personnavn()
                            .withEtternavn(etternavn)
                            .withFornavn(fornavn))
                    .withKjoenn(Kjoenn().withKjoenn(Kjoennstyper().withValue("M")))
                    .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent(foreldersPin)))
                    .withStatsborgerskap(Statsborgerskap().withLand(Landkoder().withValue("NOR")))

    private fun no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker.medBarn(barnetsPin: String): no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker =
            this
                    .withHarFraRolleI(Familierelasjon()
                            .withTilRolle(Familierelasjoner()
                                    .withValue("BARN"))
                            .withTilPerson(no.nav.tjeneste.virksomhet.person.v3.informasjon.Person()
                                    .withAktoer(PersonIdent()
                                            .withIdent(NorskIdent()
                                                    .withIdent(barnetsPin)))))


    private fun lagTomAdresse(): Adresse {
        return Adresse(
                gate = "",
                bygning = "",
                by = "",
                postnummer = "",
                land = "")
    }

    @Test
    fun testPaahentSivilstandArr9999BlirBlank() {
        val brukerTps = mockBrukerSivilstandTps("9999-01-01", "SKPQ")

        val actualList = PrefillNav.createSivilstand(brukerTps)
        val actual = actualList[0]

        assertEquals("", actual.fradato)
        assertEquals(null, actual.status)
    }

    @Test
    fun testPaahentSivilstandOk() {
        val brukerTps = mockBrukerSivilstandTps("1999-01-01", "UGIF")

        val actualList = PrefillNav.createSivilstand(brukerTps)
        val actual = actualList[0]

        assertEquals("1999-01-01", actual.fradato)
        assertEquals("01", actual.status)
    }

    @Test
    fun testPaahentSivilstandAar2099() {
        val brukerTps = mockBrukerSivilstandTps("2499-12-01", "REPA")

        val actualList = PrefillNav.createSivilstand(brukerTps)
        val actual = actualList[0]

        assertEquals("2499-12-01", actual.fradato)
        assertEquals("04", actual.status)
    }


    private fun mockBrukerSivilstandTps(gyldigPeriode: String, sivilstandType: String): no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker {
        val brukerTps = no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker()
        brukerTps.endretAv = "Test"
        brukerTps.foedested = "OSLO"

        val personnavnTps = Personnavn()
        personnavnTps.mellomnavn = "Dummy Absurd"
        personnavnTps.fornavn = "Dummy"
        personnavnTps.etternavn = "Absurd"
        brukerTps.personnavn = personnavnTps

        val sivilstandTps = Sivilstand()
        sivilstandTps.endretAv = "Test"
        sivilstandTps.fomGyldighetsperiode = createXMLCalendarFromString(gyldigPeriode)

        val sivilstanderTps = Sivilstander()
        sivilstanderTps.value = sivilstandType

        sivilstandTps.sivilstand = sivilstanderTps
        brukerTps.sivilstand = sivilstandTps

        return brukerTps
    }

    @Test
    fun `create birthplace as unknown`() {
        val bruker = no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker()
        bruker.foedested = null

        val result = PrefillNav.createFodested(bruker)

        assertNull(result)
    }

    @Test
    fun `create correct birthplace known`() {
        val bruker = no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker()
        bruker.foedested = "NOR"

        val result = PrefillNav.createFodested(bruker)

        assertNotNull(result)
        assertEquals("NOR", result?.land)
    }

    @Test
    fun `isPersonAvdod gir true`() {
        val person = no.nav.tjeneste.virksomhet.person.v3.informasjon.Person()
                .withPersonstatus(Personstatus()
                        .withPersonstatus(Personstatuser()
                                .withValue("DØD")))

        assertTrue(PrefillNav.isPersonAvdod(person))
    }
}