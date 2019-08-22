package no.nav.eessi.pensjon.fagmodul.prefill.sed.krav

import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModelMother
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonHjelper
import no.nav.eessi.pensjon.fagmodul.prefill.person.PersonDataFromTPS
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper.lesPensjonsdataFraFil
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper.readJsonResponse
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper.setupPersondataFraTPS
import no.nav.eessi.pensjon.fagmodul.prefill.tps.FodselsnummerMother.generateRandomFnr
import no.nav.eessi.pensjon.fagmodul.prefill.tps.NavFodselsnummer
import no.nav.eessi.pensjon.fagmodul.sedmodel.Nav
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PrefillP2100GLutlandInnvTest {

    private val personFnr = generateRandomFnr(65)
    private val avdodPersonFnr = generateRandomFnr(75)
    private val pesysSaksnummer = "22875355"

    lateinit var prefillData: PrefillDataModel

    lateinit var prefill: Prefill<SED>
    lateinit var prefillNav: PrefillNav
    lateinit var dataFromPEN: PensjonsinformasjonHjelper

    @BeforeEach
    fun setup() {
        val persondataFraTPS = setupPersondataFraTPS(setOf(
                PersonDataFromTPS.MockTPS("Person-30000.json", personFnr, PersonDataFromTPS.MockTPS.TPSType.PERSON),
                PersonDataFromTPS.MockTPS("Person-31000.json", avdodPersonFnr, PersonDataFromTPS.MockTPS.TPSType.PERSON)
        ))
        prefillNav = PrefillNav(
                preutfyllingPersonFraTPS = persondataFraTPS,
                institutionid = "NO:noinst002", institutionnavn = "NOINST002, NO INST002, NO")

        dataFromPEN = lesPensjonsdataFraFil("P2100-GL-UTL-INNV.xml")

        prefill = PrefillP2100(prefillNav, dataFromPEN, persondataFraTPS)

        prefillData = PrefillDataModelMother.initialPrefillDataModel("P2100", personFnr).apply {
            skipSedkey = listOf("PENSED")
            penSaksnummer = pesysSaksnummer
            avdodAktorID = "112233445566"
            avdod = avdodPersonFnr
            partSedAsJson = mutableMapOf(
                    "PersonInfo" to readJsonResponse("other/person_informasjon_selvb.json"),
                    "P4000" to readJsonResponse("other/p4000_trygdetid_part.json"))
        }
    }

    @Test
    fun `forventet korrekt utfylt P2200 uforepensjon med kap4 og 9`() {
        val p2100 = prefill.prefill(prefillData)

        val p2100gjenlev = SED(
                sed = "P2100",
                pensjon = p2100.pensjon,
                nav = Nav( krav = p2100.nav?.krav )
        )

        val sed = p2100gjenlev
        assertNotNull(sed.nav?.krav)
        assertEquals("2019-06-01", sed.nav?.krav?.dato)

    }

    @Test
    fun `forventet korrekt utfylt P2200 uforepensjon med mockdata fra testfiler`() {
        val p2100 = prefill.prefill(prefillData)

        prefill.validate(p2100)

        assertEquals(null, p2100.nav?.barn)

        assertEquals("", p2100.nav?.bruker?.arbeidsforhold?.get(0)?.yrke)
        assertEquals("2018-11-12", p2100.nav?.bruker?.arbeidsforhold?.get(0)?.planlagtstartdato)
        assertEquals("2018-11-14", p2100.nav?.bruker?.arbeidsforhold?.get(0)?.planlagtpensjoneringsdato)
        assertEquals("07", p2100.nav?.bruker?.arbeidsforhold?.get(0)?.type)

        assertEquals("foo", p2100.nav?.bruker?.bank?.navn)
        assertEquals("bar", p2100.nav?.bruker?.bank?.konto?.sepa?.iban)
        assertEquals("baz", p2100.nav?.bruker?.bank?.konto?.sepa?.swift)

        assertEquals("BAMSE LUR", p2100.nav?.bruker?.person?.fornavn)
        assertEquals("MOMBALO", p2100.nav?.bruker?.person?.etternavn)
        val navfnr1 = NavFodselsnummer(p2100.nav?.bruker?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(75, navfnr1.getAge())
        assertEquals("M", p2100.nav?.bruker?.person?.kjoenn)
        assertEquals("02", p2100.nav?.bruker?.person?.sivilstand?.first()?.status)

        assertNotNull(p2100.nav?.bruker?.person?.pin)
        val pinlist = p2100.nav?.bruker?.person?.pin
        val pinitem = pinlist?.get(0)
        assertEquals(null, pinitem?.sektor)
        assertEquals("NOINST002, NO INST002, NO", pinitem?.institusjonsnavn)
        assertEquals("NO:noinst002", pinitem?.institusjonsid)
        assertEquals(avdodPersonFnr, pinitem?.identifikator)

        assertEquals("BAMSE ULUR", p2100.pensjon?.gjenlevende?.person?.fornavn)
        assertEquals("DOLLY", p2100.pensjon?.gjenlevende?.person?.etternavn)
        val navfnr2 = NavFodselsnummer(p2100.pensjon?.gjenlevende?.person?.pin?.get(0)?.identifikator!!)
        assertEquals(65, navfnr2.getAge())
        assertEquals("K", p2100.pensjon?.gjenlevende?.person?.kjoenn)
        assertEquals("08", p2100.pensjon?.gjenlevende?.person?.sivilstand?.first()?.status)

    }

}

