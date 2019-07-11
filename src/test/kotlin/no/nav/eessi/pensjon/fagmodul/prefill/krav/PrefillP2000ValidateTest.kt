package no.nav.eessi.pensjon.fagmodul.prefill.krav

import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.models.Nav
import no.nav.eessi.pensjon.fagmodul.models.Pensjon
import no.nav.eessi.pensjon.fagmodul.models.SED
import no.nav.eessi.pensjon.fagmodul.prefill.PensjonsinformasjonHjelper
import no.nav.eessi.pensjon.fagmodul.prefill.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.ValidationException
import no.nav.eessi.pensjon.fagmodul.prefill.nav.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.nav.PrefillPersonDataFromTPS
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class PrefillP2000ValidateTest {


    @Mock
    private lateinit var mockPensjonsinformasjonHjelper: PensjonsinformasjonHjelper

    @Mock
    private lateinit var mockPrefillPersonDataFromTPS: PrefillPersonDataFromTPS

    @Mock
    private lateinit var mockPrefillNAV: PrefillNav

    private lateinit var prefillP2000: PrefillP2000

    @Before
    fun before() {
        prefillP2000 = PrefillP2000(mockPrefillNAV, mockPrefillPersonDataFromTPS, mockPensjonsinformasjonHjelper)
    }

    @Test(expected = ValidationException::class)
    fun `call prefillAndPreview| Exception ved validating SED`() {
        prefillP2000.validate(generateMockP2000ForValidatorError(generatePrefillModel()))
    }

    private fun generatePrefillModel(): PrefillDataModel {
        return PrefillDataModel().apply {
            euxCaseID = "1000"
            sed = SED.create("P2000")
            buc = "P_BUC_01"
            institution = listOf(
                    InstitusjonItem(
                            country = "NO",
                            institution = "DUMMY"
                    )
            )
            penSaksnummer = "123456789999"
            personNr = "12345678901"
        }
    }

    private fun generateMockP2000ForValidatorError(prefillModel: PrefillDataModel): SED {
        val mocksed = prefillModel.sed
        mocksed.nav = Nav()
        mocksed.pensjon = Pensjon()
        return mocksed
    }
}