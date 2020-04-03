package no.nav.eessi.pensjon.fagmodul.pesys

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import no.nav.eessi.pensjon.services.kodeverk.KodeverkKlient
import no.nav.eessi.pensjon.utils.mapAnyToJson
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class PensjonsinformasjonUtlandServiceTest {

    @Mock
    lateinit var kodeverkKlient: KodeverkKlient

    lateinit var service : PensjonsinformasjonUtlandService

    @BeforeEach
    fun setup() {
        service = PensjonsinformasjonUtlandService(kodeverkKlient)
    }

    @Test
    fun hentKravUtlandMockBuc() {
        doReturn("SWE").whenever(kodeverkKlient).finnLandkode3("SE")
        doReturn("DEU").whenever(kodeverkKlient).finnLandkode3("DE")
        doReturn("DNK").whenever(kodeverkKlient).finnLandkode3("DK")


        val response = service.hentKravUtland(1099)
        assertNotNull(response)
        assertEquals("50", response.uttaksgrad)
        assertEquals("2019-03-11", response.mottattDato.toString())
        assertEquals("SWE", response.personopplysninger?.statsborgerskap)
        assertEquals("SWE", response.soknadFraLand)
        assertEquals(true, response.vurdereTrygdeavtale)

        assertEquals("BRUKER", response.initiertAv)

        assertEquals("UGIF", response.sivilstand?.valgtSivilstatus)

        val utland = response.utland
        assertEquals(3, utland?.utlandsopphold?.size)

        val utlandEn = utland?.utlandsopphold?.get(0)
        assertEquals("DEU", utlandEn?.land)
        assertEquals("1960-01-01", utlandEn?.fom.toString())
        assertEquals("1965-01-01", utlandEn?.tom.toString())
        assertEquals(false, utlandEn?.bodd)
        assertEquals(true, utlandEn?.arbeidet)
        assertEquals("10010101010", utlandEn?.utlandPin)

        val utlandTo = utland?.utlandsopphold?.get(1)
        assertEquals("DNK", utlandTo?.land)
        assertEquals("2003-01-01", utlandTo?.fom.toString())
        assertEquals("2004-01-01", utlandTo?.tom.toString())
        assertEquals(true, utlandTo?.bodd)
        assertEquals(false, utlandTo?.arbeidet)
        assertEquals("23456789001", utlandTo?.utlandPin)

        val utlandTre = utland?.utlandsopphold?.get(2)
        assertEquals("DNK", utlandTre?.land)
        assertEquals("2002-01-01", utlandTre?.fom.toString())
        assertEquals(null, utlandTre?.tom)
        assertEquals(true, utlandTre?.bodd)
        assertEquals(false, utlandTre?.arbeidet)
        assertEquals("23456789001", utlandTre?.utlandPin)

        val json = mapAnyToJson(response)
        assertNotNull(json)
    }
}
