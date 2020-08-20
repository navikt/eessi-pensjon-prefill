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
class PrefillP2000MedUforeSakTest {

    private val personFnr = generateRandomFnr(68)
    private val pesysSaksnummer = "22874955"

    lateinit var prefillData: PrefillDataModel

    lateinit var prefillNav: PrefillNav
    lateinit var dataFromPEN: PensjonsinformasjonService
    lateinit var prefillSEDService: PrefillSEDService

    @BeforeEach
    fun setup() {
        val mockPersonV3Service = mock<PersonV3Service>()

        prefillNav = PrefillNav(
                prefillAdresse = mock<PrefillAdresse>(),
                institutionid = "NO:noinst002",
                institutionnavn = "NOINST002, NO INST002, NO")

        dataFromPEN = lesPensjonsdataFraFil("P2200-UP-INNV.xml")

        prefillData = PrefillDataModelMother.initialPrefillDataModel("P2000", personFnr, penSaksnummer = pesysSaksnummer)

        prefillSEDService = PrefillSEDService(prefillNav, mockPersonV3Service, EessiInformasjon(), dataFromPEN)
    }

    @Test
    fun `forventer exception - ikke relevant saktype for krav-SED - uforesak ikke relevant for P2000`() {
        assertThrows<FeilSakstypeForSedException>{
            prefillSEDService.prefill(prefillData)
        }
    }
}

