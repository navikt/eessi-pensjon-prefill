import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.eessi.pensjon.prefill.PesysService
import no.nav.eessi.pensjon.prefill.models.pensjon.P15000overfoeringAvPensjonssakerTilEessiDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P6000MeldingOmVedtakDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P8000AnmodningOmTilleggsinformasjon
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate

class PesysServiceTest {

    private lateinit var restTemplate: RestTemplate
    private lateinit var server: MockRestServiceServer
    private lateinit var pesysService: PesysService

    @BeforeEach
    fun setup() {
        restTemplate = RestTemplate()
        server = MockRestServiceServer.bindTo(restTemplate).build()
        pesysService = PesysService(restTemplate)
    }

    @Nested
    inner class HentP2x00FellesVerdier {

        @ParameterizedTest(name = "sed:{0}}")
        @CsvSource(
            value = ["p2000", "p2100", "p2200"], nullValues = ["null"]
        )
        fun `henter verdier for `(sed: String?) {
            server.expect(requestTo("/sed/$sed"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("vedtakId", "123"))
                .andExpect(header("fnr", "456"))
                .andExpect(header("sakId", "789"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON)) // empty body => null DTO
            val result = when (sed) {
                "p2000" -> pesysService.hentP2000data("123", "456", "789")
                "p2100" -> pesysService.hentP2100data("123", "456", "789")
                "p2200" -> pesysService.hentP2200data("123", "456", "789")
                else -> {
                    assert(false) { "Ugyldig sed-verdi i test: $sed" }
                    null
                }
            }
            assertNull(result)
            server.verify()
        }
    }

    @Nested
    inner class HentP2000Verdier {

        @Test
        fun `hentP2000data sender ikke vedtakId header naar den er null`() {
            server.expect(requestTo("/sed/p2000"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(headerDoesNotExist("vedtakId"))
                .andExpect(header("fnr", "456"))
                .andExpect(header("sakId", "789"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON))

            val result = pesysService.hentP2000data(null, "456", "789")

            assertNull(result)
            server.verify()
        }

        @Test
        fun `hentP2000data mapper alle verdier fra p2000-alder json til P2xxxMeldingOmPensjonDto`() {
            val p2000Json = javaClass.getResource("/pesys-endepunkt-2026/p2000-pesys.json")!!.readText()
            server.expect(requestTo("/sed/p2000"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(headerDoesNotExist("vedtakId"))
                .andExpect(header("fnr", "456"))
                .andExpect(header("sakId", "789"))
                .andRespond(withSuccess(p2000Json, MediaType.APPLICATION_JSON))

            val result = pesysService.hentP2000data("", "456", "789")
            // vedtak
            assert(result?.vedtak?.boddArbeidetUtland == true)
            // sak
            assert(result?.sak?.sakType?.name == "ALDER")
            assert(result?.sak?.forsteVirkningstidspunkt.toString() == "2025-01-01")
            assert(result?.sak?.status?.name == "LOPENDE")
            // kravHistorikk
            val kravHistorikk = result?.sak?.kravHistorikk
            assert(kravHistorikk?.size == 3)
            assert(kravHistorikk?.first()?.kravId == "49256020")
            assert(kravHistorikk?.get(2)?.kravStatus?.name == "TIL_BEHANDLING")
            assert(kravHistorikk?.get(2)?.kravAarsak?.name == "ANNEN_ARSAK")
            // ytelsePerMaaned
            val ytelsePerMaaned = result?.sak?.ytelsePerMaaned
            assert(ytelsePerMaaned?.size == 2)
            assert(ytelsePerMaaned?.first()?.belop == 5057)
            assert(ytelsePerMaaned?.get(1)?.ytelseskomponent?.get(2)?.ytelsesKomponentType == "TP")
            assert(ytelsePerMaaned?.get(1)?.ytelseskomponent?.get(2)?.belopTilUtbetaling == 2160)
            server.verify()
        }
    }

    @Nested
    inner class HentP2100Verdier {

    }

    @Nested
    inner class Hent6000Verdier {
        @Test
        fun `hentP6000 med flere vedtak`() {
            server.expect(requestTo("/sed/p6000"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("sakId", "789"))
                .andRespond(withSuccess(p6000FlereVedtakJson(), MediaType.APPLICATION_JSON))

            val result = pesysService.hentP6000data("789")

            assertNotNull(result)
            assertEquals("2025-10-10", result?.vedtak?.datoFattetVedtak.toString())
            server.verify()
        }
    }

    @Nested
    inner class Hent15000Verdier {
        @Test
        fun `hentP15000 med flere vedtak skal hente ihht prioritert sortering`() {
            val avdodListeJson = """
            [ {
                "sakType" : null,
                "avdod" : null,
                "avdodMor" : null,
                "avdodFar" : null
              },{
                "sakType" : null,
                "avdod" : null,
                "avdodMor" : "111111111",
                "avdodFar" : null
              },{
                "sakType" : null,
                "avdod" : null,
                "avdodMor" : "2131232321",
                "avdodFar" : "3432434234"
              }              ]
            """.trimIndent()

            server.expect(requestTo("/sed/p15000"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("sakId", "789"))
                .andRespond(withSuccess(avdodListeJson, MediaType.APPLICATION_JSON))

            val result = pesysService.hentP15000data("789")

            assert(result == P15000overfoeringAvPensjonssakerTilEessiDto(sakType=null, avdod=null, avdodMor="2131232321", avdodFar="3432434234"))
            server.verify()
        }
    }

    @Test
    fun `hentP2000data `() {
        assertNotNull(pesysService.p2xxxFraListe(alderJson().trimIndent()))
        assertNotNull(pesysService.p2xxxFraListe("""[${alderJson()}]""".trimIndent()))
    }

    fun alderJson() = """
         {
              "vedtak" : null,
              "sak" : {
                "sakType" : "ALDER",
                "forsteVirkningstidspunkt" : null,
                "kravHistorikk" : [ {
                  "kravId" : 49609017,
                  "kravType" : "F_BH_MED_UTL",
                  "mottattDato" : "2025-10-01",
                  "virkningstidspunkt" : "2025-11-01",
                  "kravAarsak" : "NY_SOKNAD",
                  "kravStatus" : "TIL_BEHANDLING"
                } ],
                "ytelsePerMaaned" : [ ],
                "status" : "TIL_BEHANDLING"
              }
            }
    """.trimIndent()


    fun p6000FlereVedtakJson() = """
        [ {
          "avdod" : {
            "avdodPid" : null,
            "avdodBoddArbeidetUtland" : false,
            "farBoddArbeidetUtland" : false,
            "morBoddArbeidetUtland" : false
          },
          "sakType" : "ALDER",
          "trygdeavtale" : {
            "erArt10BruktGP" : false,
            "erArt10BruktTP" : false
          },
          "trygdetid" : [ {
            "fom" : "1987-03-15",
            "tom" : "2020-11-15"
          } ],
          "vedtak" : {
            "virkningstidspunkt" : "2025-01-01",
            "kravGjelder" : "REVURD",
            "hovedytelseTrukket" : false,
            "boddArbeidetUtland" : true,
            "datoFattetVedtak" : null
          },
          "vilkarsvurdering" : [ {
            "fom" : "2025-01-01",
            "vilkarsvurderingUforetrygd" : null,
            "resultatHovedytelse" : "INNV",
            "harResultatGjenlevendetillegg" : false,
            "avslagHovedytelse" : null
          } ],
          "ytelsePerMaaned" : [ {
            "fom" : "2025-01-01",
            "tom" : "2025-04-30",
            "mottarMinstePensjonsniva" : false,
            "belop" : 5057,
            "ytelseskomponenter" : [ {
              "ytelsesKomponentType" : "IP",
              "belopTilUtbetaling" : 1986
            }, {
              "ytelsesKomponentType" : "GP",
              "belopTilUtbetaling" : 1004
            }, {
              "ytelsesKomponentType" : "TP",
              "belopTilUtbetaling" : 2067
            } ]
          }, {
            "fom" : "2025-05-01",
            "tom" : null,
            "mottarMinstePensjonsniva" : false,
            "belop" : 5286,
            "ytelseskomponenter" : [ {
              "ytelsesKomponentType" : "TP",
              "belopTilUtbetaling" : 2160
            }, {
              "ytelsesKomponentType" : "IP",
              "belopTilUtbetaling" : 2076
            }, {
              "ytelsesKomponentType" : "GP",
              "belopTilUtbetaling" : 1050
            } ]
          } ]
        }, {
          "avdod" : {
            "avdodPid" : null,
            "avdodBoddArbeidetUtland" : false,
            "farBoddArbeidetUtland" : false,
            "morBoddArbeidetUtland" : false
          },
          "sakType" : "ALDER",
          "trygdeavtale" : {
            "erArt10BruktGP" : false,
            "erArt10BruktTP" : false
          },
          "trygdetid" : [ {
            "fom" : "1987-03-15",
            "tom" : "2020-11-15"
          } ],
          "vedtak" : {
            "virkningstidspunkt" : "2025-01-01",
            "kravGjelder" : "F_BH_MED_UTL",
            "hovedytelseTrukket" : false,
            "boddArbeidetUtland" : true,
            "datoFattetVedtak" : "2025-02-10"
          },
          "vilkarsvurdering" : [ {
            "fom" : "2025-01-01",
            "vilkarsvurderingUforetrygd" : null,
            "resultatHovedytelse" : "INNV",
            "harResultatGjenlevendetillegg" : false,
            "avslagHovedytelse" : null
          } ],
          "ytelsePerMaaned" : [ {
            "fom" : "2025-01-01",
            "tom" : "2025-04-30",
            "mottarMinstePensjonsniva" : false,
            "belop" : 5057,
            "ytelseskomponenter" : [ {
              "ytelsesKomponentType" : "IP",
              "belopTilUtbetaling" : 1986
            }, {
              "ytelsesKomponentType" : "GP",
              "belopTilUtbetaling" : 1004
            }, {
              "ytelsesKomponentType" : "TP",
              "belopTilUtbetaling" : 2067
            } ]
          }, {
            "fom" : "2025-05-01",
            "tom" : null,
            "mottarMinstePensjonsniva" : false,
            "belop" : 5286,
            "ytelseskomponenter" : [ {
              "ytelsesKomponentType" : "IP",
              "belopTilUtbetaling" : 2076
            }, {
              "ytelsesKomponentType" : "GP",
              "belopTilUtbetaling" : 1050
            }, {
              "ytelsesKomponentType" : "TP",
              "belopTilUtbetaling" : 2160
            } ]
          } ]
        } ,        
        {
          "avdod" : {
            "avdodPid" : null,
            "avdodBoddArbeidetUtland" : false,
            "farBoddArbeidetUtland" : false,
            "morBoddArbeidetUtland" : false
          },
          "sakType" : "ALDER",
          "trygdeavtale" : {
            "erArt10BruktGP" : false,
            "erArt10BruktTP" : false
          },
          "trygdetid" : [ {
            "fom" : "1987-03-15",
            "tom" : "2020-11-15"
          } ],
          "vedtak" : {
            "virkningstidspunkt" : "2025-01-01",
            "kravGjelder" : "F_BH_MED_UTL",
            "hovedytelseTrukket" : false,
            "boddArbeidetUtland" : true,
            "datoFattetVedtak" : "2025-10-10"
          },
          "vilkarsvurdering" : [ {
            "fom" : "2025-01-01",
            "vilkarsvurderingUforetrygd" : null,
            "resultatHovedytelse" : "INNV",
            "harResultatGjenlevendetillegg" : false,
            "avslagHovedytelse" : null
          } ],
          "ytelsePerMaaned" : [ {
            "fom" : "2025-01-01",
            "tom" : "2025-04-30",
            "mottarMinstePensjonsniva" : false,
            "belop" : 5057,
            "ytelseskomponenter" : [ {
              "ytelsesKomponentType" : "IP",
              "belopTilUtbetaling" : 1986
            }, {
              "ytelsesKomponentType" : "GP",
              "belopTilUtbetaling" : 1004
            }, {
              "ytelsesKomponentType" : "TP",
              "belopTilUtbetaling" : 2067
            } ]
          }, {
            "fom" : "2025-05-01",
            "tom" : null,
            "mottarMinstePensjonsniva" : false,
            "belop" : 5286,
            "ytelseskomponenter" : [ {
              "ytelsesKomponentType" : "IP",
              "belopTilUtbetaling" : 2076
            }, {
              "ytelsesKomponentType" : "GP",
              "belopTilUtbetaling" : 1050
            }, {
              "ytelsesKomponentType" : "TP",
              "belopTilUtbetaling" : 2160
            } ]
          } ]
        } ]
        
    """.trimIndent()
}