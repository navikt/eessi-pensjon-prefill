package no.nav.eessi.pensjon.fagmodul.pesys

import no.nav.eessi.pensjon.services.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.typeRefs
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.util.ResourceUtils
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class PensjonsinformasjonUtlandControllerTest {

    lateinit var controller: PensjonsinformasjonUtlandController

    @Mock
    lateinit var kodeverkClient: KodeverkClient

    lateinit var pensjonsinformasjonUtlandService : PensjonsinformasjonUtlandService

    @BeforeEach
    fun setup() {
        pensjonsinformasjonUtlandService = PensjonsinformasjonUtlandService(kodeverkClient)
        controller = PensjonsinformasjonUtlandController(pensjonsinformasjonUtlandService)
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
        assertThrows<NoSuchElementException> {
            controller.hentKravUtland(buckey)
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
    fun hentKravUtlandManglerUttaksgradMockBuc() {
        val response = controller.hentKravUtland(1050)
        assertNotNull(response)
        assertNull(response.uttaksgrad)
    }


    @Test
    fun testLocalDatetilJson() {

        val test = KravUtland(
                mottattDato = LocalDate.now(),
                iverksettelsesdato = LocalDate.now(),
                utland = SkjemaUtland(
                        utlandsopphold = null
                ),
                uttaksgrad = "60",
                vurdereTrygdeavtale = false,
                personopplysninger = SkjemaPersonopplysninger(
                        statsborgerskap = "SWE"
                )
        )
        val json = mapAnyToJson(test)
        assertNotNull(json)
    }
}
