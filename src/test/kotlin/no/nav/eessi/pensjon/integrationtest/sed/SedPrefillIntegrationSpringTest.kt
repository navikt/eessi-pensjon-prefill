package no.nav.eessi.pensjon.integrationtest.sed

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.UnsecuredWebMvcTestLauncher
import no.nav.eessi.pensjon.eux.model.BucType
import no.nav.eessi.pensjon.eux.model.BucType.*
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.SedType.*
import no.nav.eessi.pensjon.eux.model.sed.Betalingshyppighet
import no.nav.eessi.pensjon.eux.model.sed.Krav
import no.nav.eessi.pensjon.eux.model.sed.KravType
import no.nav.eessi.pensjon.integrationtest.IntegrasjonsTestConfig
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.kodeverk.Postnummer
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.model.AktoerId
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentGruppe.AKTORID
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentGruppe.FOLKEREGISTERIDENT
import no.nav.eessi.pensjon.personoppslag.pdl.model.NorskIdent
import no.nav.eessi.pensjon.personoppslag.pdl.model.PdlPerson
import no.nav.eessi.pensjon.prefill.KrrService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.PesysService
import no.nav.eessi.pensjon.prefill.models.DigitalKontaktinfo
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P6000MeldingOmVedtakDto
import no.nav.eessi.pensjon.prefill.sed.vedtak.helper.KSAK
import no.nav.eessi.pensjon.shared.api.ApiRequest
import no.nav.eessi.pensjon.shared.api.ApiSubject
import no.nav.eessi.pensjon.shared.api.SubjectFnr
import no.nav.eessi.pensjon.utils.toJson
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestTemplate
import java.io.File
import java.time.LocalDate

