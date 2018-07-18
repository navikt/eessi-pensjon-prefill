package no.nav.eessi.eessifagmodul.preutfyll

import no.nav.eessi.eessifagmodul.clients.personv3.PersonV3Client
import no.nav.eessi.eessifagmodul.component.LandkodeService
import no.nav.eessi.eessifagmodul.component.PostnummerService
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

class PreutfyllingPersonFraTPSTest{

    @Mock
    lateinit var mockPersonClient: PersonV3Client

    lateinit var preutfyllingTPS: PreutfyllingPersonFraTPS


    @Before
    fun setup() {
        //logger.debug("Starting tests.... ...")
        //preutfylling = Preutfylling(aktoerIdClient = mockAktoerIdClient, preutfyllingNav = mockPreutfyllingNav, preutfyllingPensjon = mockPreutfyllingPensjon)

        preutfyllingTPS = PreutfyllingPersonFraTPS(mockPersonClient , PostnummerService(), LandkodeService())
    }


//    @Test
//    fun testEnumForeldre() {
//
//        preutfyllingTPS.
//
//    }


}