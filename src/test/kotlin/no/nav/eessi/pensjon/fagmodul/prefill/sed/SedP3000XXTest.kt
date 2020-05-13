package no.nav.eessi.pensjon.fagmodul.prefill.sed

import com.nhaarman.mockitokotlin2.mock
import no.nav.eessi.pensjon.fagmodul.prefill.ApiRequest
import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonService
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.tps.FodselsnummerMother
import no.nav.eessi.pensjon.fagmodul.prefill.tps.PrefillAdresse
import no.nav.eessi.pensjon.fagmodul.prefill.tps.TpsPersonService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class SedP3000XXTest {

    @Mock
    lateinit var prefillNav: PrefillNav

    @Mock
    lateinit var eessiInformasjon: EessiInformasjon

    @Mock
    lateinit var dataFromPEN: PensjonsinformasjonService

    @Mock
    lateinit var dataFromTPS: TpsPersonService

    lateinit var prefillSEDService: PrefillSEDService

    private val personFnr = FodselsnummerMother.generateRandomFnr(68)

    @BeforeEach
    fun setupAndRunAtStart() {

        prefillNav = PrefillNav(
                prefillAdresse = mock<PrefillAdresse>(),
                institutionid = "NO:noinst002",
                institutionnavn = "NOINST002, NO INST002, NO")

        prefillSEDService = PrefillSEDService(prefillNav, dataFromTPS, eessiInformasjon, dataFromPEN)
    }


    @Test
    fun testP3000_AT() {
        val datamodel = getMockDataModel("P3000_AT", personFnr)

        val sed = prefillSEDService.prefill(datamodel)
        Assertions.assertEquals("P3000_AT", sed.sed)

    }

    @Test
    fun testP3000_IT() {

        val datamodel = getMockDataModel("P3000_IT", personFnr)

        val sed = prefillSEDService.prefill(datamodel)
        Assertions.assertEquals("P3000_IT", sed.sed)
    }

    @Test
    fun testP3000_SE() {
        val datamodel = getMockDataModel("P3000_SE", personFnr)

        val sed = prefillSEDService.prefill(datamodel)
        Assertions.assertEquals("P3000_SE", sed.sed)
    }


    private fun getMockDataModel(sedType: String, fnr: String  = "someFnr"): PrefillDataModel {
        val req = ApiRequest(
                institutions = listOf(),
                sed = sedType,
                sakId = "12231231",
                euxCaseId = "99191999911",
                aktoerId = "00000",
                fnr = fnr,
                buc = "P_BUC_01",
                subjectArea = "Pensjon",
                payload = "{}"
        )
        return ApiRequest.buildPrefillDataModelConfirm(req, "12345", null)
    }

}
