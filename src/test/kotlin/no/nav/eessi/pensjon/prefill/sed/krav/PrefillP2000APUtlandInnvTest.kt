package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import no.nav.eessi.pensjon.eux.model.BucType.P_BUC_01
import no.nav.eessi.pensjon.eux.model.SedType.P2000
import no.nav.eessi.pensjon.eux.model.sed.BasertPaa
import no.nav.eessi.pensjon.eux.model.sed.KravType
import no.nav.eessi.pensjon.prefill.BasePrefillNav
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PersonDataService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.PesysService
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiKravGjelder
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiSakStatus
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiSakType
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.shared.api.ApiRequest
import no.nav.eessi.pensjon.shared.api.InstitusjonItem
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.Fodselsnummer
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.toJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestTemplate
import java.time.LocalDate

class PrefillP2000APUtlandInnvTest {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(68)
    private val ekteFnr = FodselsnummerGenerator.generateFnrForTest(70)
    private val pesysSaksnummer = "14398627"
    private val pesysService: PesysService = mockk()

    private lateinit var prefillData: PrefillDataModel
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var pensjonCollection: PensjonCollection
    private lateinit var personDataCollection: PersonDataCollection
    private lateinit var personDataService: PersonDataService



    fun readJsonResponse(file: String): String {
        return javaClass.getResource(file)!!.readText()
    }

    @BeforeEach
    fun setup() {
        every { pesysService.hentP2000data(any()) } returns mockk(){
            every { sak } returns P2xxxMeldingOmPensjonDto.Sak(
                sakType = EessiSakType.ALDER,
                kravHistorikk = listOf(
                    P2xxxMeldingOmPensjonDto.KravHistorikk(
                        mottattDato = LocalDate.of(2015, 11, 25),
                        kravType = EessiKravGjelder.F_BH_KUN_UTL,
                        virkningstidspunkt = LocalDate.of(2015, 11, 25),
                    )
                ),
                ytelsePerMaaned = emptyList(),
                forsteVirkningstidspunkt = LocalDate.of(2025, 12, 12),
                status = EessiSakStatus.TIL_BEHANDLING,
            )
            every { vedtak } returns P2xxxMeldingOmPensjonDto.Vedtak(boddArbeidetUtland = true)
        }
        personDataCollection = PersonPDLMock.createEnkelFamilie(personFnr, ekteFnr)
        personDataService = mockk(relaxed = true)

//        val dataFromPEN = lesPensjonsdataFraFil("/pensjonsinformasjon/krav/P2000-AP-UTL-INNV-24015012345_PlanB.xml")
        val innhentingService = InnhentingService(personDataService = personDataService, pesysService = pesysService)

        prefillData = PrefillDataModelMother.initialPrefillDataModel(P2000, personFnr, penSaksnummer = pesysSaksnummer, kravDato = "2015-11-25")
            .apply {
            partSedAsJson["PersonInfo"] = readJsonResponse("/json/nav/other/person_informasjon_selvb.json")
            partSedAsJson["P4000"] = readJsonResponse("/json/nav/other/p4000_trygdetid_part.json")
        }

        pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)
        prefillSEDService = BasePrefillNav.createPrefillSEDService()
    }

    @Test
    fun `forventet korrekt utfylt P2000 alderpensjon med kap4 og 9`() {
        println("prefilldata@@: ${prefillData.toJson()}")
        val P2000 = prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null)

        println("P2000: $prefillData")

        assertNotNull(P2000.nav?.krav)
        assertEquals("2015-11-25", P2000.nav?.krav?.dato)
