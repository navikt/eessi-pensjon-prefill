package no.nav.eessi.pensjon.fagmodul.prefill.sed

import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.person.PersonDataFromTPS
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.utils.validateJson
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.skyscreamer.jsonassert.JSONAssert
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class `PrefillP7000-AP-21975717Test` : AbstractPrefillIntegrationTestHelper() {

    private val personFnr = "01071843352"

    private val pesysSaksnummer = "21975717"

    lateinit var prefillData: PrefillDataModel
    lateinit var pendata: Pensjonsinformasjon
    lateinit var prefill: Prefill<SED>

    @Before
    fun setup() {
        val prefillPersonDataFromTPS = mockPrefillPersonDataFromTPS(setOf(
                PersonDataFromTPS.MockTPS("Person-11000-GIFT.json", personFnr, PersonDataFromTPS.MockTPS.TPSType.PERSON),
                PersonDataFromTPS.MockTPS("Person-12000-EKTE.json", PersonDataFromTPS.generateRandomFnr(70), PersonDataFromTPS.MockTPS.TPSType.EKTE)
        ))

        prefillData = generatePrefillData("P7000", "02345678901", sakId = pesysSaksnummer)
        prefillData.personNr = personFnr
        prefillData.partSedAsJson["PersonInfo"] = readJsonResponse("other/person_informasjon_selvb.json")
        prefillData.partSedAsJson["P4000"] = readJsonResponse("other/p4000_trygdetid_part.json")

        val prefillNav = PrefillNav(prefillPersonDataFromTPS, institutionid = "NO:noinst002", institutionnavn = "NOINST002, NO INST002, NO")

        prefill = PrefillP7000(prefillNav)
    }

    @Test
    fun `forventet korrekt utfylt P7000 Melding om vedtakssammendrag med MockData fra testfiler`() {
        val p7000 = prefill.prefill(prefillData)

        assertEquals("OKOULOV", p7000.nav?.ektefelle?.person?.etternavn)
        assertEquals("M", p7000.pensjon?.bruker?.person?.kjoenn)
    }

    @Test
    fun `forventet P7000 er lik sedfil med MockData fra testfiler`() {
        val filepath = "src/test/resources/json/nav/P7000_OK_NAV.json"
        val json = String(Files.readAllBytes(Paths.get(filepath)))
        assertTrue(validateJson(json))

        val p7000 = prefill.prefill(prefillData)

        val sed = p7000.toJsonSkipEmpty()

        assertTrue(validateJson(sed))
        JSONAssert.assertEquals(json, sed, true)
    }

}

