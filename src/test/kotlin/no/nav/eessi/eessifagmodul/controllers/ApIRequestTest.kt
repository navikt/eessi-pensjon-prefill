package no.nav.eessi.eessifagmodul.controllers

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import no.nav.eessi.eessifagmodul.models.InstitusjonItem
import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import no.nav.eessi.eessifagmodul.utils.mapJsonToAny
import no.nav.eessi.eessifagmodul.utils.typeRefs
import no.nav.eessi.eessifagmodul.utils.validateJson
import org.junit.Ignore
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ApIRequestTest {

    private val printout = false
    private val printsed = false

    fun createMockApiRequest(sedName: String, buc: String, payload: String): ApiController.ApiRequest {
        val items = listOf(InstitusjonItem(country = "NO", institution = "NAVT003"))
        return  ApiController.ApiRequest(
                institutions = items,
                sed = sedName,
                caseId = "01234567890",
                euxCaseId = "99191999911",
                pinid = "1000060964183",
                buc = buc,
                subjectArea = "Pensjon",
                payload = payload,
                mockSED = true
        )
    }

    fun readJsonAndParseToSed(filename: String): String {
        val p2200path = Paths.get("src/test/resources/json/$filename")
        val p2200file = String(Files.readAllBytes(p2200path))
        assertTrue(validateJson(p2200file))
        return p2200file
    }

    fun validateAndPrint(req: ApiController.ApiRequest) {
        if (printsed) {
            //val payload = mapJsonToAny(req.payload!!, typeRefs<SED>())
            val payload = SED().fromJson(req.payload!!)
            assertNotNull(payload)
            val jsonSed = payload.toJson()
            printOut(req.sed!!, jsonSed)
        }
        if (printout) {
            val json = mapAnyToJson(req)
            assertNotNull(json)
            printOut(req.sed!!, json)
        }
    }

    fun printOut(sedName: String, json: String) {
        println("--------------------------------[ ${sedName} ]--------------------------------------------------------")
        println(json)
        println("------------------------------------------------------------------------------------------------------")
    }

    @Test
    fun `generate request mock payload of SED P2000`() {
        val payload = readJsonAndParseToSed("P2000-NAV.json")
        validateAndPrint(createMockApiRequest("P2000","P_BUC_01", payload))
    }

    @Ignore
    fun `generate request mock payload of SED P2100`() {
        val payload = readJsonAndParseToSed("P2100-NAV.json")
        validateAndPrint(createMockApiRequest("P2100","P_BUC_02", payload))

    }

    @Test
    fun `generate request mock payload of SED P2200`() {
        val payload = readJsonAndParseToSed("P2200-NAV.json")
        validateAndPrint(createMockApiRequest("P2200","P_BUC_03", payload))

    }

    @Test
    fun `generate request mock payload of SED P4000`() {
        val payload = readJsonAndParseToSed("P4000-NAV.json")
        validateAndPrint(createMockApiRequest("P4000","P_BUC_05", payload))

    }

    @Test
    fun `generate request mock payload of SED P5000`() {
        val payload = readJsonAndParseToSed("P5000-NAV.json")
        validateAndPrint(createMockApiRequest("P5000","P_BUC_05", payload))

    }

    @Test
    fun `generate request mock payload of SED P6000`() {
        //tmp-p6000-pesys.json
        val payload = readJsonAndParseToSed("P6000-NAV.json")
        validateAndPrint(createMockApiRequest("vedtak","P_BUC_06", payload))

    }

}