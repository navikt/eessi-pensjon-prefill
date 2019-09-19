package no.nav.eessi.pensjon.fagmodul.prefill.sed

import no.nav.eessi.pensjon.fagmodul.models.SEDType
import no.nav.eessi.pensjon.fagmodul.prefill.ApiRequest
import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjonMother
import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonHjelper
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.sed.krav.PrefillP2000
import no.nav.eessi.pensjon.fagmodul.prefill.sed.krav.PrefillP2100
import no.nav.eessi.pensjon.fagmodul.prefill.sed.krav.PrefillP2200
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.PrefillP6000
import no.nav.eessi.pensjon.fagmodul.prefill.tps.PrefillPersonDataFromTPS
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import org.aspectj.lang.annotation.Before
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension


@ExtendWith(MockitoExtension::class)
class PrefillFactoryTest {

    @Mock
    lateinit var prefillNav: PrefillNav

    @Mock
    lateinit var eessiInformasjon: EessiInformasjon

    @Mock
    lateinit var dataFromPEN: PensjonsinformasjonHjelper

    @Mock
    lateinit var dataFromTPS: PrefillPersonDataFromTPS

    lateinit var prefillFactory: PrefillFactory

    @BeforeEach
    fun setupAndRunAtStart() {
            prefillFactory = PrefillFactory(prefillNav, dataFromTPS, eessiInformasjon, dataFromPEN)
    }

    @Test
    fun getH020() {
        val prefill = prefillFactory.createPrefillClass(getMockDataModel("H020"))
        Assertions.assertEquals("PrefillH02X", prefill::class.java.simpleName)
    }

    @Test
    fun getH021() {
        val prefill = prefillFactory.createPrefillClass(getMockDataModel("H021"))
        Assertions.assertEquals("PrefillH02X", prefill::class.java.simpleName)
    }

    @Test
    fun getX005() {
        val prefill = prefillFactory.createPrefillClass(getMockDataModel("X005"))
        Assertions.assertEquals("PrefillX005", prefill::class.java.simpleName)
    }

    @Test
    fun getP2000() {
        val prefill = prefillFactory.createPrefillClass(getMockDataModel("P2000"))
        Assertions.assertEquals("PrefillP2000", prefill::class.java.simpleName)
    }

    @Test
    fun getP2200() {
        val prefill = prefillFactory.createPrefillClass(getMockDataModel("P2200"))
        Assertions.assertEquals("PrefillP2200", prefill::class.java.simpleName)
    }

    @Test
    fun getP2100() {
        val prefill = prefillFactory.createPrefillClass(getMockDataModel("P2100"))
        Assertions.assertEquals("PrefillP2100", prefill::class.java.simpleName)
    }

    @Test
    fun getP5000() {
        val prefill = prefillFactory.createPrefillClass(getMockDataModel("P5000"))
        Assertions.assertEquals("PrefillDefaultSED", prefill::class.java.simpleName)
    }

    @Test
    fun getP4000() {
        val prefill = prefillFactory.createPrefillClass(getMockDataModel("P4000"))
        Assertions.assertEquals("PrefillP4000", prefill::class.java.simpleName)
    }

    @Test
    fun getP8000() {
        val prefill = prefillFactory.createPrefillClass(getMockDataModel("P8000"))
        Assertions.assertEquals("PrefillP8000", prefill::class.java.simpleName)
    }

    @Test
    fun getP7000() {
        val prefill = prefillFactory.createPrefillClass(getMockDataModel("P7000"))
        Assertions.assertEquals("PrefillP7000", prefill::class.java.simpleName)
    }

    @Test
    fun getP10000() {
        val prefill = prefillFactory.createPrefillClass(getMockDataModel("P10000"))
        Assertions.assertEquals("PrefillP10000", prefill::class.java.simpleName)
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