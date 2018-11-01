package no.nav.eessi.eessifagmodul.prefill.person

import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


@RunWith(MockitoJUnitRunner::class)
class PrefillPersonEnkeTest : PersonDataFromTPS(
        setOf(
                MockTPS("Person-20000.json", "02345678901"),
                MockTPS("Person-21000.json", "22345678901"),
                MockTPS("Person-22000.json", "12345678901")
        )) {

    @Test
    fun `create birthplace as unknown`() {
        val bruker = Bruker()
        bruker.foedested = null

        val result = preutfyllingTPS.hentFodested(bruker)

        assertNotNull(result)
        println(result)
        assertEquals(null, result.land)

    }
    @Test
    fun `create correct birthplace known`() {
        val bruker = Bruker()
        bruker.foedested = "NOR"

        val result = preutfyllingTPS.hentFodested(bruker)

        assertNotNull(result)
        assertEquals("NOR", result.land)

    }

    @Test
    fun `create personAdresse`() {
        val landkode = Landkoder()
        landkode.value = "NOR"
        val postnr = Postnummer()
        postnr.value = "0123"
        val gateadresse = Gateadresse()
        gateadresse.husnummer = 12
        gateadresse.kommunenummer = "120"
        gateadresse.gatenavn = "Kirkeveien"
        gateadresse.landkode = landkode
        gateadresse.poststed = postnr

        //val struktadr = StrukturertAdresse()
        //struktadr.
        val bostedadr = Bostedsadresse()
        bostedadr.strukturertAdresse = gateadresse

        val person = Person()
        person.bostedsadresse = bostedadr

        val result = preutfyllingTPS.personAdresse(person)
        println(result)

        assertNotNull(result)

        assertEquals("NO", result.land)
        assertEquals( "Kirkeveien 12", result.gate)
        assertEquals("OSLO", result.by)
    }

    @Test
    fun `forvent utfylling av person data av ENKE fra TPS P2000`() {
        //val prefillNav = PrefillNav(initPersonDataMedMockResponse("Person-20000.json"))
        val prefillData = generatePrefillData("P2000", "02345678901")
        val response = prefillNav.prefill(prefillData)

        val sed = prefillData.sed
        sed.nav = response

        assertEquals("JESSINE TORDNU", sed.nav?.bruker?.person?.fornavn)
        assertEquals("BOUWMANS", sed.nav?.bruker?.person?.etternavn)
        assertEquals("f", sed.nav?.bruker?.person?.kjoenn)
        assertEquals(0, sed.nav?.barn?.size)

    }

    @Test
    fun `forvent utfylling av person data av ENKE fra TPS P2100`() {
        //val prefillNav = PrefillNav(initPersonDataMedMockResponse("Person-20000.json"))
        val prefillData = generatePrefillData("P2100", "02345678901")
        val response = prefillNav.prefill(prefillData)

        val sed = prefillData.sed
        sed.nav = response

        assertEquals("JESSINE TORDNU", sed.nav?.bruker?.person?.fornavn)
        assertEquals("BOUWMANS", sed.nav?.bruker?.person?.etternavn)
        assertEquals("f", sed.nav?.bruker?.person?.kjoenn)
        assertEquals("ENKE", sed.nav?.bruker?.person?.sivilstand?.get(0)?.status)

        assertEquals(2, sed.nav?.barn?.size)

        val resultBarn = sed.nav?.barn

        val item1 = resultBarn.orEmpty().get(0)
        assertEquals("BOUWMANS", item1.person?.etternavn)
        assertEquals("TOPPI DOTTO", item1.person?.fornavn)

        val item2 = resultBarn.orEmpty().get(1)
        assertEquals("BOUWMANS", item2.person?.etternavn)
        assertEquals("EGIDIJS MASKOT", item2.person?.fornavn)

    }

    @Test
    fun `forvent utfylling av person data av ENKE fra TPS P2200`() {
        //val prefillNav = PrefillNav(initPersonDataMedMockResponse("Person-20000.json"))
        val prefillData = generatePrefillData("P2200", "02345678901")
        val response = prefillNav.prefill(prefillData)

        val sed = prefillData.sed
        sed.nav = response

        assertEquals("JESSINE TORDNU", sed.nav?.bruker?.person?.fornavn)
        assertEquals("BOUWMANS", sed.nav?.bruker?.person?.etternavn)
        assertEquals("f", sed.nav?.bruker?.person?.kjoenn)
        assertEquals(0, sed.nav?.barn?.size)

        assertEquals("P2200", sed.sed)

    }



}