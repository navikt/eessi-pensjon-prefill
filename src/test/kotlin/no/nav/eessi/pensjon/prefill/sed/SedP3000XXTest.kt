package no.nav.eessi.pensjon.prefill.sed

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.sed.SedType
import no.nav.eessi.pensjon.prefill.ApiRequest
import no.nav.eessi.pensjon.prefill.LagPDLPerson
import no.nav.eessi.pensjon.prefill.PensjonsinformasjonService
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModel
import no.nav.eessi.pensjon.prefill.models.eessi.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.pdl.FodselsnummerMother
import no.nav.eessi.pensjon.prefill.models.pdl.PrefillPDLAdresse
import no.nav.eessi.pensjon.prefill.models.person.PrefillPDLNav
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SedP3000XXTest {

    var eessiInformasjon: EessiInformasjon = mockk()
    var dataFromPEN: PensjonsinformasjonService = mockk()

    private lateinit var prefillSEDService: PrefillSEDService

    private val personFnr = FodselsnummerMother.generateRandomFnr(68)
    private lateinit var personDataCollection: PersonDataCollection
    private lateinit var pensjonCollection: PensjonCollection

    @BeforeEach
    fun setupAndRunAtStart() {
        val person = LagPDLPerson.lagPerson(personFnr, "Ola", "Testbruker")
        personDataCollection = PersonDataCollection(person, person)

        val prefillNav = PrefillPDLNav(
            prefillAdresse = mockk<PrefillPDLAdresse> {
                every { hentLandkode(any()) } returns "NO"
                every { createPersonAdresse(any()) } returns mockk()
            },
            institutionid = "NO:noinst002",
            institutionnavn = "NOINST002, NO INST002, NO"
        )

        prefillSEDService = PrefillSEDService(eessiInformasjon, prefillNav)
    }

    @Test
    fun testP3000_AT() {
        val datamodel = getMockDataModel("P3000_AT", personFnr)
        pensjonCollection = PensjonCollection(sedType = SedType.P3000_AT)

        val sed = prefillSEDService.prefill(datamodel, personDataCollection,pensjonCollection)
        Assertions.assertEquals(SedType.P3000_AT, sed.type)
    }

    @Test
    fun testP3000_IT() {
        val datamodel = getMockDataModel("P3000_IT", personFnr)
        pensjonCollection = PensjonCollection(sedType = SedType.P3000_IT)

        val sed = prefillSEDService.prefill(datamodel, personDataCollection,pensjonCollection)
        Assertions.assertEquals(SedType.P3000_IT, sed.type)
    }

    @Test
    fun testP3000_SE() {
        val datamodel = getMockDataModel("P3000_SE", personFnr)
        pensjonCollection = PensjonCollection(sedType = SedType.P3000_SE)

        val sed = prefillSEDService.prefill(datamodel, personDataCollection,pensjonCollection)
        Assertions.assertEquals(SedType.P3000_SE, sed.type)
    }


    private fun getMockDataModel(SedType: String, fnr: String  = "someFnr"): PrefillDataModel {
        val req = ApiRequest(
                institutions = listOf(),
                sed = SedType,
                sakId = "12231231",
                euxCaseId = "99191999911",
                aktoerId = "00000",
                fnr = fnr,
                buc = "P_BUC_01",
                subjectArea = "Pensjon",
                payload = "{}"
        )
        return ApiRequest.buildPrefillDataModelOnExisting(req, "12345", null)
    }
}
