package no.nav.eessi.pensjon.fagmodul.prefill.tps

import no.nav.eessi.pensjon.services.geo.LandkodeService
import no.nav.eessi.pensjon.services.geo.PostnummerService
import no.nav.eessi.pensjon.services.personv3.BrukerMock
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PrefillAdresseTest{

    lateinit var prefillAdresse: PrefillAdresse

    @BeforeEach
    fun beforeStart() {
        prefillAdresse = PrefillAdresse(PostnummerService(), LandkodeService())
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

        val bostedadr = Bostedsadresse()
        bostedadr.strukturertAdresse = gateadresse

        val person = Person()
        person.bostedsadresse = bostedadr

        val result = prefillAdresse.createPersonAdresse(person)!!

        assertNotNull(result)

        assertEquals("NO", result.land)
        assertEquals("Kirkeveien 12", result.gate)
        assertEquals("OSLO", result.by)
    }

    @Test
    fun adresseFeltDiskresjonFortroligPerson() {
        val bruker = BrukerMock.createWith()
        bruker?.diskresjonskode = Diskresjonskoder().withValue("SPFO")

        val acual = prefillAdresse.createPersonAdresse(bruker ?: Bruker())

        assertEquals(null, acual)

    }

    @Test
    fun adresseFeltDiskresjonStrengtFortoligPerson() {
        val bruker = BrukerMock.createWith()
        bruker?.diskresjonskode = Diskresjonskoder().withValue("SPSF")

        val acual = prefillAdresse.createPersonAdresse(bruker ?: Bruker())

        assertEquals(null, acual)

    }


}
