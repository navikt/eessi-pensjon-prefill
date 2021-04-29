package no.nav.eessi.pensjon.fagmodul.prefill.sed

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.sed.SedType
import no.nav.eessi.pensjon.fagmodul.models.PersonDataCollection
import no.nav.eessi.pensjon.fagmodul.models.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.ApiRequest
import no.nav.eessi.pensjon.fagmodul.prefill.LagPDLPerson
import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.pdl.FodselsnummerMother
import no.nav.eessi.pensjon.fagmodul.prefill.pdl.PrefillPDLAdresse
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonService
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillPDLNav
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SedP3000XXTest {

    var eessiInformasjon: EessiInformasjon = mockk()
    var dataFromPEN: PensjonsinformasjonService = mockk()

    private lateinit var prefillSEDService: PrefillSEDService

    private val personFnr = FodselsnummerMother.generateRandomFnr(68)
    private lateinit var personDataCollection: PersonDataCollection

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

        prefillSEDService = PrefillSEDService(dataFromPEN, eessiInformasjon, prefillNav)
    }

    @Test
    fun testP3000_AT() {
        val datamodel = getMockDataModel("P3000_AT", personFnr)

        val sed = prefillSEDService.prefill(datamodel, personDataCollection)
        Assertions.assertEquals(SedType.P3000_AT, sed.type)
    }

    @Test
    fun testP3000_IT() {

        val datamodel = getMockDataModel("P3000_IT", personFnr)

        val sed = prefillSEDService.prefill(datamodel, personDataCollection)
        Assertions.assertEquals(SedType.P3000_IT, sed.type)
    }

    @Test
    fun testP3000_SE() {
        val datamodel = getMockDataModel("P3000_SE", personFnr)

        val sed = prefillSEDService.prefill(datamodel, personDataCollection)
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
