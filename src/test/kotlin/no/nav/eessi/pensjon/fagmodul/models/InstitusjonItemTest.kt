package no.nav.eessi.pensjon.fagmodul.models

import org.junit.Test
import kotlin.test.assertEquals

class InstitusjonItemTest {

    @Test
    fun `call checkAndConvertInstituion med spesialtegn som input`() {

        val institusjonItem = InstitusjonItem(country = "NO", institution = "NO:NAVT002", name = null)
        val actual = institusjonItem.checkAndConvertInstituion()

        assertEquals(institusjonItem.institution, actual)
    }

    @Test
    fun `call checkAndConvertInstituion uten spesialtegn som input`() {

        val institusjonItem = InstitusjonItem(country = "NO", institution = "NAVT002", name = null)
        val actual = institusjonItem.checkAndConvertInstituion()

        assertEquals("NO:NAVT002", actual)
    }

    @Test
    fun `call checkAndConvertInstituion that is null as input`() {

        val institusjonItem = InstitusjonItem(country = "", institution = "", name = null)
        val actual = institusjonItem.checkAndConvertInstituion()

        assertEquals(":", actual)
    }
}