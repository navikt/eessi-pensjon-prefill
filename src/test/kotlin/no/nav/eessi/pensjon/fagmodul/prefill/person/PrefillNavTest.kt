package no.nav.eessi.pensjon.fagmodul.prefill.person

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.nav.eessi.pensjon.fagmodul.prefill.model.BrukerInformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.tps.PrefillAdresse
import no.nav.eessi.pensjon.fagmodul.prefill.tps.PrefillPersonDataFromTPS
import no.nav.eessi.pensjon.fagmodul.sedmodel.*
import no.nav.eessi.pensjon.fagmodul.sedmodel.Bruker
import no.nav.eessi.pensjon.fagmodul.sedmodel.Person
import no.nav.eessi.pensjon.services.geo.LandkodeService
import no.nav.eessi.pensjon.services.geo.PostnummerService
import no.nav.eessi.pensjon.utils.createXMLCalendarFromString
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import java.time.LocalDate

internal class PrefillNavTest {

    @Test
    fun `prefill med tom respons fra TPS`() {
        val mockPrefillPersonDataFromTPS = mock<PrefillPersonDataFromTPS>()
        val someInstitutionId = "enInstId"
        val someIntitutionNavn = "instNavn"
        val prefillNav = PrefillNav(mockPrefillPersonDataFromTPS, PrefillAdresse(PostnummerService(), LandkodeService()), someInstitutionId, someIntitutionNavn)
        val somePenSaksnr = "somePenSaksnr"
        val somePersonNr = "somePersonNr"
        val prefillData = PrefillDataModel().apply {
            penSaksnummer = somePenSaksnr
            personNr = somePersonNr
        }

        val actual = prefillNav.prefill(prefillData, true)
        val expected = Nav(
                eessisak = listOf(EessisakItem(someInstitutionId, someIntitutionNavn, somePenSaksnr, "NO")),
                krav = Krav(LocalDate.now().toString()))
        assertEquals(expected, actual)
    }

    @Test @Disabled
    fun `prefill med barn`() {
        val mockPrefillPersonDataFromTPS = mock<PrefillPersonDataFromTPS>()
        val someInstitutionId = "enInstId"
        val someIntitutionNavn = "instNavn"
        val prefillNav = PrefillNav(mockPrefillPersonDataFromTPS, PrefillAdresse(PostnummerService(), LandkodeService()), someInstitutionId, someIntitutionNavn)
        val somePenSaksnr = "somePenSaksnr"
        val somePersonNr = "somePersonNr"
        val prefillData = PrefillDataModel().apply {
            penSaksnummer = somePenSaksnr
            personNr = somePersonNr

        }
        val someBarnPin = "BarnePin"

        val brukerMedBarnSomHarSomeBarnPin = no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker()
        // FIXME her må vi putte inn relasjon til barn

        whenever(mockPrefillPersonDataFromTPS.hentBrukerFraTPS(somePersonNr)).thenReturn(brukerMedBarnSomHarSomeBarnPin)
        val barnBruker = no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker()
        // FIXME her må vi putte noe om barnet _ kanskje

        whenever(mockPrefillPersonDataFromTPS.hentBrukerFraTPS(someBarnPin)).thenReturn(barnBruker)

        val actual = prefillNav.prefill(prefillData, true)
        val expected = Nav(
                eessisak = listOf(EessisakItem(someInstitutionId, someIntitutionNavn, somePenSaksnr, "NO")),
                krav = Krav(LocalDate.now().toString()),
                barn = listOf(BarnItem(relasjontilbruker = "BARN")))

        assertEquals(expected, actual)
    }

    @Test
    fun `prefill med brukerinfo på request`() {
        val mockPrefillPersonDataFromTPS = mock<PrefillPersonDataFromTPS>()
        val someInstitutionId = "enInstId"
        val someIntitutionNavn = "instNavn"
        val prefillNav = PrefillNav(mockPrefillPersonDataFromTPS, PrefillAdresse(PostnummerService(), LandkodeService()), someInstitutionId, someIntitutionNavn)
        val somePenSaksnr = "somePenSaksnr"
        val somePersonNr = "somePersonNr"
        val prefillData = PrefillDataModel().apply {
            penSaksnummer = somePenSaksnr
            personNr = somePersonNr
            partSedAsJson["PersonInfo"] = mapAnyToJson(BrukerInformasjon(null,null,null,null,null,"Nordnb",null,null,null,null,null,null,null,null,null,null,null,null))
        }

        val someBruker = no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker()
                .withPersonnavn(Personnavn()
                        .withEtternavn("Brum")
                        .withFornavn("Ole"))
                .withKjoenn(Kjoenn().withKjoenn(Kjoennstyper().withValue("M")))
                .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent(somePersonNr)))
                .withStatsborgerskap(Statsborgerskap().withLand(Landkoder().withValue("NOR")))

        whenever(mockPrefillPersonDataFromTPS.hentBrukerFraTPS(somePersonNr)).thenReturn(someBruker)

        val actual = prefillNav.prefill(prefillData, true)
        val expected = Nav(
                eessisak = listOf(EessisakItem(someInstitutionId, someIntitutionNavn, somePenSaksnr, "NO")),
                krav = Krav(LocalDate.now().toString()),
                bruker = Bruker(
                        person = Person(
                                pin = listOf(PinItem(
                                        institusjonsnavn = someIntitutionNavn,
                                        institusjonsid = someInstitutionId,
                                        identifikator = somePersonNr,
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
