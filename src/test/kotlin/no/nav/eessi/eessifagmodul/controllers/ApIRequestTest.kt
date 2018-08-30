package no.nav.eessi.eessifagmodul.controllers

import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import no.nav.eessi.eessifagmodul.utils.validateJson
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ApIRequestTest {



    @Test
    fun `generate request mock payload of SED`() {

        val p2200path = Paths.get("src/test/resources/json/P2200-NAV.json")
        val p2200file = String(Files.readAllBytes(p2200path))
        assertTrue(validateJson(p2200file))

        val payload = p2200file

        //val trygdetid  = createPersonTrygdeTidMock()
        //val payload = mapAnyToJson(trygdetid)
        //logger.debug(payload)

        val req = ApiController.ApiRequest(
                sed = "P4000",
                caseId = "123456789",
                euxCaseId = "99191999911",
                pinid = "00000",
                buc = "P_BUC_01",
                subjectArea = "Pensjon",
                payload = payload,
                mockSED = true
        )

        val json = mapAnyToJson(req)
        assertNotNull(json)
        if (true) {
            println("-------------------------------------------------------------------------------------------------------")
            println(json)
            println("-------------------------------------------------------------------------------------------------------")
        }
    }


}