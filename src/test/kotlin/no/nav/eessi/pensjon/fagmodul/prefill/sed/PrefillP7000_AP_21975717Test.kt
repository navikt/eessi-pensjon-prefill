package no.nav.eessi.pensjon.fagmodul.prefill.sed

import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.person.PersonDataFromTPS
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper.setupPersondataFraTPS
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper.readJsonResponse
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
class PrefillP7000_AP_21975717Test {

    private val personFnr = "01071843352"

    lateinit var prefillData: PrefillDataModel
    lateinit var prefill: Prefill<SED>

    @Before
    fun setup() {
        val persondataFraTPS = setupPersondataFraTPS(setOf(
                PersonDataFromTPS.MockTPS("Person-11000-GIFT.json", personFnr, PersonDataFromTPS.MockTPS.TPSType.PERSON),
                PersonDataFromTPS.MockTPS("Person-12000-EKTE.json", PersonDataFromTPS.generateRandomFnr(70), PersonDataFromTPS.MockTPS.TPSType.EKTE)
        ))

        prefill = PrefillP7000(
                prefillNav = PrefillNav(
                        preutfyllingPersonFraTPS = persondataFraTPS,
                        institutionid = "NO:noinst002", institutionnavn = "NOINST002, NO INST002, NO"))

        prefillData = PrefillDataModel().apply {
            rinaSubject = "Pensjon"
            sed = SED("P7000")
            penSaksnummer = "21975717"
            vedtakId = "12312312"
            buc = "P_BUC_99"
            aktoerID = "123456789"
            personNr = personFnr
            institution = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
            partSedAsJson = mutableMapOf(
                    "PersonInfo" to readJsonResponse("other/person_informasjon_selvb.json"),
                    "P4000" to readJsonResponse("other/p4000_trygdetid_part.json"))
        }
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

