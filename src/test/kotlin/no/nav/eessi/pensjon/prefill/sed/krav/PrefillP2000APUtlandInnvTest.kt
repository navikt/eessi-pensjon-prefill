package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import no.nav.eessi.pensjon.eux.model.BucType.P_BUC_01
import no.nav.eessi.pensjon.eux.model.SedType.P2000
import no.nav.eessi.pensjon.eux.model.sed.BasertPaa
import no.nav.eessi.pensjon.eux.model.sed.P2000
import no.nav.eessi.pensjon.prefill.*
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.models.YtelseskomponentType
import no.nav.eessi.pensjon.prefill.models.pensjon.*
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto.YtelsePerMaaned
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
        pesysMock(YtelseskomponentType.GAP.name)

        personDataCollection = PersonPDLMock.createEnkelFamilie(personFnr, ekteFnr)
        personDataService = mockk(relaxed = true)

        val innhentingService = InnhentingService(personDataService = personDataService, pesysService = pesysService)

        prefillData = PrefillDataModelMother.initialPrefillDataModel(P2000, personFnr, penSaksnummer = pesysSaksnummer, kravDato = "2015-11-25")
            .apply {
            partSedAsJson["PersonInfo"] = readJsonResponse("/json/nav/other/person_informasjon_selvb.json")
            partSedAsJson["P4000"] = readJsonResponse("/json/nav/other/p4000_trygdetid_part.json")
        }

        pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)
        prefillSEDService = BasePrefillNav.createPrefillSEDService()
    }

    private fun pesysMock(ytelsesKomponentType: String? = YtelseskomponentType.GAP.name) {
        every { pesysService.hentP2000data(any()) } returns mockk() {
            every { sak } returns P2xxxMeldingOmPensjonDto.Sak(
                sakType = EessiFellesDto.EessiSakType.ALDER,
                kravHistorikk = listOf(
                    P2xxxMeldingOmPensjonDto.KravHistorikk(
                        mottattDato = LocalDate.of(2015, 11, 25),
                        kravType = EessiFellesDto.EessiKravGjelder.F_BH_KUN_UTL,
                        virkningstidspunkt = LocalDate.of(2015, 11, 25),
                    )
                ),
                ytelsePerMaaned = listOf(
                    YtelsePerMaaned(
                        fom = LocalDate.of(2015, 11, 25),
                        belop = 123,
                        ytelseskomponentListe = listOf(
                            Ytelseskomponent(
                                YtelseskomponentType.TP.name,
                                444
                            ),
                            Ytelseskomponent(
                                YtelseskomponentType.GAP.name,
                                444
                            )

                        )
                    )
                ),
                forsteVirkningstidspunkt = LocalDate.of(2025, 12, 12),
                status = EessiFellesDto.EessiSakStatus.TIL_BEHANDLING,
            )
            every { vedtak } returns P2xxxMeldingOmPensjonDto.Vedtak(boddArbeidetUtland = true)
        }
    }

    @Test
    fun `forventet korrekt utfylt P2000 alderpensjon med kap4 og 9`() {
        val P2000 = prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null)

        assertNotNull(P2000.nav?.krav)
        assertEquals("2015-11-25", P2000.nav?.krav?.dato)

    }

    @Test
    fun `forventet korrekt utfylt P2000 alderpensjon og mottasbasertpaa satt til botid`() {
        pesysMock(YtelseskomponentType.TP.name)
        val P2000 = prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null,) as P2000

        println("Botid: ${P2000.p2000pensjon?.ytelser?.toJson()}")

        assertNotNull(P2000.nav?.krav)
        assertEquals("2015-11-25", P2000.nav?.krav?.dato)
        assertEquals(BasertPaa.botid.name, P2000.p2000pensjon?.ytelser?.firstOrNull()?.mottasbasertpaa)
    }

    @Test
    fun `forventet korrekt utfylt P2000 med belop`() {
        // setter opp tilgang til mocking av selektive data
        val spykPensjonCollection = spyk(pensjonCollection)

        val P2000 = prefillSEDService.prefill(
            prefillData,
            personDataCollection,
            spykPensjonCollection,
            null,
        ) as P2000

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

