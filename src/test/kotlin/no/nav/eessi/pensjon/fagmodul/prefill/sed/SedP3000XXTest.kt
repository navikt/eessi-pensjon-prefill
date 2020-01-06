package no.nav.eessi.pensjon.fagmodul.prefill.sed

import com.nhaarman.mockitokotlin2.mock
import no.nav.eessi.pensjon.fagmodul.prefill.ApiRequest
import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonHjelper
import no.nav.eessi.pensjon.fagmodul.prefill.person.PersonDataFromTPS
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.tps.BrukerFromTPS
import no.nav.eessi.pensjon.fagmodul.prefill.tps.FodselsnummerMother
import no.nav.eessi.pensjon.fagmodul.prefill.tps.PrefillAdresse
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
    lateinit var dataFromPEN: PensjonsinformasjonHjelper

    @Mock
    lateinit var dataFromTPS: BrukerFromTPS

    lateinit var prefillFactory: PrefillFactory

    private val personFnr = FodselsnummerMother.generateRandomFnr(68)

    @BeforeEach
    fun setupAndRunAtStart() {

        val persondataFraTPS = PrefillTestHelper.setupPersondataFraTPS(setOf(
                PersonDataFromTPS.MockTPS("Person-11000-GIFT.json", personFnr, PersonDataFromTPS.MockTPS.TPSType.PERSON),
                PersonDataFromTPS.MockTPS("Person-12000-EKTE.json", FodselsnummerMother.generateRandomFnr(70), PersonDataFromTPS.MockTPS.TPSType.EKTE)
        ))

        prefillNav = PrefillNav(
                brukerFromTPS = persondataFraTPS,
                prefillAdresse = mock<PrefillAdresse>(),
                institutionid = "NO:noinst002", institutionnavn = "NOINST002, NO INST002, NO")

        prefillFactory = PrefillFactory(prefillNav, dataFromTPS, eessiInformasjon, dataFromPEN)
    }


    @Test
    fun testP3000_AT() {
        val datamodel = getMockDataModel("P3000_AT")
        datamodel.personNr = personFnr

        val prefillClass = prefillFactory.createPrefillClass(datamodel)
        Assertions.assertEquals("PrefillDefaultSED", prefillClass::class.java.simpleName)
        val sed = prefillClass.prefill(datamodel)
        Assertions.assertEquals("P3000_AT", sed.sed)

    }

    @Test
    fun testP3000_IT() {

        val datamodel = getMockDataModel("P3000_IT")
        datamodel.personNr = personFnr

        val prefillClass = prefillFactory.createPrefillClass(datamodel)
        Assertions.assertEquals("PrefillDefaultSED", prefillClass::class.java.simpleName)
        val sed = prefillClass.prefill(datamodel)
        Assertions.assertEquals("P3000_IT", sed.sed)
    }

    @Test
    fun testP3000_SE() {
        val datamodel = getMockDataModel("P3000_SE")
        datamodel.personNr = personFnr

        val prefillClass = prefillFactory.createPrefillClass(datamodel)
        Assertions.assertEquals("PrefillDefaultSED", prefillClass::class.java.simpleName)
        val sed = prefillClass.prefill(datamodel)
        Assertions.assertEquals("P3000_SE", sed.sed)
    }


    private fun getMockDataModel(sedType: String): PrefillDataModel {
        val req = ApiRequest(
                institutions = listOf(),
                sed = sedType,
                sakId = "12231231",
                euxCaseId = "99191999911",
                aktoerId = "00000",
                buc = "P_BUC_01",
                subjectArea = "Pensjon",
                payload = "{}"
        )
        return ApiRequest.buildPrefillDataModelConfirm(req, "12345", null)
    }

}