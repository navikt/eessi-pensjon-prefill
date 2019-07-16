package no.nav.eessi.pensjon.fagmodul.sedmodel

import no.nav.eessi.pensjon.utils.validateJson
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertTrue

fun getTestJsonFile(filename: String): String {
    val filepath = "src/test/resources/json/nav/${filename}"
    val json = String(Files.readAllBytes(Paths.get(filepath)))
    assertTrue(validateJson(json))
    return json
}

fun getSEDfromTestfile(json: String): SED {
    return SED.fromJson(json)
}

fun getHSEDfromTestfile(json: String): HSED {
    return HSED.fromJson(json)
}
