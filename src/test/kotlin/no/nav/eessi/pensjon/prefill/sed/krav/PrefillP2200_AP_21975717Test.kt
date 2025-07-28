package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.BucType.P_BUC_01
import no.nav.eessi.pensjon.eux.model.SedType.P2000
import no.nav.eessi.pensjon.eux.model.SedType.P2200
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.kodeverk.Postnummer
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.prefill.*
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLAdresse
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper.lesPensjonsdataFraFil
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper.readJsonResponse
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

class PrefillP2200_AP_21975717Test {

    private val personService: PersonService = mockk()
    var kodeverkClient: KodeverkClient = mockk(relaxed = true)

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(68)
    private val ekteFnr = FodselsnummerGenerator.generateFnrForTest(70)

    private val pesysSaksnummer = "14915730"

    private lateinit var prefillNav: PrefillPDLNav
    private lateinit var prefillData: PrefillDataModel
    private lateinit var etterlatteService: EtterlatteService
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var pensjonCollection: PensjonCollection
    private lateinit var dataFromPEN: PensjonsinformasjonService
    private lateinit var personDataCollection: PersonDataCollection

    @BeforeEach
    fun setup() {
        etterlatteService = mockk()
        personDataCollection = PersonPDLMock.createEnkelFamilie(personFnr, ekteFnr)

        prefillNav = BasePrefillNav.createPrefillNav(PrefillPDLAdresse(kodeverkClient, personService))

        dataFromPEN = lesPensjonsdataFraFil("/pensjonsinformasjon/krav/KravAlderEllerUfore_AP_UTLAND.xml")

        prefillData = PrefillDataModelMother.initialPrefillDataModel(P2200, personFnr, penSaksnummer = pesysSaksnummer).apply {
            partSedAsJson["PersonInfo"] = readJsonResponse("/json/nav/other/person_informasjon_selvb.json")
            partSedAsJson["P4000"] = readJsonResponse("/json/nav/other/p4000_trygdetid_part.json")
        }
        prefillSEDService = BasePrefillNav.createPrefillSEDService(prefillNav)

        val innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = dataFromPEN)
        pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)

    }

    @Test
    fun `forventet korrekt utfylt P2200 uforerpensjon med mockdata fra testfiler`() {

        every { kodeverkClient.finnLandkode("NOR") } returns "NO"
        every { kodeverkClient.hentPostSted(any()) } returns Postnummer("1068", "OSLO")

        val p2200 = prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null)

        assertEquals(null, p2200.nav?.barn)

        assertEquals("", p2200.nav?.bruker?.arbeidsforhold?.get(0)?.yrke)
        assertEquals("2018-11-11", p2200.nav?.bruker?.arbeidsforhold?.get(0)?.planlagtstartdato)
        assertEquals("2018-11-13", p2200.nav?.bruker?.arbeidsforhold?.get(0)?.planlagtpensjoneringsdato)
        assertEquals("07", p2200.nav?.bruker?.arbeidsforhold?.get(0)?.type)

        assertEquals("foo", p2200.nav?.bruker?.bank?.navn)
        assertEquals("bar", p2200.nav?.bruker?.bank?.konto?.sepa?.iban)
        assertEquals("baz", p2200.nav?.bruker?.bank?.konto?.sepa?.swift)

        assertEquals("ODIN ETTØYE", p2200.nav?.bruker?.person?.fornavn)
        assertEquals("BALDER", p2200.nav?.bruker?.person?.etternavn)
        val navfnr1 = Fodselsnummer.fra(p2200.nav?.bruker?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(68, navfnr1?.getAge())

        assertNotNull(p2200.nav?.bruker?.person?.pin)
        val pinlist = p2200.nav?.bruker?.person?.pin
        val pinitem = pinlist?.get(0)
        assertEquals(null, pinitem?.sektor)
        assertEquals("NOINST002, NO INST002, NO", pinitem?.institusjonsnavn)
        assertEquals("NO:noinst002", pinitem?.institusjonsid)
        assertEquals(personFnr, pinitem?.identifikator)

        assertEquals("NO", pinitem?.land)

        assertEquals("NO", p2200.nav?.bruker?.adresse?.land)
        assertEquals("FORUSBEEN 2294", p2200.nav?.bruker?.adresse?.gate)
        assertEquals("0010", p2200.nav?.bruker?.adresse?.postnummer)
        assertEquals("OSLO", p2200.nav?.bruker?.adresse?.by)

        assertEquals("THOR-DOPAPIR", p2200.nav?.ektefelle?.person?.fornavn)
        assertEquals("RAGNAROK", p2200.nav?.ektefelle?.person?.etternavn)

        val navfnr = Fodselsnummer.fra(p2200.nav?.ektefelle?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(70, navfnr?.getAge())
    }

    @Test
    fun `testing av komplett P2200 med utskrift og testing av innsending`() {
        every { kodeverkClient.finnLandkode(any()) } returns "NO"

        val p2200 = prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null)
        val json = mapAnyToJson(createMockApiRequest(p2200.toJson()))
        assertNotNull(json)
    }

    private fun createMockApiRequest(payload: String): ApiRequest {
        val items = listOf(InstitusjonItem(country = "NO", institution = "NAVT003"))
        return ApiRequest(
                institutions = items,
                sed = P2000,
                sakId = pesysSaksnummer,
                euxCaseId = null,
                aktoerId = "1000060964183",
                buc = P_BUC_01,
                subjectArea = "Pensjon",
                payload = payload
        )
    }
}

