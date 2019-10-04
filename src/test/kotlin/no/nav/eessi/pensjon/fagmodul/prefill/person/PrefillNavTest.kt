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
}