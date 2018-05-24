package no.nav.eessi.eessifagmodul.controllers

import no.nav.eessi.eessifagmodul.domian.RequestException
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.services.EESSIKomponentenService
import org.jetbrains.annotations.TestOnly
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*
import org.springframework.beans.factory.annotation.Autowired

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


    @GetMapping("/opprettRequest")
    fun opprettBucogSEDrequest(): OpprettBuCogSEDRequest {
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
    fun opprettBuCogSEDResponse(): OpprettBuCogSEDResponse? {
        val data = PENBrukerData( "123456", "Dummybruker", "Ole Olsen", Instant.now())
        val response = eessiKomponentenService.opprettBuCogSED (data)
        return response
    }

    @TestOnly
    @GetMapping("/hentsedid/{id}")
    fun hentSedbyID(@PathVariable("id") sedid : Int)  : SED {
        println("Valgre SED ID :  $sedid")

        if (sedid == 40) {
            throw RuntimeException()
        } else if (sedid == 800) {
            throw RequestException("medling")
        }

        val person = NavPerson("12345678911")
        val sed = SED (SEDType = "SED_P1000", NAVSaksnummer = "NAV-SAK-1234", ForsikretPerson = person, Barn = null, Samboer = null)
        return sed
    }

    @TestOnly
    @GetMapping("/hentsedid/{id}/bucs")
    fun hentBucsTilSedID(@PathVariable("id") sedid : String) : List<BUC> {
        println("BUCS - Valgre SED ID :  $sedid")
        val nav : Institusjon = Institusjon("NO","Norge")
        val andre : MutableList<Institusjon> = mutableListOf(Institusjon("SE","Sverige"), Institusjon("DK","Danmark"))
        val sendrev : SenderReceiver = SenderReceiver(nav, andre)
        val bucs : MutableList<BUC> =
                mutableListOf(BUC("BUC-1234-$sedid","SAKBH-4567", "Saknr:987654",sendrev,"NO;NAV","SED_P1000","NAV1001","")
                , BUC("BUC-5467-$sedid","SAKBH-7890", "Saknr:23023",sendrev,"NO;NAV","SED_P2000","NAV1002","")
                )
        return bucs
    }

}