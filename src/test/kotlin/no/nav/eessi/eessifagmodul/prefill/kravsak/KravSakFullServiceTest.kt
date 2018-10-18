package no.nav.eessi.eessifagmodul.prefill.kravsak

import org.junit.Assert.*
import org.junit.Test

class KravSakFullServiceTest {

    private val kravsak = KravSakFullService()


    @Test
    fun `hent liste med kode F_BH_BO_UTL`() {

        val list = kravsak.finnKravGjelder("F_BH_BO_UTL")
        assertEquals(8, list?.size)
    }

    @Test
    fun `sjkke liste med kode F_BH_BO_UTL inneholder ALDER`() {
        val list = kravsak.finnKravGjelder("F_BH_BO_UTL")
        assertNotNull(list)
        var result: KravSak? = null
        list?.forEach {
            if ("F_BH_BO_UTL_ALDER" == it.kravSakFull) {
                result = it
            }
        }
        assertNotNull(result)
        assertEquals("ALDER", result?.sak)
        assertEquals("Førstegangsbehandling bosatt utland", result?.decode)
    }

    @Test
    fun `hent krav med kode KLAGE_GJENLEV`() {

        val result = kravsak.finnKravSakFull("KLAGE_GJENLEV")

        assertNotNull(result)
        assertEquals("GJENLEV", result?.sak)
        assertEquals("Klage", result?.decode)
    }

    @Test
    fun `hent krav med kode F_BH_BO_UTL_UFOREP`() {
        val result = kravsak.finnKravSakFull("F_BH_BO_UTL_UFOREP")
        assertNotNull(result)
        assertEquals("UFOREP", result?.sak)
        assertEquals("F_BH_BO_UTL", result?.kravGjelder)
        assertEquals("Førstegangsbehandling bosatt utland", result?.decode)
    }

}