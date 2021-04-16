package no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak

import com.nhaarman.mockitokotlin2.mock
import no.nav.eessi.pensjon.eux.model.sed.P6000
import no.nav.eessi.pensjon.eux.model.sed.SedType
import no.nav.eessi.pensjon.fagmodul.models.PersonDataCollection
import no.nav.eessi.pensjon.fagmodul.models.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.models.PrefillDataModelMother
import no.nav.eessi.pensjon.fagmodul.prefill.PersonPDLMock
import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjonMother.standardEessiInfo
import no.nav.eessi.pensjon.fagmodul.prefill.pdl.FodselsnummerMother
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonService
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillTestHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.web.server.ResponseStatusException
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class P6000AlderpensjonAvslagTest {

    private val personFnr = FodselsnummerMother.generateRandomFnr(67)
    private val ekteFnr = FodselsnummerMother.generateRandomFnr(67)

    private lateinit var prefillData: PrefillDataModel
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var dataFromPEN: PensjonsinformasjonService
    private lateinit var eessiInformasjon: EessiInformasjon
    private lateinit var personDataCollection: PersonDataCollection
    private lateinit var prefillNav: PrefillPDLNav

    @BeforeEach
    fun setup() {
        personDataCollection = PersonPDLMock.createEnkelFamilie(personFnr, ekteFnr)

        prefillNav = PrefillPDLNav(
                prefillAdresse = mock(),
                institutionid = "NO:noinst002",
                institutionnavn = "NOINST002, NO INST002, NO")

        eessiInformasjon = standardEessiInfo()
    }


    @Test
    fun `forventet korrekt utfylling av pensjon objekt på alderpensjon med avslag`() {
        dataFromPEN = PrefillTestHelper.lesPensjonsdataVedtakFraFil("P6000vedtak-alderpensjon-avslag.xml")
        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P6000, personFnr, penSaksnummer = "22580170", vedtakId = "12312312")
        prefillSEDService = PrefillSEDService(dataFromPEN, eessiInformasjon, prefillNav)

        val p6000 = prefillSEDService.prefill(prefillData, personDataCollection) as P6000
        val p6000Pensjon = p6000.p6000Pensjon

        val vedtak = p6000Pensjon.vedtak?.get(0)
        assertEquals("01", vedtak?.type)
        assertEquals("02", vedtak?.resultat, "4.1.4 vedtak.resultat")

        val avslagBegrunnelse = vedtak?.avslagbegrunnelse?.get(0)
        assertEquals("03", avslagBegrunnelse?.begrunnelse, "4.1.13.1          AvlsagsBegrunnelse")

        assertEquals("six weeks from the date the decision is received", p6000Pensjon.sak?.kravtype?.get(0)?.datoFrist)

        assertEquals("2019-11-11", p6000Pensjon.tilleggsinformasjon?.dato)
        assertEquals("NO:noinst002", p6000Pensjon.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.institusjonsid)
        assertEquals("Postboks 6600 Etterstad TEST", p6000Pensjon.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.institusjonsadresse)
        assertEquals("0607", p6000Pensjon.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.postnummer)

    }

    @Test
    fun `forventet korrekt utfylling av pensjon objekt på alderpensjon med avslag under 1 arr`() {
        dataFromPEN = PrefillTestHelper.lesPensjonsdataVedtakFraFil("P6000-AP-Under1aar-Avslag.xml")
        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P6000, personFnr, penSaksnummer = "22580170", vedtakId = "12312312")
        prefillSEDService = PrefillSEDService(dataFromPEN, eessiInformasjon, prefillNav)

        val p6000 = prefillSEDService.prefill(prefillData, personDataCollection) as P6000
        val result = p6000.p6000Pensjon

        val vedtak = result.vedtak?.get(0)
        assertEquals("01", vedtak?.type)
        assertEquals("02", vedtak?.resultat, "4.1.4 vedtak.resultat")

        val avslagBegrunnelse = vedtak?.avslagbegrunnelse?.get(0)
        assertEquals("02", avslagBegrunnelse?.begrunnelse, "4.1.13.1          AvlsagsBegrunnelse")

        assertEquals("six weeks from the date the decision is received", result.sak?.kravtype?.get(0)?.datoFrist)

        assertEquals("2020-12-16", result.tilleggsinformasjon?.dato)
        assertEquals("NO:noinst002", result.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.institusjonsid)
        assertEquals("Postboks 6600 Etterstad TEST", result.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.institusjonsadresse)
        assertEquals("0607", result.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.postnummer)

    }

    @Test
    fun `forventet korrekt utfylling av pensjon objekt på alderpensjon med avslag under 3 arr`() {
        dataFromPEN = PrefillTestHelper.lesPensjonsdataVedtakFraFil("P6000-AP-Avslag.xml")
        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P6000, personFnr, penSaksnummer = "22580170", vedtakId = "12312312")
        prefillSEDService = PrefillSEDService(dataFromPEN, eessiInformasjon, prefillNav)

        val p6000 = prefillSEDService.prefill(prefillData, personDataCollection) as P6000
        val result = p6000.p6000Pensjon

        val vedtak = result.vedtak?.get(0)
        assertEquals("01", vedtak?.type)
        assertEquals("02", vedtak?.resultat, "4.1.4 vedtak.resultat")

        val avslagBegrunnelse = vedtak?.avslagbegrunnelse?.get(0)
        assertEquals("03", avslagBegrunnelse?.begrunnelse, "4.1.13.1          AvlsagsBegrunnelse")

        assertEquals("six weeks from the date the decision is received", result.sak?.kravtype?.get(0)?.datoFrist)

        assertEquals("2020-12-16", result.tilleggsinformasjon?.dato)
        assertEquals("NO:noinst002", result.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.institusjonsid)
        assertEquals("Postboks 6600 Etterstad TEST", result.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.institusjonsadresse)
        assertEquals("0607", result.tilleggsinformasjon?.andreinstitusjoner?.get(0)?.postnummer)

    }


    @Test
    fun `preutfylling P6000 feiler ved mangler av vedtakId`() {
        dataFromPEN = PrefillTestHelper.lesPensjonsdataVedtakFraFil("P6000vedtak-alderpensjon-avslag.xml")
        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P6000, personFnr, penSaksnummer = "22580170", vedtakId = "")
        prefillSEDService = PrefillSEDService(dataFromPEN, eessiInformasjon, prefillNav)

        assertThrows<ResponseStatusException> {
            prefillSEDService.prefill(prefillData, personDataCollection)
        }
    }
}
