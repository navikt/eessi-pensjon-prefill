package no.nav.eessi.pensjon.config

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.eessi.pensjon.kodeverk.KodeVerkHentLandkoder
import no.nav.eessi.pensjon.kodeverk.KodeverkCacheConfig
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.kodeverk.PostnummerService
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.person.PrefillPDLAdresse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.web.client.RestTemplate

@SpringJUnitConfig(classes = [PrefillAdresseKodeverkTest.Config::class, KodeverkCacheConfig::class])
class PrefillAdresseKodeverkTest {

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
    fun `henting av en landkode fra kodeverk skal hente alle land fra kodeverkClient og neste kall fra cache`() {
        prefillPDLAdresse.hentLandkode("SE")
        prefillPDLAdresse.hentLandkode("NO")
        prefillPDLAdresse.hentLandkode("SE")
        prefillPDLAdresse.hentLandkode("NO")

        val cache = cacheManager.getCache("kodeverk") as ConcurrentMapCache
        assertEquals("[hentLandKoder=[Landkode(landkode2=NO, landkode3=NOR), Landkode(landkode2=SE, landkode3=SWE)]]", cache.nativeCache.entries.toString())

        //kun ett kall til utsiden
        verify (exactly = 1) { restTemplate
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
    }

    @TestConfiguration
    class Config {
        @Bean
        fun kodeVerkHentLandkoder(): KodeVerkHentLandkoder {
            return KodeVerkHentLandkoder("testApp", restTemplate, MetricsHelper.ForTest())
        }
        @Bean
        fun kodeverkClient(): KodeverkClient {
            return KodeverkClient(kodeVerkHentLandkoder())
        }

        @Bean
        fun prefillPDLAdresse(): PrefillPDLAdresse {
            return PrefillPDLAdresse(PostnummerService(), kodeverkClient(), mockk())
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