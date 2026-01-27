package no.nav.eessi.pensjon.prefill.sed

import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.P7000
import no.nav.eessi.pensjon.prefill.BasePrefillNav
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import no.nav.eessi.pensjon.utils.validateJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.nio.file.Files
import java.nio.file.Paths

class PrefillP7000_AP_21975717Test {


    private val personFnr = "01071843352"
    private lateinit var prefillPDLNav: PrefillPDLNav
    private lateinit var prefillData: PrefillDataModel
    private lateinit var pensjonCollection: PensjonCollection
    private lateinit var personCollection: PersonDataCollection
    private lateinit var prefillSEDService: PrefillSEDService

    @BeforeEach
    fun setup() {
        prefillPDLNav = BasePrefillNav.createPrefillNav()
        val person = PersonPDLMock.createWith(etternavn = "BALDER")
        personCollection = PersonDataCollection(person, person, barnPersonList = emptyList())

        pensjonCollection = PensjonCollection(sedType = SedType.P7000)
        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P7000,
            penSaksnummer = "21975717", pinId = personFnr, vedtakId = "12312312")
        prefillSEDService = BasePrefillNav.createPrefillSEDService()
    }

    @Test
    fun `forventet korrekt utfylt P7000 Melding om vedtakssammendrag med MockData fra testfiler`() {
        val p7000 = prefillSEDService.prefill(prefillData, personCollection, pensjonCollection, null,) as P7000

        assertEquals("BALDER", p7000.nav?.ektefelle?.person?.etternavn)
        assertEquals("M", p7000.pensjon?.bruker?.person?.kjoenn)
        assertEquals("1988-07-12", p7000.nav?.bruker?.person?.foedselsdato)
    }

    @Test
    fun `forventet P7000 er lik sedfil med MockData fra testfiler`() {
        val filepath = "src/test/resources/json/nav/P7000_OK_NAV.json"
        val json = String(Files.readAllBytes(Paths.get(filepath)))
        assertTrue(validateJson(json))

        val p7000 = prefillSEDService.prefill(prefillData, personCollection, pensjonCollection, null,)
        val sed = p7000.toJsonSkipEmpty()

        assertTrue(validateJson(sed))
        JSONAssert.assertEquals(json, sed, true)
    }

}

