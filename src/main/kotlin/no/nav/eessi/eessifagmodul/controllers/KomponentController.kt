package no.nav.eessi.eessifagmodul.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jdk.nashorn.internal.objects.NativeArray.forEach
import jdk.nashorn.internal.parser.JSONParser
import no.nav.eessi.eessifagmodul.models.OpprettBuCogSEDResponse
import no.nav.eessi.eessifagmodul.models.PENBrukerData
import no.nav.eessi.eessifagmodul.services.EESSIKomponentenService
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.SpringBootConfiguration
import org.springframework.core.SpringProperties


@CrossOrigin
@RestController
@RequestMapping("/komponent")
class KomponentController(val eessiKomponentenService: EESSIKomponentenService) {

    @RequestMapping("/req")
    fun testMethodBucogSED() {
        eessiKomponentenService.opprettBuCogSED ("1234", "Testeren", "12345612345")
    }


    @Value("\${no.nav.eessi.eessifagmodul.hello:}")
    lateinit var test:String

    @Autowired
    lateinit var props: Properties

    @GetMapping("/test")
    fun testReqogRes() : EESSIKomponentenService.OpprettBuCogSEDRequest {
        printSystemProperties()
        //eessiKomponentenService.opprettBuCogSED ("1234", "Testeren", "12345612345")
        val data = PENBrukerData( "123456", "Dummybruker", "Ole Olsen", Instant.now())
        val req = eessiKomponentenService.opprettBucogSEDrequest(data)
        return req
    }

    fun printSystemProperties() {
        println("System Properties\n---------------------------------------------")

        val keynames = props.stringPropertyNames()
        val iterator = keynames.iterator()

        iterator.forEach {
            var strValue = props.getProperty(it)
            println("= :  $it, Value : " + strValue)
        }
        println("\n---------------------------------------------")
    }


    @GetMapping("/opprettEESSIreq")
    fun getOpprettEESSIreq(): EESSIKomponentenService.OpprettBuCogSEDRequest {
        val data = PENBrukerData( "123456", "Dummybruker", "Ole Olsen", Instant.now())
        val req = eessiKomponentenService.opprettBucogSEDrequest(data)
        return req
    }

    @PostMapping("/opprett")
    fun opprettBuCogSED(): OpprettBuCogSEDResponse? {
        val response = eessiKomponentenService.opprettBuCogSED ("1234", "Testeren", "12345612345")
        return response
    }

    @PostMapping("/postres")
    fun opprettBuCogSEDResponse(@RequestBody body: OpprettBuCogSEDResponse) {
        print(body)
    }

    @PostMapping("/postreq")
    fun opprettBuCogSEDRequest(@RequestBody body: EESSIKomponentenService.OpprettBuCogSEDRequest) {
       println(body)
    }

}