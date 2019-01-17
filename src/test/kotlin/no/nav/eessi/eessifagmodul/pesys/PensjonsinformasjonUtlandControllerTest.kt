package no.nav.eessi.eessifagmodul.pesys

import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import no.nav.eessi.eessifagmodul.utils.mapJsonToAny
import no.nav.eessi.eessifagmodul.utils.typeRefs
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.springframework.util.ResourceUtils
import java.time.LocalDate
import kotlin.test.*

class PensjonsinformasjonUtlandControllerTest {

    @Mock
    lateinit var controller: PensjonsinformasjonUtlandController

    @Before
    fun bringItOn() {
        controller = PensjonsinformasjonUtlandController()
    }

    @Test
    fun mockPutKravUtland() {
        val buckey = 999
        val resource = ResourceUtils.getFile("classpath:json/pesys/kravutlandalderpen.json").readText()
        val testdata = mapJsonToAny(resource, typeRefs<KravUtland>())

        //feil med ikke godtatt id
        controller.mockPutKravUtland(buckey, testdata)
        val svar = controller.hentKravUtland(buckey)
        assertEquals(testdata, svar)
        val set = controller.mockGetKravUtlandKeys()
        assertEquals(1, set.size)
        assertEquals(buckey, set.first())

        //rydder opp
        controller.mockDeleteKravUtland(buckey)

        //prøver å hente ut som skal feile.
        try {
            controller.hentKravUtland(buckey)
            fail("skal ikke komme hit")
        } catch (ex: Exception) {
            assertTrue(true)
            ex.printStackTrace()
        }
    }

    @Test
    fun mockDeleteKravUtland() {
        //skal være tom, 0 i set
        assertEquals(0, controller.mockGetKravUtlandKeys().size)
        //legger til
        controller.mockPutKravUtland(940, KravUtland())
        //fjerner
        controller.mockDeleteKravUtland(940)
        //skal være tom, 0 i set
        assertEquals(0, controller.mockGetKravUtlandKeys().size)
    }

    @Test
    fun mockGetKravUtlandKeys() {
        val set = controller.mockGetKravUtlandKeys()
        set.forEach {
            println(it)
        }
        assertEquals(0, set.size)
    }

    @Test
    fun hentKravUtlandMockKravUtland() {
        val resource = ResourceUtils.getFile("classpath:json/pesys/kravutlandalderpen.json").readText()
        val testdata = mapJsonToAny(resource, typeRefs<KravUtland>())

        val buckey1 = 910
        controller.mockPutKravUtland(buckey1, testdata)

        val svar1 = controller.hentKravUtland(buckey1)
        assertEquals(testdata, svar1)

        val buckey2 = 920
        controller.mockPutKravUtland(buckey2, testdata)

        val svar2 = controller.hentKravUtland(buckey2)
        assertEquals(testdata, svar2)

        val set = controller.mockGetKravUtlandKeys()
        assertEquals(2, set.size)

        controller.mockDeleteKravUtland(910)
        controller.mockDeleteKravUtland(920)

        assertEquals(0, controller.mockGetKravUtlandKeys().size)
    }

    @Test
    fun hentKravUtlandMockBuc() {
        val response = controller.hentKravUtland(1099)
        assertNotNull(response)
        assertEquals("50", response.uttaksgrad)

        val json = mapAnyToJson(response)
        println(json)


        val response2 = controller.hentKravUtland(1050)
        assertNotNull(response2)

        assertNull(response2.uttaksgrad)

        val json2 = mapAnyToJson(response2)
        println(json2)

    }

    @Test
    fun testLocalDatetilJson() {

        val test = KravUtland(
                mottattDato = LocalDate.now(),
                iverksettelsesdato = LocalDate.now(),
                utland = SkjemaUtland(
                        utlandsopphold = null,
                        harOpphold = false
                ),
                uttaksgrad = "60",
                vurdereTrygdeavtale = false,
                personopplysninger = SkjemaPersonopplysninger(
                        statsborgerskap = "SWE"
                )
        )
        val json = mapAnyToJson(test)
        println(json)


    }

}