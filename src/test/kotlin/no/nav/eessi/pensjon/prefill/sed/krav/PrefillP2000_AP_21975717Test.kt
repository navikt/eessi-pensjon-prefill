package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.BucType.P_BUC_01
import no.nav.eessi.pensjon.eux.model.SedType.P2000
import no.nav.eessi.pensjon.eux.model.sed.Nav
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.personoppslag.pdl.model.UtenlandskIdentifikasjonsnummer
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.PersonPDLMock.medFodsel
import no.nav.eessi.pensjon.prefill.PersonPDLMock.mockMeta
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.KrrPerson.Companion.validateEmail
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother.initialPrefillDataModel
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
import java.time.LocalDate

class PrefillP2000_AP_21975717Test {

    private val pesysSaksnummer = "21975717"

    private val giftFnr = FodselsnummerGenerator.generateFnrForTest(68)
    private val ekteFnr = FodselsnummerGenerator.generateFnrForTest(70)

    private lateinit var prefillData: PrefillDataModel
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var persondataCollection: PersonDataCollection
    private lateinit var pensjonCollection: PensjonCollection

    @BeforeEach
    fun setup() {
        persondataCollection = PersonPDLMock.createEnkelFamilie(giftFnr, ekteFnr).copy(
            gjenlevendeEllerAvdod = PersonPDLMock.createEnkelFamilie(giftFnr, ekteFnr).forsikretPerson?.copy(
                utenlandskIdentifikasjonsnummer = listOf(
                    UtenlandskIdentifikasjonsnummer(
                        "123123123",
                        "SWE",
                        false,
                        metadata = mockMeta(
                        )),
                    UtenlandskIdentifikasjonsnummer(
                        "222-12-3123",
                        "USA",
                        false,
                        metadata = mockMeta(
                        ))
                ))
        )

        val prefillNav = PrefillPDLNav(
            prefillAdresse = mockk<PrefillPDLAdresse> {
                every { hentLandkode(null) } returns "NO"
                every { hentLandkode(eq("SWE")) } returns "SE"
                every { hentLandkode(eq("USA")) } returns "US"
                every { hentLandkode(eq("NOR")) } returns "NO"
                every { createPersonAdresse(any()) } returns mockk(relaxed = true)
            },
            institutionid = "NO:noinst002",
            institutionnavn = "NOINST002, NO INST002, NO"
        )

        val dataFromPEN = lesPensjonsdataFraFil("/pensjonsinformasjon/krav/KravAlderEllerUfore_AP_UTLAND.xml")

        prefillData = initialPrefillDataModel(P2000, giftFnr, penSaksnummer = pesysSaksnummer).apply {
            partSedAsJson["PersonInfo"] = readJsonResponse("/json/nav/other/person_informasjon_selvb.json")
            partSedAsJson["P4000"] = readJsonResponse("/json/nav/other/p4000_trygdetid_part.json")
        }
        val innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = dataFromPEN)
        pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)

        prefillSEDService = PrefillSEDService(EessiInformasjon(), prefillNav, mockk())

    }

    @Test
    fun `forventet korrekt utfylt P2000 alderpensjon med kap4 og 9`() {
        val p2000 = prefillSEDService.prefill(prefillData, persondataCollection, pensjonCollection)

        val p2000pensjon = SED(
                type = P2000,
                pensjon = p2000.pensjon,
                nav = Nav( krav = p2000.nav?.krav )
        )

        assertNotNull(p2000pensjon.nav?.krav)
        assertEquals("2015-06-16", p2000pensjon.nav?.krav?.dato)
        assertEquals("NO", p2000.nav?.bruker?.person?.statsborgerskap?.first()?.land)
        assertEquals("SE", p2000.nav?.bruker?.person?.statsborgerskap?.last()?.land)
        assertEquals(giftFnr, p2000.nav?.bruker?.person?.pin?.first()?.identifikator)
        assertEquals("123123123", p2000.nav?.bruker?.person?.pin?.last()?.identifikator)

        assertEquals("NO", p2000.nav?.bruker?.person?.pin?.first()?.land)
        assertEquals("SE", p2000.nav?.bruker?.person?.pin?.last()?.land)
        assertEquals("SE", p2000.nav?.bruker?.person?.pin?.last()?.land)

    }

    @Test
    fun `forventet korrekt utfylt P2000 med epost og telefonummer`() {
        val p2000 = prefillSEDService.prefill(prefillData, persondataCollection, pensjonCollection)

        assertEquals(prefillData.bruker.telefonKrr, p2000.nav?.bruker?.person?.kontakt?.telefon?.get(0)?.nummer)
        assertEquals(prefillData.bruker.epostKrr, p2000.nav?.bruker?.person?.kontakt?.email?.get(0)?.adresse)
    }

    @Test
    fun `forventet korrekt utfylt P2000 med telefonummer og uten epost som inkluderer underscore`() {
        val edited = prefillData.copy(
            bruker = prefillData.bruker.copy(
                epostKrr = "somethin_g@gmail.com".validateEmail()
            )
        )
        val p2000 = prefillSEDService.prefill(edited, persondataCollection, pensjonCollection)

        assertEquals(edited.bruker.telefonKrr, p2000.nav?.bruker?.person?.kontakt?.telefon?.get(0)?.nummer)
        assertEquals(null, p2000.nav?.bruker?.person?.kontakt?.email)
    }


    @Test
    fun `forventet korrekt utfylt P2000 alderpersjon med mockdata fra testfiler`() {
        val p2000 = prefillSEDService.prefill(prefillData, persondataCollection, pensjonCollection)

        assertEquals(null, p2000.nav?.barn)

        assertEquals("", p2000.nav?.bruker?.arbeidsforhold?.get(0)?.yrke)
        assertEquals("2018-11-11", p2000.nav?.bruker?.arbeidsforhold?.get(0)?.planlagtstartdato)
        assertEquals("2018-11-13", p2000.nav?.bruker?.arbeidsforhold?.get(0)?.planlagtpensjoneringsdato)
        assertEquals("07", p2000.nav?.bruker?.arbeidsforhold?.get(0)?.type)


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
        assertEquals(giftFnr, pinitem?.identifikator)

        assertEquals("THOR-DOPAPIR", p2000.nav?.ektefelle?.person?.fornavn)
        assertEquals("RAGNAROK", p2000.nav?.ektefelle?.person?.etternavn)

        assertEquals(ekteFnr, p2000.nav?.ektefelle?.person?.pin?.get(0)?.identifikator)
        assertEquals(giftFnr, p2000.nav?.bruker?.person?.pin?.get(0)?.identifikator)

        assertEquals("NO", p2000.nav?.ektefelle?.person?.pin?.get(0)?.land)

        val navfnr = Fodselsnummer.fra(p2000.nav?.ektefelle?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(70, navfnr?.getAge())

        assertNotNull(p2000.nav?.krav)
        assertEquals("2015-06-16", p2000.nav?.krav?.dato)
    }

    @Test
    fun `Preutfylling av P2000 med barn over 18 aar`() {
        val personDataCollection = PersonDataCollection(
                forsikretPerson = PersonPDLMock.createWith(),
                gjenlevendeEllerAvdod = PersonPDLMock.createWith(),
                barnPersonList = listOf(PersonPDLMock.createWith(fornavn = "Barn", etternavn = "Barnesen", fnr = "01010436857")
                    .medFodsel(LocalDate.of(2004, 1, 1))
                )
        )

        val p2000 = prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection)

        assertEquals("Barn", p2000.nav?.barn?.get(0)?.person?.fornavn)
        assertEquals("2004-01-01", p2000.nav?.barn?.get(0)?.person?.foedselsdato)

    }

    @Test
    fun `testing av komplett P2000 med utskrift og testing av innsending`() {
        val p2000 = prefillSEDService.prefill(prefillData, persondataCollection, pensjonCollection)

        val json = mapAnyToJson(createMockApiRequest(p2000.toJson()))
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

