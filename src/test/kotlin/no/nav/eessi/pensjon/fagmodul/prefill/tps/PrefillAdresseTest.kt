package no.nav.eessi.pensjon.fagmodul.prefill.tps

import no.nav.eessi.pensjon.services.geo.LandkodeService
import no.nav.eessi.pensjon.services.geo.PostnummerService
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bostedsadresse
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Landkoder
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Postnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class PrefillAdresseTest{

    @Test
    fun `create personAdresse`() {
        val prefillAdresse = PrefillAdresse(PostnummerService(), LandkodeService())

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

        val bostedadr = Bostedsadresse()
        bostedadr.strukturertAdresse = gateadresse

        val person = Person()
        person.bostedsadresse = bostedadr

        val result = prefillAdresse.createPersonAdresse(person)!!

        assertNotNull(result)

        assertEquals("NO", result.land)
        assertEquals("Kirkeveien 12", result.gate)
        assertEquals("OSLO", result.by)
    }}
