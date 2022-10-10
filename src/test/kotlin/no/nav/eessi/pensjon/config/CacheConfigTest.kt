package no.nav.eessi.pensjon.config

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.logging.RequestIdHeaderInterceptor
import no.nav.eessi.pensjon.logging.RequestResponseLoggerInterceptor
import no.nav.eessi.pensjon.pensjonsinformasjon.clients.PensjonsinformasjonClient
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PensjonsinformasjonService
import no.nav.eessi.pensjon.prefill.person.PrefillPDLAdresse
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate


@SpringBootTest(classes = [UnsecuredWebMvcTestLauncher::class, CacheConfig::class] )
@ActiveProfiles("excludeKodeverk")
class CacheConfigTest {

        @Autowired
        lateinit var cacheManager: CacheManager

        @MockkBean
        lateinit var innhentingService: InnhentingService

        @MockkBean
        lateinit var prefillSedService: PrefillSEDService

//        @MockkBean
//        lateinit var prefillPDLAdresse: PrefillPDLAdresse

        @MockkBean
        lateinit var pensjoninformasjonService: PensjonsinformasjonService

        @MockkBean
        lateinit var pensjonsinformasjonsClient: PensjonsinformasjonClient


        val kodeverkClient: KodeverkClient = mockk(relaxed = true)
        val prefillPDLAdresse = PrefillPDLAdresse(mockk(), kodeverkClient, mockk())

        @Test
        fun `Sjekker cache`() {
                every { kodeverkClient.finnLandkode("NO") } returns "NOR"

                prefillPDLAdresse.hentLandkode("NO")
                val resultcache = cacheManager.getCache("landkoder")
                println(cacheManager.cacheNames)

                Assertions.assertEquals("NOR", resultcache?.get("NO")!!)

        }


        @TestConfiguration
        class Config() {

                @Primary
                @Bean
                fun restTemplate(): RestTemplate {
                        return RestTemplateBuilder()
                                .rootUri("localhost")
                                .errorHandler(DefaultResponseErrorHandler())
                                .additionalInterceptors(
                                        RequestIdHeaderInterceptor(),
                                        RequestResponseLoggerInterceptor(),
                                )
                                .build().apply {
                                        requestFactory =
                                                BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory())
                                }
                }
        }


}