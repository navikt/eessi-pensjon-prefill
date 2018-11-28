package no.nav.eessi.eessifagmodul.models

import no.nav.eessi.eessifagmodul.utils.validateJson
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class SedP7000Test {
    val logger: Logger by lazy { LoggerFactory.getLogger(SedP7000Test::class.java) }

    @Test
    fun `create SED P7000 from json datafile`() {
        val p7000path = Paths.get("src/test/resources/json/P7000-NAV.json")
        val p7000file = String(Files.readAllBytes(p7000path))
        assertTrue(validateJson(p7000file))
        val p7000sed = SED.fromJson(p7000file)
        val json = p7000sed.toJson()
        JSONAssert.assertEquals(p7000file, json, false)

        assertEquals("daglig4166", p7000sed.pensjon?.ytelser?.get(0)?.beloep?.get(0)?.annenbetalingshyppighetytelse)

        assertEquals("5.1.5.2.1. Adressat for revurderingen Repetetive", p7000sed.pensjon?.samletVedtak?.avslag?.get(0)?.adresse)

        assertEquals("1942-12-19", p7000sed.pensjon?.ytelser?.get(0)?.startdatoutbetaling)
        assertEquals("4163", p7000sed.pensjon?.vedtak?.get(0)?.beregning?.get(0)?.beloepBrutto?.beloep)
    }
}