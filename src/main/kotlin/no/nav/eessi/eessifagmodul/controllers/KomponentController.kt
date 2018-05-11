package no.nav.eessi.eessifagmodul.controllers

import com.google.common.collect.Lists
import no.nav.eessi.eessifagmodul.models.OpprettBuCogSEDRequest
import no.nav.eessi.eessifagmodul.models.OpprettBuCogSEDResponse
import no.nav.eessi.eessifagmodul.models.PENBrukerData
import no.nav.eessi.eessifagmodul.services.EESSIKomponentenService
import org.jetbrains.annotations.TestOnly
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.RequestEntity
import org.springframework.web.servlet.ModelAndView

@CrossOrigin
@RestController
@RequestMapping("/komponent")

//klasse for prototyping, test o.l
class KomponentController {

    @Autowired
    lateinit var eessiKomponentenService: EESSIKomponentenService

    @Value("\${no.nav.eessi.eessifagmodul.hello:}")
    lateinit var test:String

    @Autowired
    lateinit var props: Properties

    @RequestMapping("/req")
    fun testMethodBucogSED() : OpprettBuCogSEDResponse {
        val brukerData = PENBrukerData("123456", "DummyTester1", "12123123123")
        val response = eessiKomponentenService.opprettBuCogSED (brukerData)
        return response!!
    }

    @GetMapping("/prop")
    fun hentutProperties() : List<Pair<String, String>> {
        return printSystemProperties()
    }


    @GetMapping("/test")
    fun testReqogRes() : OpprettBuCogSEDRequest {
        printSystemProperties()

        val data = PENBrukerData( "123456", "Dummybruker", "Ole Olsen", Instant.now())
        val request = eessiKomponentenService.opprettBucogSEDrequest(data)

        val reqModel = ModelAndView("jsonView").addObject(request)

        println("request : " + reqModel)

        val response= eessiKomponentenService.opprettBuCogSED (data)
        return request
    }

    fun printSystemProperties() : List<Pair<String, String>> {
        println("System Properties\n---------------------------------------------")

        val keynames = props.stringPropertyNames()
        val iterator = keynames.iterator()

        val list : MutableList<Pair<String, String>> = mutableListOf()
        iterator.forEach {
            val strValue = props.getProperty(it)
            println("= :  $it, Value : $strValue")
            var pair : Pair<String, String> = Pair("Key : $it", "Value : $strValue")
            list.add(pair)
        }
        println("\n---------------------------------------------")
        return list
    }


    @GetMapping("/opprettEESSIreq")
    fun getOpprettEESSIreq(): OpprettBuCogSEDRequest {
        val data = PENBrukerData( "123456", "Dummybruker", "Ole Olsen", Instant.now())
        val req = eessiKomponentenService.opprettBucogSEDrequest(data)
        return req
    }

    @GetMapping("/opprettPENBrukerData")
    fun getOpprettBrukerData() : PENBrukerData  {
        val data = PENBrukerData( "123456", "Dummybruker", "Ole Olsen", Instant.now())
        return data
    }


    @PostMapping("/opprettResponse")
    fun opprettBuCogSED(): OpprettBuCogSEDResponse? {
        val data = PENBrukerData( "123456", "Dummybruker", "Ole Olsen", Instant.now())
        val response = eessiKomponentenService.opprettBuCogSED (data)
        return response
    }

//    @PostMapping("/postres")
//    fun opprettBuCogSEDResponse(@RequestBody body: OpprettBuCogSEDResponse) {
//        print(body)
//    }
//
//    @PostMapping("/postreq")
//    fun opprettBuCogSEDRequest(@RequestBody body: OpprettBuCogSEDRequest) {
//       println(body)
//    }

}