package no.nav.eessi.eessifagmodul.models

import no.nav.eessi.eessifagmodul.controllers.ApiController
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import no.nav.eessi.eessifagmodul.utils.mapJsonToAny
import no.nav.eessi.eessifagmodul.utils.typeRefs
import no.nav.eessi.eessifagmodul.utils.validateJson
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


@RunWith(MockitoJUnitRunner::class)
class SedP4000Test {

    val logger: Logger by lazy { LoggerFactory.getLogger(SedP4000Test::class.java) }

    @Before
    fun setup() {
        logger.debug("Starting tests.... ...")
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun `create mock structure P4000`() {
        val result = createPersonTrygdeTidMock()
        assertNotNull(result)
        val json = mapAnyToJson(result, true)
        println(json)


        val sed = createSED("P4000")
        val nav = NavMock().genererNavMock()
        val pen = PensjonMock().genererMockData()
        sed.nav = nav
        sed.pensjon = pen
        sed.trygdetid = result

        println("\n\n\n------------------------------------------------------------------------------------------------\n\n\n")

        val json2 = mapAnyToJson(sed, true)
        println(json2)

        val mapSED = mapJsonToAny(json2, typeRefs<SED>())

        assertNotNull(mapSED)
        assertEquals(result, mapSED.trygdetid)

    }


    @Test
    fun `create and validate P4000 on multiple ways`() {

        //map load P4000-NAV refrence
        val path = Paths.get("src/test/resources/json/P4000-NAV.json")
        val p4000file = String(Files.readAllBytes(path))
        assertNotNull(p4000file)
        validateJson(p4000file)


        val sed = mapJsonToAny(p4000file, typeRefs<SED>())
        assertNotNull(sed)

        println(sed)

        val json = mapAnyToJson(sed, true)
        println("\n\n\n-------------[ Fra fil -> SED -> Json ]--------------------------------------------------------------------------\n\n\n")
        println(json)
    }


    @Test
    fun `create dummy or mock apiRequest with p4000 json as payload`() {

        val trygdetid  = createPersonTrygdeTidMock()
        val payload = mapAnyToJson(trygdetid)
        println(payload)

        val req = ApiController.ApiRequest(
                sed = "P4000",
                caseId = "12231231",
                euxCaseId = "99191999911",
                pinid = "00000",
                buc = "P_BUC_01",
                subjectArea = "Pensjon",
                payload = payload
        )

        val json = mapAnyToJson(req)
        assertNotNull(json)
        println("-------------------------------------------------------------------------------------------------------")
        println(json)
        println("-------------------------------------------------------------------------------------------------------")

        val apireq = mapJsonToAny(json, typeRefs<ApiController.ApiRequest>())

        val payjson = apireq.payload ?: ""
        assertNotNull(payjson)

        println(payjson)
        assertEquals(payload, payjson)

        val p4k = mapJsonToAny(payjson, typeRefs<PersonTrygdeTid>())
        assertNotNull(p4k)

        assertEquals("DK", p4k.boPerioder!![0].land)

    }

    @Test
    fun `create trygdetid P4000 from file`() {

        val path = Paths.get("src/test/resources/json/Trygdetid_part.json")
        val jsonfile = String(Files.readAllBytes(path))
        assertNotNull(jsonfile)
        validateJson(jsonfile)

        val obj = mapJsonToAny(jsonfile, typeRefs<PersonTrygdeTid>(), true)
        assertNotNull(obj)

        val backtojson = mapAnyToJson(obj, true)
        assertNotNull(backtojson)
        validateJson(backtojson)
        println("jsonfile size : ${jsonfile.length}")
        println("backtojs size : ${backtojson.length}")

        println("-------------------------------------------------------------------------------------------------------")
        println(jsonfile)
        println("-------------------------------------------------------------------------------------------------------")
        println(backtojson)

        val payload = mapAnyToJson(obj)

        val req = ApiController.ApiRequest(
                sed = "P4000",
                caseId = "12231231",
                euxCaseId = "99191999911",
                pinid = "00000",
                buc = "P_BUC_01",
                subjectArea = "Pensjon",
                payload = payload
        )

        val jsonreq = mapAnyToJson(req)

        println("-------------------------------------------------------------------------------------------------------")
        println(  jsonreq        )
        println("-------------------------------------------------------------------------------------------------------")

    }

}