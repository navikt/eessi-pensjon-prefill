package no.nav.eessi.pensjon.prefill.sed

import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.BucType.P_BUC_01
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.SedType.*
import no.nav.eessi.pensjon.prefill.BasePrefillNav
import no.nav.eessi.pensjon.prefill.LagPdlPerson
import no.nav.eessi.pensjon.prefill.models.pensjon.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.shared.api.ApiRequest
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SedP3000XXTest {
//    var dataFromPEN: PensjonsinformasjonService = mockk()
    private lateinit var prefillSEDService: PrefillSEDService

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(68)
    private lateinit var personDataCollection: PersonDataCollection
    private lateinit var pensjonCollection: PensjonCollection

    @BeforeEach
    fun setupAndRunAtStart() {
        val person = LagPdlPerson.lagPerson(personFnr, "Ola", "Testbruker")
        personDataCollection = PersonDataCollection(person, person)
        prefillSEDService = BasePrefillNav.createPrefillSEDService()
    }

    @Test
    fun testP3000_AT() {
        val datamodel = getMockDataModel(P3000_AT, personFnr)
        pensjonCollection = PensjonCollection(sedType = P3000_AT)

        val sed = prefillSEDService.prefill(datamodel, personDataCollection, pensjonCollection, null,)
        Assertions.assertEquals(P3000_AT, sed.type)
    }

    @Test
    fun testP3000_IT() {
        val datamodel = getMockDataModel(P3000_IT, personFnr)
        pensjonCollection = PensjonCollection(sedType = P3000_IT)

        val sed = prefillSEDService.prefill(datamodel, personDataCollection, pensjonCollection, null,)
        Assertions.assertEquals(P3000_IT, sed.type)
    }

    @Test
    fun testP3000_SE() {
        val datamodel = getMockDataModel(P3000_SE, personFnr)
        pensjonCollection = PensjonCollection(sedType = P3000_SE)

        val sed = prefillSEDService.prefill(datamodel, personDataCollection, pensjonCollection, null,)
        Assertions.assertEquals(P3000_SE, sed.type)
    }


    private fun getMockDataModel(sed: SedType, fnr: String  = "someFnr"): PrefillDataModel {
        val req = ApiRequest(
                institutions = listOf(),
                sed = sed,
                sakId = "12231231",
                euxCaseId = "99191999911",
                aktoerId = "00000",
                fnr = fnr,
                buc = P_BUC_01,
                subjectArea = "Pensjon",
                payload = "{}"
        )
        return ApiRequest.buildPrefillDataModelOnExisting(req, PersonInfo("12345", req.aktoerId!!), null)
    }
}
