package no.nav.eessi.pensjon.fagmodul.prefill.tps

import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjon
import no.nav.eessi.pensjon.services.geo.LandkodeService
import no.nav.eessi.pensjon.services.geo.PostnummerService
import no.nav.eessi.pensjon.services.personv3.PersonV3Service
import no.nav.eessi.pensjon.utils.createXMLCalendarFromString
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Sivilstand
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Sivilstander
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class PrefillPersonDataFromTPSTest{

    @Mock
    private lateinit var personV3Service: PersonV3Service

    private lateinit var prefillPersonFromTPS: PrefillPersonDataFromTPS

    @BeforeEach
    fun bringItOnDude() {
        prefillPersonFromTPS = PrefillPersonDataFromTPS(personV3Service)
    }


}
