package no.nav.eessi.eessifagmodul.prefill

import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.InstitusjonItem
import no.nav.eessi.eessifagmodul.models.createSED
import no.nav.eessi.eessifagmodul.services.pensjonsinformasjon.PensjonsinformasjonService
import no.nav.eessi.eessifagmodul.utils.simpleFormat
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.sak.V1Sak
import no.nav.pensjon.v1.trygdetidliste.V1TrygdetidListe
import no.nav.pensjon.v1.vedtak.V1Vedtak
import no.nav.pensjon.v1.vilkarsvurderingliste.V1VilkarsvurderingListe
import no.nav.pensjon.v1.ytelsepermaaned.V1YtelsePerMaaned
import no.nav.pensjon.v1.ytelsepermaanedliste.V1YtelsePerMaanedListe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.time.LocalDate
import javax.xml.datatype.DatatypeFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class PrefillP6000PensionDataFromPESYSTest {

    @Mock
    lateinit var pensjonsinformasjonService: PensjonsinformasjonService

    @Mock
    lateinit var preutfyllingPersonFraTPS: PrefillPersonDataFromTPS

    lateinit var dataFromPESYS: PrefillP6000PensionDataFromPESYS

    lateinit var  prefill: PrefillDataModel

    @Before
    fun setup() {
        prefill = PrefillDataModel()
        dataFromPESYS = PrefillP6000PensionDataFromPESYS(pensjonsinformasjonService)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test for pensjon feiler mangler vedtakid`() {
        prefill = generatePrefillData(68, "P6000")
        prefill.vedtakId = ""

//        val mockResult = generateFakePensjoninformasjonForALDER()
//        whenever(pensjonsinformasjonService.hentAlt(ArgumentMatchers.anyString())).thenReturn(mockResult)
        dataFromPESYS.prefill(prefill)
//        assertNotNull(result)
//        val vedtak = result.vedtak
//        assertNotNull(vedtak)
//        vedtak?.forEach {
//            assertEquals("01", it.type)
//            assertEquals(mockResult.vedtak.virkningstidspunkt.simpleFormat() , it.virkningsdato)
//        }

    }



    @Test
    fun `Grunnlag for ytelse alderpensjon old age`() {
        prefill = generatePrefillData(68, "P6000")

        val mockResult = generateFakePensjoninformasjonForALDER()
        whenever(pensjonsinformasjonService.hentAlt(ArgumentMatchers.anyString())).thenReturn(mockResult)

        val result = dataFromPESYS.prefill(prefill)

        assertNotNull(result)
        val vedtak = result.vedtak
        assertNotNull(vedtak)
        vedtak?.forEach {
            assertEquals("01", it.type)
            assertEquals(mockResult.vedtak.virkningstidspunkt.simpleFormat() , it.virkningsdato)
        }

    }

    @Test
    fun `Grunnlag for ytelse alderpensjon early age`() {
        prefill = generatePrefillData(66, "P6000")

        val mockResult = generateFakePensjoninformasjonForALDER()
        mockResult.sak.isUttakFor67 = true

        whenever(pensjonsinformasjonService.hentAlt(ArgumentMatchers.anyString())).thenReturn(mockResult)

        val result = dataFromPESYS.prefill(prefill)

        assertNotNull(result)
        val vedtak = result.vedtak
        assertNotNull(vedtak)
        vedtak?.forEach {
            assertEquals("06", it.type)
            assertEquals(mockResult.vedtak.virkningstidspunkt.simpleFormat() , it.virkningsdato)
        }

    }

    @Test
    fun `Grunnlag for ytelse alderpensjon med uforepen`() {
        prefill = generatePrefillData(66, "P6000")

        val mockResult = generateFakePensjoninformasjonForUFOREP()
        whenever(pensjonsinformasjonService.hentAlt(ArgumentMatchers.anyString())).thenReturn(mockResult)

        val result = dataFromPESYS.prefill(prefill)

        assertNotNull(result)
        val vedtak = result.vedtak
        assertNotNull(vedtak)
        vedtak?.forEach {
            assertEquals("02", it.type)
            assertEquals(mockResult.vedtak.virkningstidspunkt.simpleFormat() , it.virkningsdato)
        }

    }

    @Test
    fun `Grunnlag for ytelse alderpensjon med GJENLEV`() {
        prefill = generatePrefillData(66, "P6000")

        val mockResult = generateFakePensjoninformasjonForGJENLEV()
        whenever(pensjonsinformasjonService.hentAlt(ArgumentMatchers.anyString())).thenReturn(mockResult)

        val result = dataFromPESYS.prefill(prefill)

        assertNotNull(result)
        val vedtak = result.vedtak
        assertNotNull(vedtak)
        vedtak?.forEach {
            assertEquals("03", it.type)
            assertEquals(mockResult.vedtak.virkningstidspunkt.simpleFormat() , it.virkningsdato)
        }

    }


    private fun generateFakePensjoninformasjonForALDER(): Pensjonsinformasjon {
        return generateFakePensjoninformasjonForKSAK("ALDER")
    }

    private fun generateFakePensjoninformasjonForUFOREP(): Pensjonsinformasjon {
        return generateFakePensjoninformasjonForKSAK("UFOREP")
    }

    private fun generateFakePensjoninformasjonForGJENLEV(): Pensjonsinformasjon {
        return generateFakePensjoninformasjonForKSAK("GJENLEV")
    }

    private fun generatePrefillData(subtractYear: Int, sedId: String): PrefillDataModel {
        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        prefill.apply {
            rinaSubject= "Pensjon"
            sed = createSED(sedId)
            penSaksnummer = "12345"
            vedtakId = "12312312"
            buc = "P_BUC_99"
            aktoerID = "123456789"
            personNr = generateRandomFnr(subtractYear)
            institution = items
        }
        return prefill
    }

    private fun generateRandomFnr(yearsToSubtract: Int) : String {
        val fnrdate = LocalDate.now().minusYears(yearsToSubtract.toLong())
        val y = fnrdate.year.toString()
        val day = fixDigits(fnrdate.dayOfMonth.toString())
        val month = fixDigits(fnrdate.month.value.toString())
        val fixedyear = y.substring(2,y.length)
        val fnr = day+month+fixedyear+43352
        return fnr
    }

    private fun fixDigits(str: String): String {
        if (str.length == 1) {
            return "0$str"
        }
        return str
    }

    private fun generateFakePensjoninformasjonForKSAK(ksak: String): Pensjonsinformasjon {
        val xmlcal = DatatypeFactory.newInstance().newXMLGregorianCalendar()

        val v1sak = V1Sak()
        v1sak.sakType = ksak // "ALDER"
        v1sak.isUttakFor67 = false
        val v1vedtak = V1Vedtak()

        v1vedtak.datoFattetVedtak = xmlcal
        v1vedtak.virkningstidspunkt = xmlcal

        val ytelsePerMaanedListe = V1YtelsePerMaanedListe()

        val v1YtelsePerMaaned = V1YtelsePerMaaned()
        v1YtelsePerMaaned.isMottarMinstePensjonsniva = true
        v1YtelsePerMaaned.fom = xmlcal
        v1YtelsePerMaaned.tom = xmlcal
        ytelsePerMaanedListe.ytelsePerMaanedListe.add(v1YtelsePerMaaned)

        val vilkarsvurderingListe = V1VilkarsvurderingListe()

        val trygdetidListe = V1TrygdetidListe()

        val peninfo = Pensjonsinformasjon()
        peninfo.sak = v1sak
        peninfo.vedtak = v1vedtak

        peninfo.ytelsePerMaanedListe = ytelsePerMaanedListe
        peninfo.vilkarsvurderingListe = vilkarsvurderingListe
        peninfo.trygdetidListe = trygdetidListe

        return peninfo
    }

}