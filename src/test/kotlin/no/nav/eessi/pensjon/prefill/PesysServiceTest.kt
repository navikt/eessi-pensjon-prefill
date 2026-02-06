import io.mockk.mockk
import no.nav.eessi.pensjon.prefill.PesysService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PesysServiceTest {

    private val pesysService = PesysService(mockk(relaxed = true))

    @Test
    fun `buildUri legger til alle parameter`() {
//        val method = pesysService.javaClass.getDeclaredMethod(
//            "buildUri",
//            String::class.java,
//            Array<Any>::class.java
//        ).apply { isAccessible = true }

            val uri = pesysService.buildUri(
            "/sed/p2000",
            arrayOf(
                "vedtaksId" to "123",
                "fnr" to "456",
                "sakId" to "789"
            )
        )

        assertEquals("/sed/p2000?vedtaksId=123&fnr=456&sakId=789", uri.toString())
    }

//    @Test
//    fun `leggTilParameter filterer null eller tommer parameter`() {
//        val url = pesysService.javaClass.getDeclaredMethod(
//            "get", String::class.java, List::class.java
//        ).apply { isAccessible = true }
//            .invoke(pesysService, "/sed/p6000", listOf(
//                "vedtaksId" to null,
//                "sakId" to ""
//            )) as String
//
//        assertEquals("/sed/p6000", url)
//    }
}