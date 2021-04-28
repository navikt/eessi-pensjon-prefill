package no.nav.eessi.pensjon.fagmodul.prefill.sed

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.sed.P7000
import no.nav.eessi.pensjon.eux.model.sed.SedType
import no.nav.eessi.pensjon.fagmodul.models.PersonDataCollection
import no.nav.eessi.pensjon.fagmodul.models.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.models.PrefillDataModelMother
import no.nav.eessi.pensjon.fagmodul.prefill.PersonPDLMock
import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.pdl.PrefillPDLAdresse
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonService
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillPDLNav
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

    var pensjonInformasjonService: PensjonsinformasjonService = mockk()

    private val personFnr = "01071843352"
    private lateinit var prefillData: PrefillDataModel
    private lateinit var prefillPDLNav: PrefillPDLNav
    private lateinit var personCollection: PersonDataCollection

    @BeforeEach
    fun setup() {
        val person = PersonPDLMock.createWith(etternavn = "BALDER")
        personCollection = PersonDataCollection(person, person, barnPersonList = emptyList())

        prefillPDLNav = PrefillPDLNav(
            prefillAdresse = mockk<PrefillPDLAdresse> {
                every { hentLandkode(any()) } returns "NO"
                every { createPersonAdresse(any()) } returns mockk()
            },
            institutionid = "NO:noinst002",
            institutionnavn = "NOINST002, NO INST002, NO"
        )

        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P7000,penSaksnummer = "21975717", pinId = personFnr, vedtakId = "12312312")
    }

    @Test
    fun `forventet korrekt utfylt P7000 Melding om vedtakssammendrag med MockData fra testfiler`() {
        val prefillSEDService = PrefillSEDService(pensjonInformasjonService, EessiInformasjon(), prefillPDLNav)
        val p7000 = prefillSEDService.prefill(prefillData, personCollection) as P7000

        assertEquals("BALDER", p7000.nav?.ektefelle?.person?.etternavn)
        assertEquals("M", p7000.p7000Pensjon.bruker?.person?.kjoenn)

        assertEquals("1988-07-12", p7000.nav?.bruker?.person?.foedselsdato)
    }

    @Test
    fun `forventet P7000 er lik sedfil med MockData fra testfiler`() {
        val filepath = "src/test/resources/json/nav/P7000_OK_NAV.json"
        val json = String(Files.readAllBytes(Paths.get(filepath)))
        assertTrue(validateJson(json))

        val prefillSEDService = PrefillSEDService(pensjonInformasjonService, EessiInformasjon(), prefillPDLNav)
        val p7000 = prefillSEDService.prefill(prefillData, personCollection)

        val sed = p7000.toJsonSkipEmpty()

        assertTrue(validateJson(sed))
        JSONAssert.assertEquals(json, sed, true)
    }

}

