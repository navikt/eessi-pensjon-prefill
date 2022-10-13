package no.nav.eessi.pensjon.config

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.kodeverk.KodeverkCacheConfig
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.kodeverk.PostnummerService
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.person.PrefillPDLAdresse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

@SpringBootTest(classes = [UnsecuredWebMvcTestLauncher::class, CacheConfigTest.Config::class, KodeverkCacheConfig::class])
class CacheConfigTest {

    @Autowired
    lateinit var cacheManager: ConcurrentMapCacheManager

    @MockkBean
    lateinit var innhentingService: InnhentingService

    @Autowired
    lateinit var prefillPDLAdresse: PrefillPDLAdresse

    @BeforeEach
    fun setup() {
        every {
            restTemplate
            .exchange(
                eq("/api/v1/hierarki/LandkoderSammensattISO2/noder"),
                any(),
                any<HttpEntity<Unit>>(),
                eq(String::class.java)
            )
        } returns ResponseEntity(landkoderNoSE().trimIndent(), HttpStatus.OK)
    }

    @Test
    fun `Gitt et kall til finnLandkode saa skal den kun hente fra kodeverkClient kun en gang pr land, og deretter fra cache`() {
        prefillPDLAdresse.hentLandkode("SE")
        prefillPDLAdresse.hentLandkode("NO")
        prefillPDLAdresse.hentLandkode("SE")
        prefillPDLAdresse.hentLandkode("NO")

        val cache = cacheManager.getCache("kodeverk") as ConcurrentMapCache
        assertEquals("[SE=SWE, NO=NOR]", cache.nativeCache.entries.toString())

        //et kall for NO, ett for SE
        verify (exactly = 2) { restTemplate
            .exchange(
                eq("/api/v1/hierarki/LandkoderSammensattISO2/noder"),
                any(),
                any<HttpEntity<Unit>>(),
                eq(String::class.java)
            )
        }
    }

    companion object {
        var restTemplate: RestTemplate = mockk()
        var kodeverkClient = KodeverkClient(restTemplate, "testApp")
    }

    @TestConfiguration
    class Config {
        @Bean
        fun kodeverkClient(): KodeverkClient {
            return KodeverkClient(restTemplate, "testApp")
        }

        @Bean
        fun prefillPDLAdresse(): PrefillPDLAdresse {
            return PrefillPDLAdresse(PostnummerService(), kodeverkClient(), mockk()).apply { initMetrics() }
        }
    }

    private fun landkoderNoSE() = """
        {
          "hierarkinivaaer": [
            "LandkoderISO2",
            "Landkoder"
          ],
          "noder": {
              "SE": {
                  "kode": "SE",
                  "undernoder": {
                    "SWE": {
                      "kode": "SWE"
                    }
                  }
              },
              "NO": {
              "kode": "NO",
              "undernoder": {
                "NOR": {
                  "kode": "NOR"
                }
              }                    
            }
          }           
       }              
    """
}