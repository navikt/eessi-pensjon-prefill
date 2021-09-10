package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.sed.Nav
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.eux.model.sed.SedType
import no.nav.eessi.pensjon.prefill.ApiRequest
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.InstitusjonItem
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModel
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother.initialPrefillDataModel
import no.nav.eessi.pensjon.prefill.person.FodselsnummerMother.generateRandomFnr
import no.nav.eessi.pensjon.prefill.person.NavFodselsnummer
import no.nav.eessi.pensjon.prefill.person.PrefillPDLAdresse
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper.lesPensjonsdataFraFil
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper.readJsonResponse
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.toJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class PrefillP2000_AP_21975717Test {

    private val pesysSaksnummer = "21975717"

    private val giftFnr = generateRandomFnr(68)
    private val ekteFnr = generateRandomFnr(70)

    private lateinit var prefillData: PrefillDataModel
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var persondataCollection: PersonDataCollection
    private lateinit var pensjonCollection: PensjonCollection

    @BeforeEach
    fun setup() {
        persondataCollection = PersonPDLMock.createEnkelFamilie(giftFnr, ekteFnr)

        val prefillNav = PrefillPDLNav(
            prefillAdresse = mockk<PrefillPDLAdresse> {
                every { hentLandkode(any()) } returns "NO"
                every { createPersonAdresse(any()) } returns mockk(relaxed = true)
            },
            institutionid = "NO:noinst002",
            institutionnavn = "NOINST002, NO INST002, NO"
        )

        val dataFromPEN = lesPensjonsdataFraFil("KravAlderEllerUfore_AP_UTLAND.xml")

        prefillData = initialPrefillDataModel(SedType.P2000, giftFnr, penSaksnummer = pesysSaksnummer).apply {
            partSedAsJson["PersonInfo"] = readJsonResponse("other/person_informasjon_selvb.json")
            partSedAsJson["P4000"] = readJsonResponse("other/p4000_trygdetid_part.json")
        }
        val innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = dataFromPEN)
        pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)

       prefillSEDService = PrefillSEDService(EessiInformasjon(), prefillNav)

    }

    @Test
    fun `forventet korrekt utfylt P2000 alderpensjon med kap4 og 9`() {
        val P2000 = prefillSEDService.prefill(prefillData, persondataCollection, pensjonCollection)

        val P2000pensjon = SED(
                type = SedType.P2000,
                pensjon = P2000.pensjon,
                nav = Nav( krav = P2000.nav?.krav )
        )
        assertNotNull(P2000pensjon.nav?.krav)
        assertEquals("2015-06-16", P2000pensjon.nav?.krav?.dato)


    }

    @Test
    fun `forventet korrekt utfylt P2000 alderpersjon med mockdata fra testfiler`() {
        val p2000 = prefillSEDService.prefill(prefillData, persondataCollection, pensjonCollection)

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
        val navfnr1 = NavFodselsnummer(p2000.nav?.bruker?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(68, navfnr1.getAge())

        assertNotNull(p2000.nav?.bruker?.person?.pin)
        val pinlist = p2000.nav?.bruker?.person?.pin
        val pinitem = pinlist?.get(0)
        assertEquals(null, pinitem?.sektor)
        assertEquals("NOINST002, NO INST002, NO", pinitem?.institusjonsnavn)
        assertEquals("NO:noinst002", pinitem?.institusjonsid)
        assertEquals(giftFnr, pinitem?.identifikator)

        assertEquals("THOR-DOPAPIR", p2000.nav?.ektefelle?.person?.fornavn)
        assertEquals("RAGNAROK", p2000.nav?.ektefelle?.person?.etternavn)

        assertEquals(ekteFnr, p2000.nav?.ektefelle?.person?.pin?.get(0)?.identifikator)
        assertEquals(giftFnr, p2000.nav?.bruker?.person?.pin?.get(0)?.identifikator)

        assertEquals("NO", p2000.nav?.ektefelle?.person?.pin?.get(0)?.land)

        val navfnr = NavFodselsnummer(p2000.nav?.ektefelle?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(70, navfnr.getAge())

        assertNotNull(p2000.nav?.krav)
        assertEquals("2015-06-16", p2000.nav?.krav?.dato)

    }

    @Test
    fun `testing av komplett P2000 med utskrift og testing av innsending`() {
        val P2000 = prefillSEDService.prefill(prefillData, persondataCollection,pensjonCollection)

        val json = mapAnyToJson(createMockApiRequest("P2000", "P_BUC_01", P2000.toJson()))
        assertNotNull(json)
    }

    private fun createMockApiRequest(sedName: String, buc: String, payload: String): ApiRequest {
        val items = listOf(InstitusjonItem(country = "NO", institution = "NAVT003"))
        return ApiRequest(
                institutions = items,
                sed = sedName,
                sakId = pesysSaksnummer,
                euxCaseId = null,
                aktoerId = "1000060964183",
                buc = buc,
                subjectArea = "Pensjon",
                payload = payload
        )
    }

}

