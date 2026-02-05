import io.mockk.mockk
import no.nav.eessi.pensjon.prefill.PesysService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PesysServiceTest {

    private val pesysService = PesysService(mockk(relaxed = true))

    @Test
    fun `leggTilParameter legger til alle parameter`() {
        val url = pesysService.javaClass.getDeclaredMethod(
            "leggTilParameter", String::class.java, List::class.java
        ).apply { isAccessible = true }
            .invoke(pesysService, "/sed/p2000", listOf(
                "vedtaksId" to "123",
                "fnr" to "456",
                "sakId" to "789"
            )) as String

        assertEquals("/sed/p2000?vedtaksId=123&fnr=456&sakId=789", url)
    }

    @Test
    fun `leggTilParameter filterer null eller tommer parameter`() {
        val url = pesysService.javaClass.getDeclaredMethod(
            "leggTilParameter", String::class.java, List::class.java
        ).apply { isAccessible = true }
            .invoke(pesysService, "/sed/p6000", listOf(
                "vedtaksId" to null,
                "sakId" to ""
            )) as String

        assertEquals("/sed/p6000", url)
    }
}