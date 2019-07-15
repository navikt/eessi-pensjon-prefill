package no.nav.eessi.pensjon.fagmodul.prefill

import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.models.SED
import no.nav.eessi.pensjon.services.geo.LandkodeService
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.validateJson
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ApiRequestTest {

    private val printout = false
    private val printsed = false

    private val logger: Logger by lazy { LoggerFactory.getLogger(LandkodeService::class.java) }

    private fun createMockApiRequest(sedName: String, buc: String, payload: String): ApiRequest {
        val items = listOf(InstitusjonItem(country = "NO", institution = "NAVT003"))
        return ApiRequest(
                institutions = items,
                sed = sedName,
                sakId = "01234567890",
                euxCaseId = "99191999911",
                aktoerId = "1000060964183",
                buc = buc,
                subjectArea = "Pensjon",
                payload = payload,
                mockSED = true
        )
    }

    private fun readJsonAndParseToSed(filename: String): String {
        val p2200path = Paths.get("src/test/resources/json/nav/$filename")
        val p2200file = String(Files.readAllBytes(p2200path))
        assertTrue(validateJson(p2200file))
        return p2200file
    }

    fun validateAndPrint(req: ApiRequest) {
        if (printsed) {
            val json = SED.fromJson(req.payload!!).toJson()
            logger.info(json)
        }
        if (printout) {
            val json = mapAnyToJson(req)
            assertNotNull(json)
            logger.info("\n\n\n $json \n\n\n")
        }
    }

    @Test
    fun `generate request mock payload of SED P2000`() {
        val payload = readJsonAndParseToSed("P2000-NAV.json")
        //val payload = readJsonAndParseToSed("P2000-NAV-mockAP.json")
        validateAndPrint(createMockApiRequest("P2000", "P_BUC_01", payload))
    }

    @Test
    fun `generate request mock payload of SED P2100`() {
        val payload = readJsonAndParseToSed("P2100-NAV-unfin.json")
        createMockApiRequest("P2100", "P_BUC_02", payload)
    }

    @Test
    fun `generate request mock payload of SED P2200`() {
        val payload = readJsonAndParseToSed("P2200-NAV.json")
        createMockApiRequest("P2200", "P_BUC_03", payload)
    }

    @Test
    fun `generate request mock payload of SED P4000`() {
        val payload = readJsonAndParseToSed("P4000-NAV.json")
        createMockApiRequest("P4000", "P_BUC_05", payload)
    }

    @Test
    fun `generate request mock payload of SED P5000`() {
        val payload = readJsonAndParseToSed("P5000-NAV.json")
        createMockApiRequest("P5000", "P_BUC_05", payload)
    }

    @Test
    fun `generate request mock payload of SED P6000`() {
        val payload = readJsonAndParseToSed("P6000-NAV.json")
        createMockApiRequest("vedtak", "P_BUC_06", payload)
    }

    @Test(expected = MangelfulleInndataException::class)
    fun `confirm document when sed is not valid`() {
        val mockData = ApiRequest(
                subjectArea = "Pensjon",
                sakId = "EESSI-PEN-123",
                institutions = listOf(InstitusjonItem("NO", "DUMMY")),
                sed = "Q3300",
                buc = "P_BUC_06",
                aktoerId = "0105094340092"
        )
        ApiRequest.buildPrefillDataModelConfirm(mockData, "12345")
    }

    @Test(expected = MangelfulleInndataException::class)
    fun `confirm document sed is null`() {
        val mockData = ApiRequest(
                subjectArea = "Pensjon",
                sakId = "EESSI-PEN-123",
                institutions = listOf(InstitusjonItem("NO", "DUMMY")),
                sed = null,
                buc = "P_BUC_06",
                aktoerId = "0105094340092"
        )
        ApiRequest.buildPrefillDataModelConfirm(mockData, "12345")
    }

    @Test
    fun `check on minimum valid request to model`() {
        val mockData = ApiRequest(
                sakId = "12234",
                sed = "P6000",
                aktoerId = "0105094340092"
        )

        val model = ApiRequest.buildPrefillDataModelConfirm(mockData, "12345")

        assertEquals("12345", model.personNr)
        assertEquals("12234", model.penSaksnummer)
        assertEquals("0105094340092", model.aktoerID)
        assertEquals("P6000", model.getSEDid())

        assertEquals(SED::class.java, model.sed::class.java)

    }

    @Test(expected = IllegalArgumentException::class)
    fun `check on aktoerId is null`() {
        val mockData = ApiRequest(
                sakId = "1213123123",
                sed = "P6000",
                aktoerId = null
        )
        ApiRequest.buildPrefillDataModelConfirm(mockData, "12345")
    }

}