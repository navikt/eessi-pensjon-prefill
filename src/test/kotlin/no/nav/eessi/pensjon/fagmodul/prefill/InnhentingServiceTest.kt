package no.nav.eessi.pensjon.fagmodul.prefill

import no.nav.eessi.pensjon.fagmodul.eux.EuxService
import no.nav.eessi.pensjon.fagmodul.models.SEDType
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class InnhentingServiceTest {

    @Spy
    lateinit var mockEuxService: EuxService

    @Mock
    lateinit var mockPrefillSEDService: PrefillSEDService

    @Mock
    lateinit var personService: PersonDataService

    private lateinit var innhentingService: InnhentingService

    @BeforeEach
    fun setUp() {
        mockEuxService.initMetrics()

        val prefillService = PrefillService(mockPrefillSEDService)
        prefillService.initMetrics()

        innhentingService = InnhentingService(personService, prefillService, mockEuxService)

        personService.initMetrics()
    }


    @Test
    fun `update SED Version from old version to new version`() {
        val sed = SED(SEDType.P2000)
        val bucVersion = "v4.2"

        innhentingService.updateSEDVersion(sed, bucVersion)
        assertEquals(bucVersion, "v${sed.sedGVer}.${sed.sedVer}")
    }

    @Test
    fun `update SED Version from old version to same version`() {
        val sed = SED(SEDType.P2000)
        val bucVersion = "v4.1"

        innhentingService.updateSEDVersion(sed, bucVersion)
        assertEquals(bucVersion, "v${sed.sedGVer}.${sed.sedVer}")
    }


    @Test
    fun `update SED Version from old version to unknown new version`() {
        val sed = SED(SEDType.P2000)
        val bucVersion = "v4.4"

        innhentingService.updateSEDVersion(sed, bucVersion)
        assertEquals("v4.1", "v${sed.sedGVer}.${sed.sedVer}")
    }


}

