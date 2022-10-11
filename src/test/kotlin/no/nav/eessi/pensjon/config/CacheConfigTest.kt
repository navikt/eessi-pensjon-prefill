package no.nav.eessi.pensjon.config

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.kodeverk.PostnummerService
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.person.PrefillPDLAdresse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean

@SpringBootTest(classes = [UnsecuredWebMvcTestLauncher::class, CacheConfig::class, CacheConfigTest.Config::class])
class CacheConfigTest {

    @Autowired
    lateinit var cacheManager: CacheManager

    @MockkBean
    lateinit var innhentingService: InnhentingService

    @Autowired
    lateinit var kodeverkClient: KodeverkClient

    @Autowired
    lateinit var prefillPDLAdresse: PrefillPDLAdresse

    @Test
    fun `Gitt et kall til finnLandkode saa skal den kun hente fra kodeverkClient en gang, og deretter fra cache`() {
        every { kodeverkClient.finnLandkode("NO") } returns "NOR"

        prefillPDLAdresse.hentLandkode("NO")
        prefillPDLAdresse.hentLandkode("NO")

        verify (exactly = 1) { kodeverkClient.finnLandkode(eq("NO"))  }

        val cachedLandkode = cacheManager.getCache("landkoder")?.get("NO")?.get()
        assertEquals("NOR", cachedLandkode)
    }


    @TestConfiguration
    class Config() {

        @Bean
        fun kodeverkClient(): KodeverkClient {
            return mockk(relaxed = true)
        }

        @Bean
        fun prefillPDLAdresse(): PrefillPDLAdresse {
            return PrefillPDLAdresse(PostnummerService(), kodeverkClient(), mockk()).apply { initMetrics() }
        }
    }
}