//Daniel
@SpringBootTest(
    classes = [IntegrasjonsTestConfig::class, UnsecuredWebMvcTestLauncher::class, SedPrefillIntegrationSpringTest.TestConfig::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("unsecured-webmvctest", "excludeKodeverk")
@AutoConfigureMockMvc
@DirtiesContext
@EmbeddedKafka
class SedPrefillIntegrationSpringTest {

//    @MockkBean
//    private lateinit var pesysClientRestTemplate: RestTemplate

    @MockkBean
    private lateinit var kodeverkClient: KodeverkClient

    @MockkBean
    private lateinit var personService: PersonService

    @MockkBean
    private lateinit var krrService: KrrService

    @Autowired
    lateinit var pesysService: PesysService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @TestConfiguration
    internal class TestConfig {

        @Bean
        fun pesysClientRestTemplate(): RestTemplate = mockk()

        @Bean
        fun pesysService() = mockk<PesysService>()
    }

    private companion object {
        const val FNR_VOKSEN = "11067122781"    // KRAFTIG VEGGPRYD
        const val FNR_VOKSEN_3 = "12312312312"
        const val FNR_VOKSEN_4 = "9876543210"

        const val AKTOER_ID = "0123456789000"
        const val AKTOER_ID_2 = "0009876543210"
    }

    @BeforeEach
    fun setup() {
//        pesysService = PesysService(pesysClientRestTemplate)

        every { krrService.hentPersonerFraKrr(any()) } returns DigitalKontaktinfo(
            epostadresse = "melleby11@melby.no", mobiltelefonnummer = "11111111", aktiv = true, personident = FNR_VOKSEN
        )
        every { krrService.hentPersonerFraKrr(eq(FNR_VOKSEN_4)) } returns DigitalKontaktinfo(
            epostadresse = "melleby11@melby.no",
            mobiltelefonnummer = "11111111",
            aktiv = true,
            personident = FNR_VOKSEN_4
        )
        every { kodeverkClient.hentPostSted(any()) } returns Postnummer("1068", "SØRUMSAND")
        every { kodeverkClient.finnLandkode(any()) } returns "QX"

        val mockP6000MeldingOmVedtakDto = P6000MeldingOmVedtakDto(
            avdod = P6000MeldingOmVedtakDto.Avdod(
                avdod = null,
                avdodBoddArbeidetUtland = null,
                avdodFarBoddArbeidetUtland = true,
                avdodMorBoddArbeidetUtland = true
            ), sakAlder = P6000MeldingOmVedtakDto.SakAlder(
                sakType = KSAK.BARNEP // Assuming KSAK has BARNEP as an enum value
            ), trygdeavtale = null, // No data in XML for Trygdeavtale
            trygdetidListe = listOf(
                P6000MeldingOmVedtakDto.Trygdetid(
                    fom = LocalDate.of(2008, 1, 13), tom = LocalDate.of(2020, 8, 20)
                )
            ), vedtak = P6000MeldingOmVedtakDto.Vedtak(
                virkningstidspunkt = LocalDate.of(2020, 8, 1),
                kravGjelder = "F_BH_BO_UTL",
                hovedytelseTrukket = false,
                boddArbeidetUtland = false,
                datoFattetVedtak = LocalDate.of(2020, 8, 21)
            ), vilkarsvurderingListe = listOf(
                P6000MeldingOmVedtakDto.Vilkarsvurdering(
                    fom = LocalDate.of(2020, 8, 1),
                    vilkarsvurderingUforetrygd = null,
                    resultatHovedytelse = "INNV",
                    harResultatGjenlevendetillegg = false,
                    avslagHovedytelse = null
                )
            ), ytelsePerMaanedListe = listOf(
                P6000MeldingOmVedtakDto.YtelsePerMaaned(
                    fom = LocalDate.of(2020, 8, 1),
                    tom = null,
                    mottarMinstePensjonsniva = false,
                    vinnendeBeregningsmetode = "FOLKETRYGD",
                    belop = 16644,
                    ytelseskomponentListe = listOf(
                        EessiFellesDto.Ytelseskomponent(
                            ytelsesKomponentType = "GP", belopTilUtbetaling = 8322
                        ), EessiFellesDto.Ytelseskomponent(
                            ytelsesKomponentType = "TP", belopTilUtbetaling = 5398
                        ), EessiFellesDto.Ytelseskomponent(
                            ytelsesKomponentType = "ST", belopTilUtbetaling = 2924
                        )
                    )
                )
            )
        )
        every { pesysService.hentP6000data(any()) } returns mockP6000MeldingOmVedtakDto


        val mockP2000 = mockk<P2xxxMeldingOmPensjonDto> {
            every { sak } returns P2xxxMeldingOmPensjonDto.Sak(
                sakType = EessiFellesDto.EessiSakType.ALDER,
                kravHistorikk = listOf(
                    P2xxxMeldingOmPensjonDto.KravHistorikk(
                        mottattDato = LocalDate.parse("2020-08-08"),
//                        kravType = EessiFellesDto.EessiKravGjelder.REVURD,
                        virkningstidspunkt = LocalDate.parse("2019-07-15"),
                        kravStatus = EessiFellesDto.EessiSakStatus.INGEN_STATUS,
//                        kravArsak = EessiKravArsak.NY_SOKNAD.name
                    )
                ),
                ytelsePerMaaned = emptyList(),
                forsteVirkningstidspunkt = null,
                status = EessiFellesDto.EessiSakStatus.TIL_BEHANDLING,
            )
            every { vedtak } returns P2xxxMeldingOmPensjonDto.Vedtak(boddArbeidetUtland = true)
        }
//        every {
//            pesysService.hentP2000data(any())
//        } returns mockP2000
    }

    @ParameterizedTest(name = "for verdier for sakId:{0}, vedtak:{1}, sedType:{2}, og feilmelding:{3}")
    @CsvSource(
        value = ["123, null, P6000, Mangler vedtakID", "null, 12121, P2000, Mangler sakId"], nullValues = ["null"]
    )
    @Throws(Exception::class)
    fun `Validering av prefill kaster exception`(
        sakId: String?, vedtakid: String?, sedType: String, expectedErrorMessage: String
    ) {
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(
            true, fnr = FNR_VOKSEN, aktoerid = AKTOER_ID
        )

        val apijson = dummyApijson(
            sakid = sakId ?: "", vedtakid = vedtakid, aktoerId = AKTOER_ID, sedType = SedType.valueOf(sedType)
        )

        mockMvcSedPrefill(apijson, expectedErrorMessage)
    }

    fun mockPersonService(
        fnr: String, aktoerId: String, fornavn: String? = null, etternavn: String? = null
    ): PdlPerson {
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(aktoerId)) } returns NorskIdent(fnr)
        PersonPDLMock.createWith(
            true, fornavn = fornavn ?: "", fnr = fnr, aktoerid = aktoerId, etternavn = etternavn ?: ""
        ).also {
            every { personService.hentPerson(NorskIdent(fnr)) } returns PersonPDLMock.createWith(
                true, fornavn = fornavn ?: "", fnr = fnr, aktoerid = aktoerId, etternavn = etternavn ?: ""
            )
            return it
        }
    }

    @ParameterizedTest(name = "{0} skal gi feilmelding:{2}")
    @CsvSource(
        value = [
            "med alder with uføre pensjondata throw error bad request, 22874955, Du kan ikke opprette en P2000 med saktype UFOREP. (PESYS-saksnr: 22874955 har sakstype UFOREP), UFOREP",
            "med sak fra GJENLEV feiler, 22927579, Du kan ikke opprette en P2000 med saktype GJENLEV. (PESYS-saksnr: 22927579 har sakstype GJENLEV), GJENLEV"
        ],
        nullValues = ["null"]
    )
    @Throws(Exception::class)
    fun `prefill sed `(testInfo: String, sakId: String, feilmelding: String, sakType: String) {

        mockPersonService(FNR_VOKSEN, AKTOER_ID)

        when(sakType) {
            "UFOREP" -> mockP2000(sakId, EessiFellesDto.EessiSakType.UFOREP, EessiFellesDto.EessiSakStatus.INNV, EessiFellesDto.EessiKravAarsak.NY_SOKNAD)
            "GJENLEV" ->  mockP2000(sakId, EessiFellesDto.EessiSakType.GJENLEV, EessiFellesDto.EessiSakStatus.INNV, EessiFellesDto.EessiKravAarsak.NY_SOKNAD)
        }

        val apijson = dummyApijson(sakid = sakId, aktoerId = AKTOER_ID, vedtakid =sakId)

        mockMvcSedPrefill(apijson, feilmelding)

    }
    fun mockP2000(
        id: String,
        sakType: EessiFellesDto.EessiSakType,
        kravStatus: EessiFellesDto.EessiSakStatus,
        kravArsak: EessiFellesDto.EessiKravAarsak? = null
    ) {
        val kravHistorikk = P2xxxMeldingOmPensjonDto.KravHistorikk(
            mottattDato = LocalDate.parse("2021-03-01"),
            kravType = EessiFellesDto.EessiKravGjelder.F_BH_MED_UTL,
            virkningstidspunkt = LocalDate.parse("2019-06-01"),
            kravStatus = kravStatus,
            kravArsak = kravArsak
        )

        val sakMock = P2xxxMeldingOmPensjonDto.Sak(
            sakType = sakType,
            kravHistorikk = listOf(kravHistorikk),
            ytelsePerMaaned = if (sakType == EessiFellesDto.EessiSakType.UFOREP)
                listOf(
                    P2xxxMeldingOmPensjonDto.YtelsePerMaaned(
                        fom = LocalDate.parse("2019-06-01"),
                        belop = 18384,
                        ytelseskomponentListe = listOf(
                            EessiFellesDto.Ytelseskomponent(
                                ytelsesKomponentType = "UT_ORDINER", belopTilUtbetaling = 18384
                            )
                        )
                    )
                )
            else emptyList(),
            forsteVirkningstidspunkt = LocalDate.parse("2021-03-01"),
            status = if (sakType == EessiFellesDto.EessiSakType.GJENLEV) EessiFellesDto.EessiSakStatus.INNV else kravStatus
        )

        val mockP2000 = mockk<P2xxxMeldingOmPensjonDto> {
            every { sak } returns sakMock
            every { vedtak } returns null
        }
        every { pesysService.hentP2000data(id) } returns mockP2000
    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P6000 P_BUC_02 Gjenlevende har med avdod skal returnere en gyldig SED`() {
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentIdent(AKTORID, NorskIdent(FNR_VOKSEN_4)) } returns AktoerId(AKTOER_ID_2)

        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(
            true, "Lever", "Gjenlev", FNR_VOKSEN, AKTOER_ID
        )
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_4)) } returns PersonPDLMock.createWith(
            true, "Avdød", "Død", FNR_VOKSEN_4, AKTOER_ID_2, true
        )
//        every { pesysService.hentP6000data(any()) } returns P6000MeldingOmVedtakDto(
//            gjenlevendeFnr = FNR_VOKSEN,
//            avdodFnr = FNR_VOKSEN_4,
//            krav = Krav(
//                type = KravType.GJENLEV,
//                belop = 15000.0,
//                betalingshyppighet = Betalingshyppighet.MANEDLIG
//            )
//        )
//
//        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/vedtak/P6000-BARNEP-GJENLEV.xml")

        val apijson = dummyApijson(
            sakid = "22874955",
            vedtakid = "987654321122355466",
            aktoerId = AKTOER_ID,
            sedType = P6000,
            buc = P_BUC_02,
            fnravdod = FNR_VOKSEN_4
        )

        val result = mockMvcPrefill(apijson)

        val response = result.response.getContentAsString(charset("UTF-8"))

        val mapper = jacksonObjectMapper()
        val sedRootNode = mapper.readTree(response)
        val gjenlevendePIN = finnPin(sedRootNode.at("/pensjon/gjenlevende/person"))
        val avdodPIN = finnPin(sedRootNode.at("/nav/bruker"))

        Assertions.assertEquals(FNR_VOKSEN, gjenlevendePIN)
        Assertions.assertEquals(FNR_VOKSEN_4, avdodPIN)

    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P6000 P_BUC_01 Alderpensjon med avslag skal returnere en gyldig SED`() {

        val person = mockPersonService(FNR_VOKSEN_3, AKTOER_ID, fornavn = "Alder", etternavn = "Pensjonist")
//        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/vedtak/P6000-AP-Avslag.xml")

        val mockP6000MeldingOmVedtakDto = P6000MeldingOmVedtakDto(
            sakAlder = P6000MeldingOmVedtakDto.SakAlder(
                sakType = KSAK.ALDER
            ), trygdeavtale = null, // No data in XML for Trygdeavtale
            avdod = null,
            vedtak = P6000MeldingOmVedtakDto.Vedtak(
                virkningstidspunkt = LocalDate.parse("2020-12-16"),
                kravGjelder = "F_BH_BO_UTL",
                hovedytelseTrukket = false,
                boddArbeidetUtland = true,
                datoFattetVedtak = LocalDate.parse("2020-12-16")
            ), vilkarsvurderingListe = listOf(
                P6000MeldingOmVedtakDto.Vilkarsvurdering(
                    fom = LocalDate.parse("2020-11-01"),
                    vilkarsvurderingUforetrygd = null,
                    resultatHovedytelse = "AVSL",
                    harResultatGjenlevendetillegg = false,
                    avslagHovedytelse = "UNDER_3_AR_TT",
                )
            ),
            trygdetidListe = emptyList(),
            ytelsePerMaanedListe = emptyList()
        )
        every { pesysService.hentP6000data(any()) } returns mockP6000MeldingOmVedtakDto

        val apijson = dummyApijson(
            sakid = "22874955", vedtakid = "123123423423", aktoerId = AKTOER_ID, sedType = P6000, buc = P_BUC_01
        )

        val result = mockMvcPrefill(apijson)

        val response = result.response.getContentAsString(charset("UTF-8"))

        val validResponse = SedBuilder.ValidResponseBuilder().apply {
            sed = P6000
            pensjon = SedBuilder.P6000PensjonBuilder().build()
            nav {
                bruker {
                    person {
                        fornavn = "Alder"
                        etternavn = "Pensjonist"
                    }
                }
            }
        }.build().toJsonSkipEmpty()
        JSONAssert.assertEquals(response, validResponse, true)
    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P3000_SE Gjenlevende har med avdod skal returnere en gyldig SED`() {

        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentIdent(AKTORID, NorskIdent(FNR_VOKSEN_4)) } returns AktoerId(AKTOER_ID_2)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(
            true, "Lever", "Gjenlev", FNR_VOKSEN, AKTOER_ID
        )
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_4)) } returns PersonPDLMock.createWith(
            true, "Avdød", "Død", FNR_VOKSEN_4, AKTOER_ID_2, true
        )

        val apijson = dummyApijson(
            sakid = "22874955",
            vedtakid = "9876543211",
            aktoerId = AKTOER_ID,
            sedType = P3000_SE,
            buc = P_BUC_10,
            fnravdod = FNR_VOKSEN_4
        )

        val result = mockMvcPrefill(apijson)

        val response = result.response.getContentAsString(charset("UTF-8"))

        val mapper = jacksonObjectMapper()
        val sedRootNode = mapper.readTree(response)
        val gjenlevendePIN = finnPin(sedRootNode.at("/pensjon/gjenlevende/person"))
        val annenPersonPIN = finnPin(sedRootNode.at("/nav/annenperson/person"))
        val avdodPIN = finnPin(sedRootNode.at("/nav/bruker"))

        Assertions.assertEquals(FNR_VOKSEN, gjenlevendePIN)
        Assertions.assertEquals(FNR_VOKSEN, annenPersonPIN)
        Assertions.assertEquals(FNR_VOKSEN_4, avdodPIN)

    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P5000 med Gjenlevende og avdod skal returnere en gyldig SED`() {

        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentIdent(AKTORID, NorskIdent(FNR_VOKSEN_4)) } returns AktoerId(AKTOER_ID_2)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(
            true, "Lever", "Gjenlev", FNR_VOKSEN, AKTOER_ID
        )
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_4)) } returns PersonPDLMock.createWith(
            true, "Avdød", "Død", FNR_VOKSEN_4, AKTOER_ID_2, true
        )

        val apijson = dummyApijson(
            sakid = "22874955",
            vedtakid = "9876543211",
            aktoerId = AKTOER_ID,
            sedType = P5000,
            buc = P_BUC_10,
            fnravdod = FNR_VOKSEN_4
        )

        val result = mockMvcPrefill(apijson)

        val response = result.response.getContentAsString(charset("UTF-8"))

        val mapper = jacksonObjectMapper()
        val sedRootNode = mapper.readTree(response)
        val gjenlevendePIN = finnPin(sedRootNode.at("/pensjon/gjenlevende/person"))
        val avdodPIN = finnPin(sedRootNode.at("/nav/bruker"))

        Assertions.assertEquals(FNR_VOKSEN, gjenlevendePIN)
        Assertions.assertEquals(FNR_VOKSEN_4, avdodPIN)

    }


    @Test
    @Throws(Exception::class)
    fun `prefill sed P4000 med forsikret person skal returnere en gyldig SED`() {
        mockPersonService(fnr = FNR_VOKSEN_3, aktoerId = AKTOER_ID, fornavn = "Lever", etternavn = "Gjenlev")
        val apijson = dummyApijson(
            sakid = "22874955", vedtakid = "9876543211", aktoerId = AKTOER_ID, sedType = P4000, buc = P_BUC_05
        )

        val result = mockMvcPrefill(apijson)

        val response = result.response.getContentAsString(charset("UTF-8"))
        val validResponse = SedBuilder.ValidResponseBuilder().apply {
            sed = P4000
            nav {
                krav = null
            }
        }.build().toJsonSkipEmpty()

        val mapper = jacksonObjectMapper()
        val sedRootNode = mapper.readTree(response)
        val forsikretPin = finnPin(sedRootNode.at("/nav/bruker"))

        Assertions.assertEquals(FNR_VOKSEN_3, forsikretPin)
        JSONAssert.assertEquals(response, validResponse, true)
    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P2000 alder return valid sedjson`() {
        val person = PersonPDLMock.createWith()
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns person
        every { pesysService.hentP2000data(any()) } returns readP2000FromXml("/pensjonsinformasjon/krav/P2000-AP-UP-21337890.xml")

        val apijson = dummyApijson(sakid = "21337890", aktoerId = AKTOER_ID, vedtakid = "21337890")
        val validResponse = SedBuilder.ValidResponseBuilder().apply {
            sed = P2000
            pensjon = SedBuilder.P2000PensjonBuilder().apply {
                ytelser = SedBuilder.YtelserBuilder(
                    belop = null
                ).build()
            }.build()
            nav {
                eessisak[0].saksnummer = "21337890"
                bruker {
                    person {
                        fornavn = "Test"
                        etternavn = "Testesen"
                        pinList[0].identifikator = "3123"
                        pinList[1].identifikator = "123123123"
                    }
                }
            }
        }.build().toJsonSkipEmpty()

        val response = prefillFraRestOgVerifiserResultet(apijson)
        JSONAssert.assertEquals(response, validResponse, true)
    }

    fun readP2000FromXml(path: String): P2xxxMeldingOmPensjonDto {
        val xmlMapper = XmlMapper()
        val resource = javaClass.getResource(path)
        val root: JsonNode = xmlMapper.readTree(File(resource!!.toURI()))

        // Navigate to the first "brukersSakerListe" node
        val sakNode = root
            .path("brukersSakerListe")
            .path("brukersSakerListe")
            .firstOrNull() ?: throw IllegalArgumentException("No sak found")

        // Map fields manually
        val sakType = EessiFellesDto.EessiSakType.valueOf(sakNode.path("sakType").asText())
        val status = EessiFellesDto.EessiSakStatus.valueOf(sakNode.path("status").asText())
        val forsteVirkningstidspunkt = LocalDate.parse(sakNode.path("forsteVirkningstidspunkt").asText().substring(0, 10))

        val kravHistorikk = sakNode
            .path("kravHistorikkListe")
            .path("kravHistorikkListe")
            .map { kravNode ->
                val kravStatusText = kravNode.path("status").asText()
                val kravStatus = if (kravStatusText == "Ingen status") {
                    EessiFellesDto.EessiSakStatus.INGEN_STATUS
                } else {
                    EessiFellesDto.EessiSakStatus.valueOf(kravStatusText)
                }
                P2xxxMeldingOmPensjonDto.KravHistorikk(
                    mottattDato = LocalDate.parse(kravNode.path("mottattDato").asText().substring(0, 10)),
                    kravType = EessiFellesDto.EessiKravGjelder.valueOf(kravNode.path("kravType").asText()),
                    virkningstidspunkt = kravNode.path("virkningstidspunkt")?.let {
                        if (it.isMissingNode) null else LocalDate.parse(it.asText().substring(0, 10))
                    },
                    kravStatus = kravStatus
                )
            }

        val ytelsePerMaaned = sakNode
            .path("ytelsePerMaanedListe")
            .path("ytelsePerMaanedListe")
            .map { ytelseNode ->
                P2xxxMeldingOmPensjonDto.YtelsePerMaaned(
                    fom = LocalDate.parse(ytelseNode.path("fom").asText().substring(0, 10)),
                    belop = ytelseNode.path("belop").asInt(),
                    ytelseskomponentListe = ytelseNode
                        .path("ytelseskomponentListe")
                        .map { kompNode ->
                            EessiFellesDto.Ytelseskomponent(
                                ytelsesKomponentType = kompNode.path("ytelsesKomponentType").asText(),
                                belopTilUtbetaling = kompNode.path("belopTilUtbetaling").asInt()
                            )
                        }
                )
            }

        return P2xxxMeldingOmPensjonDto(
            sak = P2xxxMeldingOmPensjonDto.Sak(
                sakType = sakType,
                kravHistorikk = kravHistorikk,
                ytelsePerMaaned = ytelsePerMaaned,
                forsteVirkningstidspunkt = forsteVirkningstidspunkt,
                status = status
            ),
            vedtak = null // Map if needed
        )
    }

    @Test
    fun `prefill sed P2000 alder med overgang fra ufore med sakstatus Ukjent return valid sedjson`() {
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()

        val mockP2000 = mockk<P2xxxMeldingOmPensjonDto> {
            every { sak } returns P2xxxMeldingOmPensjonDto.Sak(
                sakType = EessiFellesDto.EessiSakType.ALDER,
                kravHistorikk = listOf(
                    P2xxxMeldingOmPensjonDto.KravHistorikk(
                        mottattDato = LocalDate.parse("2015-11-25"),
                        kravType = EessiFellesDto.EessiKravGjelder.F_BH_MED_UTL,
                        virkningstidspunkt = LocalDate.parse("2016-03-01"),
                        kravStatus = EessiFellesDto.EessiSakStatus.AVSL,
                    ),
                    P2xxxMeldingOmPensjonDto.KravHistorikk(
                        mottattDato = LocalDate.parse("2020-08-08"),
                        kravType = EessiFellesDto.EessiKravGjelder.SLUTT_BH_UTL,
                        virkningstidspunkt = LocalDate.parse("2016-03-01"),
                        kravStatus = EessiFellesDto.EessiSakStatus.INNV,
                    ),
                ),
                ytelsePerMaaned = listOf(
                    P2xxxMeldingOmPensjonDto.YtelsePerMaaned(
                        fom = LocalDate.parse("2016-03-01"),
                        belop = 14574,
                        ytelseskomponentListe = listOf(
                            EessiFellesDto.Ytelseskomponent(
                                ytelsesKomponentType = "GP", belopTilUtbetaling = 4768
                            ),
                            EessiFellesDto.Ytelseskomponent(
                                ytelsesKomponentType = "TP", belopTilUtbetaling = 8514
                            ),

                            EessiFellesDto.Ytelseskomponent(
                                ytelsesKomponentType = "IP", belopTilUtbetaling = 1124
                            ),
                            EessiFellesDto.Ytelseskomponent(
                                ytelsesKomponentType = "GAP", belopTilUtbetaling = 168
                            )
                        ),
                    )
                ),
                forsteVirkningstidspunkt = LocalDate.parse("2016-03-01"),
                status = EessiFellesDto.EessiSakStatus.UKJENT,
            )
            every { vedtak } returns null
        }
        every {
            pesysService.hentP2000data(any())
        } returns mockP2000

        val apijson = dummyApijson(sakid = "21841174", aktoerId = AKTOER_ID, vedtakid = "21337890")

        val response = prefillFraRestOgVerifiserResultet(apijson)

        val validResponse = SedBuilder.ValidResponseBuilder().apply {
            sed = P2000
            pensjon = SedBuilder.P2000PensjonBuilder().apply {
                kravDato = Krav("2015-11-25")
                forespurtstartdato = "2016-03-01"
                ytelser = SedBuilder.YtelserBuilder(
                    status = "01",
                    mottasbasertpaa = null,
                    startdatoutbetaling = "2016-03-01",
                    startdatoretttilytelse = "2016-03-01",
                    totalbruttobeloeparbeidsbasert = "9638",
                    totalbruttobeloepbostedsbasert = "4936",
                    belop = SedBuilder.BelopBuilder("14574", Betalingshyppighet.maaned_12_per_aar, "2016-03-01").build()
                ).build()
            }.build()
            nav {
                eessisak[0].saksnummer = "21841174"
                bruker {
                    person {
                        fornavn = "Test"
                        etternavn = "Testesen"
                        pinList[0].identifikator = "3123"
                        pinList[1].identifikator = "123123123"
                    }
                }
                krav = SedBuilder.KravBuilder("2015-11-25")
            }
        }.build().toJsonSkipEmpty()
        JSONAssert.assertEquals(validResponse, response, true)
    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P2000 alder F_BH_KUN_UTL return valid sedjson`() {
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()
//        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/AP_2000_KUN_UTLAND.xml")
        val mockP2000 = mockk<P2xxxMeldingOmPensjonDto> {
            every { sak } returns P2xxxMeldingOmPensjonDto.Sak(
                sakType = EessiFellesDto.EessiSakType.ALDER,
                kravHistorikk = listOf(
                    P2xxxMeldingOmPensjonDto.KravHistorikk(
                        mottattDato = LocalDate.parse("2021-03-01"),
                        kravType = EessiFellesDto.EessiKravGjelder.F_BH_KUN_UTL,
                        virkningstidspunkt = LocalDate.parse("2019-06-01"),
                        kravStatus = EessiFellesDto.EessiSakStatus.INGEN_STATUS,
                        kravArsak = EessiFellesDto.EessiKravAarsak.NY_SOKNAD
                    )
                ),
                ytelsePerMaaned = emptyList(),
                forsteVirkningstidspunkt =  LocalDate.parse("2021-03-01"),
                status = EessiFellesDto.EessiSakStatus.INGEN_STATUS,
            )
            every { vedtak } returns null
        }
        every { pesysService.hentP2000data(any()) } returns mockP2000
        val apijson = dummyApijson(sakid = "22932784", aktoerId = AKTOER_ID, vedtakid = "21337890")
        val validResponse = SedBuilder.ValidResponseBuilder().apply {
            sed = P2000
            pensjon = SedBuilder.P2000PensjonBuilder().apply {
                kravDato = Krav("2021-03-01")
                ytelser = SedBuilder.YtelserBuilder(
                    belop = null
                ).build()
            }.build()
            nav {
                eessisak[0].saksnummer = "22932784"
                bruker {
                    person {
                        fornavn = "Test"
                        etternavn = "Testesen"
                        pinList[0].identifikator = "3123"
                        pinList[1].identifikator = "123123123"
                    }
                }
                krav = SedBuilder.KravBuilder("2021-03-01")
            }
        }.build().toJsonSkipEmpty()
        val response = prefillFraRestOgVerifiserResultet(apijson)
        JSONAssert.assertEquals(validResponse, response, true)
    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed P2000 alder med AVSL returnerer en valid sedjson`() {

        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()
//        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2000krav-alderpensjon-avslag.xml")

        val mockP2000 = mockk<P2xxxMeldingOmPensjonDto> {
            every { sak } returns P2xxxMeldingOmPensjonDto.Sak(
                sakType = EessiFellesDto.EessiSakType.ALDER,
                kravHistorikk = listOf(
                    P2xxxMeldingOmPensjonDto.KravHistorikk(
                        mottattDato = LocalDate.parse("2019-04-30"),
                        kravType = EessiFellesDto.EessiKravGjelder.F_BH_MED_UTL,
                        virkningstidspunkt = LocalDate.parse("2019-06-01"),
                        kravStatus = EessiFellesDto.EessiSakStatus.AVSL,
                    )
                ),
                ytelsePerMaaned = emptyList(),
                forsteVirkningstidspunkt =  LocalDate.parse("2019-06-01"),
                status = EessiFellesDto.EessiSakStatus.AVSL,
            )
            every { vedtak } returns null
        }
        every { pesysService.hentP2000data(any()) } returns mockP2000

        val apijson = dummyApijson(sakid = "22889955", aktoerId = AKTOER_ID, vedtakid = "22889955")
        val response = prefillFraRestOgVerifiserResultet(apijson)
        val validResponse = SedBuilder.ValidResponseBuilder().apply {
            sed = P2000
            pensjon = SedBuilder.P2000PensjonBuilder().apply {
                kravDato = Krav("2019-04-30")
                ytelser = SedBuilder.YtelserBuilder("03", belop = null).build()
            }.build()
            nav {
                eessisak[0].saksnummer = "22889955"
                bruker {
                    person {
                        fornavn = "Test"
                        etternavn = "Testesen"
                        pinList[0].identifikator = "3123"
                        pinList[1].identifikator = "123123123"
                    }
                }
                krav = SedBuilder.KravBuilder("2019-04-30")
            }
        }.build().toJsonSkipEmpty()
        JSONAssert.assertEquals(validResponse, response, true)
    }

    @Test
    fun `prefill sed med kravtype førstehangbehandling norge men med vedtak bodsatt utland skal prefylle sed`() {

        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN_3)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_3)) } returns PersonPDLMock.createWith(
            true, "Lever", "Gjenlev", FNR_VOKSEN_3
        )

        val mockP2000 = mockk<P2xxxMeldingOmPensjonDto> {
            every { sak } returns P2xxxMeldingOmPensjonDto.Sak(
                sakType = EessiFellesDto.EessiSakType.ALDER,
                kravHistorikk = listOf(
                    P2xxxMeldingOmPensjonDto.KravHistorikk(
                        mottattDato = LocalDate.parse("2018-05-31"),
                        kravType = EessiFellesDto.EessiKravGjelder.F_BH_MED_UTL,
                        virkningstidspunkt = LocalDate.parse("2018-05-31"),
                        kravStatus = EessiFellesDto.EessiSakStatus.AVSL,
                    )
                ),
                ytelsePerMaaned = listOf(
                    P2xxxMeldingOmPensjonDto.YtelsePerMaaned(
                        fom = LocalDate.parse("2018-08-01"),
                        belop = 21232,
                        ytelseskomponentListe = listOf(
                            EessiFellesDto.Ytelseskomponent(
                                ytelsesKomponentType = "GP", belopTilUtbetaling = 7034
                            ),
                            EessiFellesDto.Ytelseskomponent(
                                ytelsesKomponentType = "TP", belopTilUtbetaling = 14198
                            ),
                        ),
                    )
                ),
                forsteVirkningstidspunkt = LocalDate.parse("2018-08-01"),
                status = EessiFellesDto.EessiSakStatus.INNV,
            )
            every { vedtak } returns null
        }
        every {
            pesysService.hentP2000data(any())
        } returns mockP2000

        val apijson = dummyApijson(sakid = "22580170", aktoerId = AKTOER_ID, vedtakid = "5134513451345")

        val response = prefillFraRestOgVerifiserResultet(apijson)

        val validResponse = SedBuilder.ValidResponseBuilder().apply {
            sed = P2000
            pensjon = SedBuilder.P2000PensjonBuilder().apply {
                kravDato = Krav("2018-05-31")
                forespurtstartdato = "2018-08-01"
                ytelser = SedBuilder.YtelserBuilder(
                    status = "02",
                    mottasbasertpaa = null,
                    startdatoutbetaling = "2018-08-01",
                    startdatoretttilytelse = "2018-08-01",
                    totalbruttobeloeparbeidsbasert = "14198",
                    totalbruttobeloepbostedsbasert = "7034",
                    belop = SedBuilder.BelopBuilder("21232", Betalingshyppighet.maaned_12_per_aar).build()
                ).build()
            }.build()
            nav {
                eessisak[0].saksnummer = "22580170"
                bruker {
                    person {
                        fornavn = "Lever"
                        etternavn = "Gjenlev"
                        pinList[0].identifikator = "12312312312"
                        pinList[1].identifikator = "123123123"
                    }
                }
                krav = SedBuilder.KravBuilder("2018-05-31")
            }
        }.build().toJsonSkipEmpty()
        JSONAssert.assertEquals(validResponse, response, true)

    }


    /** test på validering av pensjoninformasjon krav **/
    @Test
    fun `prefill sed med kun utland, manglende kravtype skal kaste en Exception`() {
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()

        val mockP2000 = mockk<P2xxxMeldingOmPensjonDto> {
            every { sak } returns P2xxxMeldingOmPensjonDto.Sak(
                sakType = EessiFellesDto.EessiSakType.ALDER,
                kravHistorikk = listOf(
                    P2xxxMeldingOmPensjonDto.KravHistorikk(
                        kravStatus = EessiFellesDto.EessiSakStatus.INGEN_STATUS,
                        mottattDato = LocalDate.parse("2020-08-08"),
                    )
                ),
                ytelsePerMaaned = emptyList(),
                forsteVirkningstidspunkt = null,
                status = EessiFellesDto.EessiSakStatus.TIL_BEHANDLING,
            )
            every { vedtak } returns null
        }
        every {
            pesysService.hentP2000data(any())
        } returns mockP2000

        val apijson = dummyApijson(sakid = "21920707", aktoerId = AKTOER_ID, vedtakid = "22580170")
        val expectedError =
            """Det finnes ingen iverksatte vedtak for førstegangsbehandling kun utland, eller sluttbehandling. Vennligst gå til EESSI-Pensjon fra vedtakskontekst.""".trimIndent()

        mockMvcSedPrefill(apijson, expectedError)

    }

    @Test
    fun `prefill sed med kravtype førstehangbehandling skal kaste en Exception`() {

        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()

        val mockP2000 = mockk<P2xxxMeldingOmPensjonDto> {
            every { sak } returns P2xxxMeldingOmPensjonDto.Sak(
                sakType = EessiFellesDto.EessiSakType.ALDER,
                kravHistorikk = listOf(
                    P2xxxMeldingOmPensjonDto.KravHistorikk(
                        mottattDato = LocalDate.parse("2020-08-08"),
                        kravType = EessiFellesDto.EessiKravGjelder.FORSTEG_BH,
                    )
                ),
                ytelsePerMaaned = emptyList(),
                forsteVirkningstidspunkt = null,
                status = EessiFellesDto.EessiSakStatus.TIL_BEHANDLING,
            )
            every { vedtak } returns null
        }
        every { pesysService.hentP2000data(any()) } returns mockP2000

        val apijson = dummyApijson(sakid = "22580170", aktoerId = AKTOER_ID, vedtakid = "22580170")

        mockMvcSedPrefill(
            apijson, "Det er ikke markert for bodd/arbeidet i utlandet. Krav SED P2000 blir ikke opprettet"
        )

    }

    @Test
    fun `prefill sed med ALDERP uten korrekt kravårsak skal kaste en Exception`() {
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN_3)
        every { personService.hentIdent(AKTORID, NorskIdent(FNR_VOKSEN_4)) } returns AktoerId(AKTOER_ID_2)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_3)) } returns PersonPDLMock.createWith(
            true, "Lever", "Gjenlev", FNR_VOKSEN_3, AKTOER_ID
        )
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_4)) } returns PersonPDLMock.createWith(
            true, "Avdød", "Død", FNR_VOKSEN_4, AKTOER_ID_2, true
        )
        val mockP2000 = mockk<P2xxxMeldingOmPensjonDto> {
            every { sak } returns P2xxxMeldingOmPensjonDto.Sak(
                sakType = EessiFellesDto.EessiSakType.ALDER,
                kravHistorikk = listOf(
                    P2xxxMeldingOmPensjonDto.KravHistorikk(
                        mottattDato = LocalDate.parse("2020-08-08"),
                        kravType = EessiFellesDto.EessiKravGjelder.F_BH_KUN_UTL,

                    )
                ),
                ytelsePerMaaned = emptyList(),
                forsteVirkningstidspunkt = null,
                status = EessiFellesDto.EessiSakStatus.TIL_BEHANDLING,
            )
            every { vedtak } returns null
        }
        every { pesysService.hentP2100data(any()) } returns mockP2000

        every { kodeverkClient.finnLandkode(any()) } returns "NO"

        val apijson = dummyApijson(
            sakid = "20541862",
            aktoerId = AKTOER_ID,
            sedType = P2100,
            buc = P_BUC_02,
            fnravdod = FNR_VOKSEN_4,
            vedtakid = "20541862"
        )

        mockMvcSedPrefill(
            apijson, "Ingen gyldig kravårsak funnet for ALDER eller UFØREP for utfylling av en krav SED P2100"
        )
    }

    @Test
    fun `prefill sed X010 valid sedjson`() {

        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()
//        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2000-AP-UP-21337890.xml")

        val apijson = dummyApijson(sakid = "21337890", aktoerId = AKTOER_ID, sedType = X010)

        val validResponse = """
                {
                  "sed" : "X010",
                  "sedGVer" : "4",
                  "sedVer" : "2",
                  "nav" : {
                    "sak" : {
                      "kontekst" : {
                        "bruker" : {
                          "person" : {
                            "etternavn" : "Testesen",
                            "fornavn" : "Test",
                            "kjoenn" : "M",
                            "foedselsdato" : "1988-07-12"
                          }
                        }
                      },
                      "paaminnelse" : {
                        "svar" : {
                          "informasjon" : {
                            "kommersenere" : [ { 
                                "type": "dokument",
                                "opplysninger": "."
                            } ]
                          }
                        }
                      }
                    }
                  }
                }
        """.trimIndent()

        val response = prefillFraRestOgVerifiserResultet(apijson)
        JSONAssert.assertEquals(response, validResponse, false)

    }

    @Test
    fun `prefill sed X010 med json data fra X009 gir en valid X010`() {

        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith()
//        every { pensjonsinformasjonOidcRestTemplate.exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java)) } returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2000-AP-UP-21337890.xml")

//        val x009 = SED.fromJsonToConcrete(PrefillTestHelper.readJsonResponse("/json/nav/X009-NAV.json")) as X009

//        val apijson = dummyApijson(sakid = "21337890", aktoerId = AKTOER_ID, sedType = X010, payload = x009.toJson())

        val validResponse = XSedBuilder.ValidResponseBuilderXSEd().build().toJsonSkipEmpty()
//        val response = prefillFraRestOgVerifiserResultet(apijson)
//        JSONAssert.assertEquals(response, validResponse, false)

    }

    private fun prefillFraRestOgVerifiserResultet(apijson: String): String {
        val result = mockMvcPrefill(apijson)

        val response = result.response.getContentAsString(charset("UTF-8"))
        return response
    }

    @Test
    @Throws(Exception::class)
    fun `prefill sed med uten korrekt kravtype skal kaste en Exception`() {
        every { personService.hentIdent(FOLKEREGISTERIDENT, AktoerId(AKTOER_ID)) } returns NorskIdent(FNR_VOKSEN)
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns PersonPDLMock.createWith(
            true, fnr = FNR_VOKSEN, aktoerid = AKTOER_ID
        )
//        every { pesysService.hentP2000data(any()) } returns mockk{
//            every { sak } returns P2xxxMeldingOmPensjonDto.Sak(
//                sakType = EessiFellesDto.EessiSakType.ALDER,
//                kravHistorikk = listOf(
//                    P2xxxMeldingOmPensjonDto.KravHistorikk(
//                        mottattDato = LocalDate.parse("2020-08-08"),
////                        kravType = EessiFellesDto.EessiKravGjelder.REVURD,
////                        virkningstidspunkt = LocalDate.parse("2019-07-15"),
//                        kravStatus = EessiFellesDto.EessiSakStatus.INGEN_STATUS,
////                        kravArsak = EessiKravArsak.NY_SOKNAD.name
//                    )
//                ),
//                ytelsePerMaaned = emptyList(),
//                forsteVirkningstidspunkt = null,
//                status = EessiFellesDto.EessiSakStatus.TIL_BEHANDLING,
//            )
//            every { vedtak } returns P2xxxMeldingOmPensjonDto.Vedtak(boddArbeidetUtland = true)
//        }


//        every { pensjonsinformasjonOidcRestTemplate .exchange(any<String>(), any(), any<HttpEntity<Unit>>(), eq(String::class.java))} returns PrefillTestHelper.readXMLresponse("/pensjonsinformasjon/krav/P2000-AP-MANGLER_BOSATT_UTLAND.xml")

        val apijson = dummyApijson(sakid = "21920707", aktoerId = AKTOER_ID)

        val melding =
            "Det finnes ingen iverksatte vedtak for førstegangsbehandling kun utland, eller sluttbehandling. Vennligst gå til EESSI-Pensjon fra vedtakskontekst."
//        mockMvcSedPrefill(apijson, melding)
    }

    private val PREFILL_URL = "/sed/prefill"

    private fun performPrefillRequest(apijson: String) = mockMvc.perform(
        post(PREFILL_URL).contentType(MediaType.APPLICATION_JSON).content(apijson)
    )

    private fun mockMvcSedPrefill(apijson: String, melding: String) {
        performPrefillRequest(apijson).andExpect(status().isBadRequest)
            .andExpect(status().reason(Matchers.containsString(melding)))
    }

    private fun mockMvcPrefill(apijson: String): MvcResult = performPrefillRequest(apijson).andExpect(status().isOk)
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn()

    private fun finnPin(pinNode: JsonNode): String? {
        return pinNode.findValue("pin").filter { pin -> pin.get("land").textValue() == "NO" }
            .map { pin -> pin.get("identifikator").textValue() }.lastOrNull()
    }

}

fun dummyApi(
    sakid: String?,
    vedtakid: String? = null,
    aktoerId: String,
    sedType: SedType = P2000,
    buc: BucType? = P_BUC_06,
    fnravdod: String? = null,
    kravtype: KravType? = null,
    kravdato: String? = null,
    payload: String? = null
): ApiRequest {
    val subject = if (fnravdod != null) {
        ApiSubject(null, SubjectFnr(fnravdod))
    } else {
        null
    }
    return ApiRequest(
        sakId = sakid,
        vedtakId = vedtakid,
        kravId = null,
        aktoerId = aktoerId,
        sed = sedType,
        buc = buc,
        kravType = kravtype,
        kravDato = kravdato,
        euxCaseId = "12345",
        institutions = emptyList(),
        subject = subject,
        payload = payload
    )
}

fun dummyApijson(
    sakid: String?,
    vedtakid: String? = null,
    aktoerId: String,
    sedType: SedType = P2000,
    buc: BucType? = P_BUC_06,
    fnravdod: String? = null,
    kravtype: KravType? = null,
    kravdato: String? = null,
    payload: String? = null
): String {
    return dummyApi(sakid, vedtakid, aktoerId, sedType, buc, fnravdod, kravtype, kravdato, payload).toJson()
}