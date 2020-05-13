package no.nav.eessi.pensjon.fagmodul.prefill.sed.krav

import com.nhaarman.mockitokotlin2.mock
import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.model.PersonId
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonService
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.fagmodul.prefill.tps.PrefillAdresse
import no.nav.eessi.pensjon.fagmodul.prefill.tps.TpsPersonService
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class PrefillP2000ValidateTest {

    private lateinit var prefillNav: PrefillNav

    @Mock
    lateinit var dataFromPEN: PensjonsinformasjonService

    @Mock
    lateinit var persondataFraTPS: TpsPersonService

    private lateinit var prefillSEDService: PrefillSEDService


    @BeforeEach
    fun before() {
        prefillNav = PrefillNav(prefillAdresse = mock<PrefillAdresse>(),
                institutionid = "NO:noinst002",
                institutionnavn = "NOINST002, NO INST002, NO")
        prefillSEDService = PrefillSEDService(prefillNav, persondataFraTPS, EessiInformasjon(), dataFromPEN)

    }

    @Test
    fun `call prefillAndPreview  Exception ved validating SED`() {
        assertThrows<ValidationException> {
            prefillSEDService.prefill(generatePrefillModel())
        }
    }

    private fun generatePrefillModel(): PrefillDataModel {
        return PrefillDataModel(penSaksnummer = "123456789999", bruker = PersonId("12345678901", "dummy"), avdod = null).apply {
            euxCaseID = "1000"
            sed = SED("P2000")
            buc = "P_BUC_01"
            institution = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        }
    }
}
