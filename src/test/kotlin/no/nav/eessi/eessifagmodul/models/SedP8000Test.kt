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


class SedP8000Test {
    val logger: Logger by lazy { LoggerFactory.getLogger(SedP8000Test::class.java) }

    @Test
    fun `create SED P8000 from json datafile`() {
        val p8000path = Paths.get("src/test/resources/json/P8000-NAV.json")
        val p8000file = String(Files.readAllBytes(p8000path))
        assertTrue(validateJson(p8000file))
        val p8000sed = SED.fromJson(p8000file)
        val json = p8000sed.toJson()
        JSONAssert.assertEquals(p8000file, json, false)

        assertEquals("Her kommer tekst ad Begrunnelse for kravet", p8000sed.pensjon?.anmodning?.informasjon?.begrunnelseKrav)

        assertEquals("Begrunnelse for kravet", p8000sed.pensjon?.anmodning?.begrunnKrav)



    }
}