package no.nav.eessi.pensjon.prefill.sed.vedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.P6000
import no.nav.eessi.pensjon.integrationtest.XmlToP6000Mapper
import no.nav.eessi.pensjon.prefill.BasePrefillNav
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.PesysService
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.web.server.ResponseStatusException

class P6000AlderpensjonAvslagTest {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(67)
    private val ekteFnr = FodselsnummerGenerator.generateFnrForTest(67)

    private lateinit var prefillData: PrefillDataModel
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var personDataCollection: PersonDataCollection
    private val pesysService: PesysService = mockk()

    private val innhentingService by lazy {
        InnhentingService(mockk(), pesysService = pesysService)
    }

    @BeforeEach
    fun setup() {
        prefillSEDService = BasePrefillNav.createPrefillSEDService()
        personDataCollection = PersonPDLMock.createEnkelFamilie(personFnr, ekteFnr)

    }

    @Test
    fun `preutfylling P6000 feiler ved mangler av vedtakId`() {
        every { pesysService.hentP6000data(any()) } returns XmlToP6000Mapper.readP6000FromXml("/pensjonsinformasjon/vedtak/P6000vedtak-alderpensjon-avslag.xml")
        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P6000, personFnr, penSaksnummer = "22580170", vedtakId = null)
        assertThrows<ResponseStatusException> {
            innhentingService.hentPensjoninformasjonCollection(prefillData)
        }
    }

    @ParameterizedTest
    @DisplayName("Preutfylling av P6000 ved avslag gir riktig preutfylling av sed")
    @CsvSource(
        "03, /pensjonsinformasjon/vedtak/P6000-AP-Avslag.xml, 12312312",
        "02, /pensjonsinformasjon/vedtak/P6000-AP-Under1aar-Avslag.xml, 12312312"
    )
    fun `Preutfylling av P6000 ved avslag gir forskjellig preutfylling av riktig avslagsbegrunnelse`(avslagsbegrunnelse: String, fraFil : String, vedtakId: String) {
        every { pesysService.hentP6000data(any()) } returns XmlToP6000Mapper.readP6000FromXml(fraFil)

        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P6000, personFnr, penSaksnummer = "22580170", vedtakId = vedtakId)

        val pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)
        val p6000 = prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null,) as P6000

        val vedtak = p6000.pensjon?.vedtak?.get(0)
        val result = p6000.pensjon!!

        assertEquals(avslagsbegrunnelse, vedtak?.avslagbegrunnelse?.get(0)?.begrunnelse)

        assertEquals("01", vedtak?.type)
        assertEquals("02", vedtak?.resultat, "4.1.4 vedtak.resultat")
        assertEquals("2020-12-16", result.tilleggsinformasjon?.dato)
        assertEquals("0607", result.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.postnummer)
        assertEquals("NO:noinst002", result.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.institusjonsid)
        assertEquals("six weeks from the date the decision is received", result.sak?.kravtype?.get(0)?.datoFrist)
        assertEquals("Postboks 6600 Etterstad TEST", result.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.institusjonsadresse)

    }

}
