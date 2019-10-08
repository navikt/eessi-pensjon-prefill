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

    @Test
    fun `prefill med tom respons fra TPS`() {
        val mockBrukerFromTPS = mock<BrukerFromTPS>()
        val someInstitutionId = "enInstId"
        val someIntitutionNavn = "instNavn"
        val prefillNav = PrefillNav(mockBrukerFromTPS, PrefillAdresse(PostnummerService(), LandkodeService()), someInstitutionId, someIntitutionNavn)
        val somePenSaksnr = "somePenSaksnr"
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
        val mockBrukerFromTPS = mock<BrukerFromTPS>()
        val someInstitutionId = "enInstId"
        val someIntitutionNavn = "instNavn"
        val prefillNav = PrefillNav(mockBrukerFromTPS, PrefillAdresse(PostnummerService(), LandkodeService()), someInstitutionId, someIntitutionNavn)
        val somePenSaksnr = "somePenSaksnr"
        val foreldersPin = "somePersonNr"
        val prefillData = PrefillDataModel().apply {
            penSaksnummer = somePenSaksnr
            personNr = foreldersPin
        }
        val barnetsPin = "12345678901"

        val forelder = no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker()
                .withPersonnavn(Personnavn()
                        .withEtternavn("Robin")
                        .withFornavn("Christopher"))
                .withKjoenn(Kjoenn().withKjoenn(Kjoennstyper().withValue("M")))
                .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent(foreldersPin)))
                .withStatsborgerskap(Statsborgerskap().withLand(Landkoder().withValue("NOR")))
                .withHarFraRolleI(Familierelasjon()
                        .withTilRolle(Familierelasjoner()
                                .withValue("BARN"))
                        .withTilPerson(no.nav.tjeneste.virksomhet.person.v3.informasjon.Person()
                                .withAktoer(PersonIdent()
                                        .withIdent(NorskIdent()
                                                .withIdent(barnetsPin)))))

        whenever(mockBrukerFromTPS.hentBrukerFraTPS(foreldersPin)).thenReturn(forelder)

        val barn = no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker()
                .withPersonnavn(Personnavn()
                        .withEtternavn("Brum")
                        .withFornavn("Ole"))
                .withKjoenn(Kjoenn().withKjoenn(Kjoennstyper().withValue("M")))
                .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent(barnetsPin)))
                .withStatsborgerskap(Statsborgerskap().withLand(Landkoder().withValue("NOR")))

        whenever(mockBrukerFromTPS.hentBrukerFraTPS(barnetsPin)).thenReturn(barn)

        val actual = prefillNav.prefill(prefillData, true)

        val expected = Nav(
                eessisak = listOf(EessisakItem(someInstitutionId, someIntitutionNavn, somePenSaksnr, "NO")),
                krav = Krav(LocalDate.now().toString()),
                bruker = Bruker(
                        person = Person(
                                pin = listOf(PinItem(
                                        institusjonsnavn = someIntitutionNavn,
                                        institusjonsid = someInstitutionId,
                                        identifikator = foreldersPin,
                                        land = "NO")),
                                statsborgerskap = listOf(StatsborgerskapItem(land = "NO")),
                                etternavn = "Robin",
                                fornavn = "Christopher",
                                kjoenn = "M",
                                fornavnvedfoedsel = "Christopher"),
                        adresse = Adresse(
                                gate = "",
                                bygning = "",
                                by = "",
                                postnummer = "",
                                land = "")),
                barn = listOf(BarnItem(
                        person = Person(
                                pin = listOf(PinItem(
                                        institusjonsnavn = someIntitutionNavn,
                                        institusjonsid = someInstitutionId,
                                        identifikator = barnetsPin,
                                        land = "NO")),
                                statsborgerskap = listOf(StatsborgerskapItem(land = "NO")),
                                etternavn = "Brum",
                                fornavn = "Ole",
                                kjoenn = "M",
                                fornavnvedfoedsel = "Ole"),
                        relasjontilbruker = "BARN")))

        assertEquals(expected, actual)
    }

    @Test
    fun `prefill med barn og relasjon Far`() {
        val mockBrukerFromTPS = mock<BrukerFromTPS>()
        val someInstitutionId = "enInstId"
        val someIntitutionNavn = "instNavn"
        val prefillNav = PrefillNav(mockBrukerFromTPS, PrefillAdresse(PostnummerService(), LandkodeService()), someInstitutionId, someIntitutionNavn)
        val somePenSaksnr = "somePenSaksnr"
        val somePersonNr = FodselsnummerMother.generateRandomFnr(57).toString()
        val someBarnPersonNr = FodselsnummerMother.generateRandomFnr(17).toString()

        val prefillData = PrefillDataModel().apply {
            penSaksnummer = somePenSaksnr
            personNr = somePersonNr
        }

        val barnBruker = no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker()
                .withPersonnavn(Personnavn()
                        .withFornavn("Nasse")
                        .withEtternavn("Nøff"))
                .withKjoenn(Kjoenn().withKjoenn(Kjoennstyper().withValue("K")))
                .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent(someBarnPersonNr)))
                .withStatsborgerskap(Statsborgerskap().withLand(Landkoder().withValue("NOR")))

        val someBruker = no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker()
                .withPersonnavn(Personnavn()
                        .withEtternavn("Brum")
                        .withFornavn("Ole"))
                .withKjoenn(Kjoenn().withKjoenn(Kjoennstyper().withValue("M")))
                .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent(somePersonNr)))
                .withStatsborgerskap(Statsborgerskap().withLand(Landkoder().withValue("NOR")))
                .withHarFraRolleI(Familierelasjon().withTilRolle(Familierelasjoner().withValue("BARN")).withTilPerson(barnBruker))

        barnBruker.withHarFraRolleI(Familierelasjon().withTilRolle(Familierelasjoner().withValue("FARA")).withTilPerson(someBruker)
        )


        whenever(mockBrukerFromTPS.hentBrukerFraTPS(somePersonNr)).thenReturn(someBruker)

        whenever(mockBrukerFromTPS.hentBrukerFraTPS(someBarnPersonNr)).thenReturn(barnBruker)

        val actual = prefillNav.prefill(prefillData, true)
        val expected = Nav(
                eessisak = listOf(EessisakItem(someInstitutionId, someIntitutionNavn, somePenSaksnr, "NO")),
                krav = Krav(LocalDate.now().toString()),
                bruker = Bruker(person = Person(
                        etternavn = "Brum",
                        fornavn = "Ole",
                        fornavnvedfoedsel = "Ole",
                        kjoenn = "M",
                        pin = listOf( PinItem(identifikator = somePersonNr, land = "NO", institusjonsid = "enInstId", institusjonsnavn = "instNavn")),
                        statsborgerskap = listOf(StatsborgerskapItem(land = "NO"))
                ),
                        adresse = Adresse(
                                gate = "",
                                bygning = "",
                                by = "",
                                postnummer = "",
                                land = "")
                ) ,
                barn = listOf(BarnItem(mor = null, far = Foreldre(
                 Person(
                         fornavn = "Ole",
                         etternavnvedfoedsel = "Brum",
                         pin = listOf( PinItem(identifikator = somePersonNr, land = "NO", institusjonsid = "enInstId", institusjonsnavn = "instNavn"))
                 )
                ), person =
                Person(
                        fornavn = "Nasse",
                        etternavn = "Nøff",
                        fornavnvedfoedsel = "Nasse",
                        kjoenn = "K",
                        pin = listOf( PinItem(identifikator = someBarnPersonNr, land = "NO", institusjonsid = "enInstId", institusjonsnavn = "instNavn")),
                        statsborgerskap = listOf(StatsborgerskapItem(land = "NO"))
                ),
                        relasjontilbruker = "BARN")))

        assertEquals(expected, actual)
    }


    @Test
    fun `minimal prefill med brukerinfo på request`() {
        val mockBrukerFromTPS = mock<BrukerFromTPS>()
        val someInstitutionId = "enInstId"
        val someIntitutionNavn = "instNavn"
        val prefillNav = PrefillNav(mockBrukerFromTPS, PrefillAdresse(PostnummerService(), LandkodeService()), someInstitutionId, someIntitutionNavn)
        val somePenSaksnr = "somePenSaksnr"
        val brukerensPin = "somePersonNr"
        val prefillData = PrefillDataModel().apply {
            penSaksnummer = somePenSaksnr
            personNr = brukerensPin
            partSedAsJson["PersonInfo"] = mapAnyToJson(BrukerInformasjon(null,null,null,null,null,"Nordnb",null,null,null,null,null,null,null,null,null,null,null,null))
        }

        val brukeren = no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker()
                .withPersonnavn(Personnavn()
                        .withEtternavn("Brum")
                        .withFornavn("Ole"))
                .withKjoenn(Kjoenn().withKjoenn(Kjoennstyper().withValue("M")))
                .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent(brukerensPin)))
                .withStatsborgerskap(Statsborgerskap().withLand(Landkoder().withValue("NOR")))

        whenever(mockBrukerFromTPS.hentBrukerFraTPS(brukerensPin)).thenReturn(brukeren)

        val actual = prefillNav.prefill(prefillData, true)
        val expected = Nav(
                eessisak = listOf(EessisakItem(someInstitutionId, someIntitutionNavn, somePenSaksnr, "NO")),
                krav = Krav(LocalDate.now().toString()),
                bruker = Bruker(
                        person = Person(
                                pin = listOf(PinItem(
                                        institusjonsnavn = someIntitutionNavn,
                                        institusjonsid = someInstitutionId,
                                        identifikator = brukerensPin,
                                        land = "NO")),
                                statsborgerskap = listOf(StatsborgerskapItem(land = "NO")),
                                etternavn = "Brum",
                                fornavn = "Ole",
                                kjoenn = "M",
                                fornavnvedfoedsel = "Ole"),
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
                        adresse = Adresse(
                                gate = "",
                                bygning = "",
                                by = "",
                                postnummer = "",
                                land = "")))

        assertEquals(expected, actual)
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
}
