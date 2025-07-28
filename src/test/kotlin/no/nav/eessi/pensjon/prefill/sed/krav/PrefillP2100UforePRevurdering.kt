package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.prefill.BasePrefillNav
import no.nav.eessi.pensjon.prefill.EtterlatteService
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper.lesPensjonsdataFraFil
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PrefillP2100UforePRevurdering {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(45)
    private val avdodPersonFnr = FodselsnummerGenerator.generateFnrForTest(45)
    private val pesysSaksnummer = "22917763"
    private val pesysKravid = "12354"

    private lateinit var prefillNav: PrefillPDLNav
    private lateinit var prefillData: PrefillDataModel
    private lateinit var etterlatteService: EtterlatteService
    private lateinit var prefillSEDService: PrefillSEDService

    @BeforeEach
    fun setup() {
        etterlatteService = mockk()
        prefillNav = BasePrefillNav.createPrefillNav()

        prefillData = PrefillDataModelMother.initialPrefillDataModel(
                sedType = SedType.P2100,
                pinId = personFnr,
                penSaksnummer = pesysSaksnummer,
                avdod = PersonInfo(avdodPersonFnr, "112233445566"),
                kravId = pesysKravid)

    }

    @Test
    fun `forventet korrekt utfylt P2100 uforepensjon med kap4 og 9`() {
        val personDataCollection = PersonPDLMock.createAvdodFamilie(personFnr, avdodPersonFnr)
        val dataFromPEN = lesPensjonsdataFraFil("/pensjonsinformasjon/krav/P2100-UP-GJ-REVURD-M-KRAVID.xml")

        val innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = dataFromPEN)
        val pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)

        prefillSEDService = BasePrefillNav.createPrefillSEDService()

        val p2100 = prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null)

        assertNotNull(p2100.nav?.krav)
        assertEquals("2020-08-01", p2100.nav?.krav?.dato)
        assertEquals("Kravdato fra det opprinnelige vedtak med gjenlevenderett er angitt i SED P2100", prefillData.melding)
    }

}

