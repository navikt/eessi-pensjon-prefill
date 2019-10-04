package no.nav.eessi.pensjon.fagmodul.prefill.sed

import no.nav.eessi.pensjon.fagmodul.prefill.ApiRequest
import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonHjelper
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.tps.PrefillPersonDataFromTPS
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension


@ExtendWith(MockitoExtension::class)
class PrefillFactoryParameterizedTest {

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

    companion object {
        @JvmStatic

        fun `collection data`(): Collection<Array<Any>> {
            return listOf(
                    arrayOf(1, "P2000", "PrefillP2000"),
                    arrayOf(2, "H020", "PrefillH02X"),
                    arrayOf(2, "H021", "PrefillH02X"),
                    arrayOf(2, "X005", "PrefillX005"),
                    arrayOf(2, "P2000", "PrefillP2000"),
                    arrayOf(2, "P2200", "PrefillP2200"),
                    arrayOf(2, "P2100", "PrefillP2100"),
                    arrayOf(2, "P5000", "PrefillDefaultSED"),
                    arrayOf(2, "P4000", "PrefillP4000"),
                    arrayOf(2, "P8000", "PrefillP8000"),
                    arrayOf(2, "P7000", "PrefillP7000"),
                    arrayOf(2, "P10000", "PrefillP10000")
            )
        }
    }

    @ParameterizedTest
    @MethodSource("collection data")
    fun `create mock and validate prefill class` (index: Int, sedtype: String, prefillClassName: String) {
        val prefill = prefillFactory.createPrefillClass(getMockDataModel(sedtype))
        Assertions.assertEquals(prefillClassName, prefill::class.java.simpleName)
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