//            pensjonCollection = PensjonCollection(p2xxxMeldingOmPensjonDto = P2xxxMeldingOmPensjonDto(
//                sak = P2xxxMeldingOmPensjonDto.Sak(
//                    sakType = EessiSakType.ALDER,
//                    kravHistorikk = listOf(
//                        P2xxxMeldingOmPensjonDto.KravHistorikk(
//                            mottattDato = LocalDate.of(2025, 12, 12)
//                        )
//                    ),
//                    ytelsePerMaaned = emptyList(),
//                    status = EessiSakStatus.INNV,
//
//                    )
//            ))
////
//            val prefillData = PrefillDataModel(bruker = PersonInfo("3216546897", null, false),
//                avdod = PersonInfo(personFnr, null, false),
//                sedType = P2000,
//                kravDato = "2025-12-12",
//                buc = P_BUC_01,
//                euxCaseID = "123456",
//                institution = listOf(InstitusjonItem("NO", "Inst"))
//            )

    }

    @Test
    fun `forventet korrekt utfylt P2000 alderpensjon og mottasbasertpaa satt til botid`() {
        val P2000 = prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null,) as no.nav.eessi.pensjon.eux.model.sed.P2000

        assertNotNull(P2000.nav?.krav)
        assertEquals("2015-11-25", P2000.nav?.krav?.dato)
        assertEquals(BasertPaa.botid.name, P2000.p2000pensjon?.ytelser?.firstOrNull()?.mottasbasertpaa)
    }

    @Test
    fun `forventet korrekt utfylt P2000 med belop`() {

//        val ytelsePerMaaned = PensjonsInformasjonHelper.createYtelsePerMaaned(
//            mottarMinstePensjonsniva = true,
//            belop = 123,
//            belopUtenAvkorting = 111,
//            fomDate = PensjonsInformasjonHelper.dummyDate(20),
//            tomDate = PensjonsInformasjonHelper.dummyDate(30)
//        ).apply {
//            ytelseskomponentListe.addAll(
//                listOf(
//                    PensjonsInformasjonHelper.createYtelseskomponent(
//                        type = YtelseskomponentType.GAP,
//                        belopTilUtbetaling = 444,
//                        belopUtenAvkorting = 333
//                    ),
//                    PensjonsInformasjonHelper.createYtelseskomponent(
//                        type = YtelseskomponentType.TP,
//                        belopTilUtbetaling = 444,
//                        belopUtenAvkorting = 333
//                    )
//                )
//            )
//        }
//        val gjenlevendHistorikk = PensjonsInformasjonHelper.createKravHistorikk(KravArsak.GJNL_SKAL_VURD.name, PenKravtype.F_BH_MED_UTL.name)

        // setter opp tilgang til mocking av selektive data
        val spykPensjonCollection = spyk(pensjonCollection)

//        every { spykPensjonCollection.sak } returns PensjonsInformasjonHelper.createSak(gjenlevendHistorikk, ytelsePerMaaned)

        val P2000 = prefillSEDService.prefill(
            prefillData,
            personDataCollection,
            spykPensjonCollection,
            null,
        ) as no.nav.eessi.pensjon.eux.model.sed.P2000

        assertEquals("444", P2000.p2000pensjon?.ytelser?.firstOrNull()?.totalbruttobeloepbostedsbasert)
        assertEquals("444", P2000.p2000pensjon?.ytelser?.firstOrNull()?.totalbruttobeloeparbeidsbasert)
        assertEquals("123", P2000.p2000pensjon?.ytelser?.firstOrNull()?.beloep?.firstOrNull()?.beloep)

    }

    @Test
    fun `forventet korrekt utfylt P2000 alderpersjon med mockdata fra testfiler`() {
        val p2000 = prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null,)

        assertEquals(null, p2000.nav?.barn)

        assertEquals("", p2000.nav?.bruker?.arbeidsforhold?.get(0)?.yrke)
        assertEquals("2018-11-11", p2000.nav?.bruker?.arbeidsforhold?.get(0)?.planlagtstartdato)
        assertEquals("2018-11-13", p2000.nav?.bruker?.arbeidsforhold?.get(0)?.planlagtpensjoneringsdato)
        assertEquals("07", p2000.nav?.bruker?.arbeidsforhold?.get(0)?.type)

        assertEquals("foo", p2000.nav?.bruker?.bank?.navn)
        assertEquals("bar", p2000.nav?.bruker?.bank?.konto?.sepa?.iban)
        assertEquals("baz", p2000.nav?.bruker?.bank?.konto?.sepa?.swift)

        assertEquals("ODIN ETTØYE", p2000.nav?.bruker?.person?.fornavn)
        assertEquals("BALDER", p2000.nav?.bruker?.person?.etternavn)
        val navfnr1 = Fodselsnummer.fra(p2000.nav?.bruker?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(68, navfnr1?.getAge())

        assertNotNull(p2000.nav?.bruker?.person?.pin)
        val pinlist = p2000.nav?.bruker?.person?.pin
        val pinitem = pinlist?.get(0)
        assertEquals(null, pinitem?.sektor)
        assertEquals("NOINST002, NO INST002, NO", pinitem?.institusjonsnavn)
        assertEquals("NO:noinst002", pinitem?.institusjonsid)
        assertEquals(personFnr, pinitem?.identifikator)


        assertEquals("THOR-DOPAPIR", p2000.nav?.ektefelle?.person?.fornavn)
        assertEquals("RAGNAROK", p2000.nav?.ektefelle?.person?.etternavn)

        val navfnr = Fodselsnummer.fra(p2000.nav?.ektefelle?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(70, navfnr?.getAge())

    }

    @Test
    fun `testing av komplett P2000 med utskrift og testing av innsending`() {
        val p2000 = prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null,)

        val json = mapAnyToJson(createMockApiRequest(p2000.toJson()))
        assertNotNull(json)

    }

    private fun createMockApiRequest(payload: String): ApiRequest {
        val items = listOf(InstitusjonItem(country = "NO", institution = "NAVT003"))
        return ApiRequest(
                institutions = items,
                sed = P2000,
                sakId = "01234567890",
                euxCaseId = "99191999911",
                aktoerId = "1000060964183",
                buc = P_BUC_01,
                subjectArea = "Pensjon",
                payload = payload
        )
    }

}

