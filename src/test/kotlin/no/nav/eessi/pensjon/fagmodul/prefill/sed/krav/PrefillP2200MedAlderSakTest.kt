package no.nav.eessi.pensjon.fagmodul.prefill.sed.krav

import com.nhaarman.mockitokotlin2.mock
import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModelMother
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonService
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.sed.FeilSakstypeForSedException
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper.lesPensjonsdataFraFil
import no.nav.eessi.pensjon.fagmodul.prefill.tps.FodselsnummerMother.generateRandomFnr
import no.nav.eessi.pensjon.fagmodul.prefill.tps.PrefillAdresse
import no.nav.eessi.pensjon.personoppslag.personv3.PersonV3Service
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class PrefillP2200MedAlderSakTest {

    private val personFnr = generateRandomFnr(68)
    private val pesysSaksnummer = "14069110"

    lateinit var prefillData: PrefillDataModel
    lateinit var dataFromPEN: PensjonsinformasjonService
    private lateinit var prefillSEDService: PrefillSEDService


    @BeforeEach
    fun setup() {
        val mockPersonV3Service = mock<PersonV3Service>()

        val prefillNav = PrefillNav(
                prefillAdresse = mock<PrefillAdresse>(),
                institutionid = "NO:noinst002",
                institutionnavn = "NOINST002, NO INST002, NO")

        dataFromPEN = lesPensjonsdataFraFil("PensjonsinformasjonSaksliste-AP-14069110.xml")

        prefillData = PrefillDataModelMother.initialPrefillDataModel("P2200", personFnr, penSaksnummer = pesysSaksnummer)

        prefillSEDService = PrefillSEDService(prefillNav, mockPersonV3Service, EessiInformasjon(), dataFromPEN)
    }

    @Test
    fun `forventer exception - ikke relevant saktype for krav-SED - aldersak ikke relevant for P2200`() {
        assertThrows<FeilSakstypeForSedException>{
            prefillSEDService.prefill(prefillData)
        }
    }
}

