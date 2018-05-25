package no.nav.eessi.eessifagmodul.controllers

import no.nav.eessi.eessifagmodul.domian.RequestException
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.services.EESSIKomponentenService
import org.jetbrains.annotations.TestOnly
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*


/**
 * Klasse for prototyping, test o.l
 */
@CrossOrigin
@RestController
@RequestMapping("/komponent")
class KomponentController {

    @Autowired
    lateinit var eessiKomponentenService: EESSIKomponentenService

    @Value("\${no.nav.eessi.eessifagmodul.hello:}")
    lateinit var test: String

    @Autowired
    lateinit var props: Properties

    @RequestMapping("/req")
    fun testMethodBucogSED(): OpprettBuCogSEDResponse {
        val brukerData = PENBrukerData("123456", "DummyTester1", "12123123123")
        val response = eessiKomponentenService.opprettBuCogSED(brukerData)
        return response!!
    }

    @GetMapping("/prop")
    fun hentutProperties(): List<Pair<String, String>> {
        return printSystemProperties()
    }

    fun printSystemProperties(): List<Pair<String, String>> {
        println("System Properties\n---------------------------------------------")

        val keynames = props.stringPropertyNames()
        val iterator = keynames.iterator()

        val list: MutableList<Pair<String, String>> = mutableListOf()
        iterator.forEach {
            val strValue = props.getProperty(it)
            println("= :  $it, Value : $strValue")
            val pair: Pair<String, String> = Pair("Key : $it", "Value : $strValue")
            list.add(pair)
        }
        println("\n---------------------------------------------")
        return list
    }


    @GetMapping("/opprettRequest")
    fun opprettBucogSEDrequest(): OpprettBuCogSEDRequest {
        val data = PENBrukerData("123456", "Dummybruker", "Ole Olsen", Instant.now())
        return eessiKomponentenService.opprettBucogSEDrequest(data)
    }

    @GetMapping("/opprettPENBrukerData")
    fun getOpprettBrukerData(): PENBrukerData {
        return PENBrukerData("123456", "Dummybruker", "Ole Olsen", Instant.now())
    }

    @PostMapping("/opprettResponse")
    fun opprettBuCogSEDResponse(): OpprettBuCogSEDResponse? {
        val data = PENBrukerData("123456", "Dummybruker", "Ole Olsen", Instant.now())
        return eessiKomponentenService.opprettBuCogSED(data)
    }

    @TestOnly
    @GetMapping("/hentsedid/{id}")
    fun hentSedbyID(@PathVariable("id") sedid: Int): SED {
        println("Valgre SED ID :  $sedid")

        if (sedid == 40) {
            throw RuntimeException()
        } else if (sedid == 800) {
            throw RequestException("medling")
        }

        val person = NavPerson("12345678911")
        return SED(SEDType = "SED_P1000", NAVSaksnummer = "NAV-SAK-1234", ForsikretPerson = person, Barn = null, Samboer = null)
    }

    @TestOnly
    @GetMapping("/hentsedid/{id}/bucs")
    fun hentBucsTilSedID(@PathVariable("id") sedid: String): List<BUC> {
        println("BUCS - Valgre SED ID :  $sedid")
        val nav = Institusjon("NO", "Norge")
        val andre: MutableList<Institusjon> = mutableListOf(Institusjon("SE", "Sverige"), Institusjon("DK", "Danmark"))
        val sendrev = SenderReceiver(nav, andre)
        return mutableListOf(BUC("BUC-1234-$sedid", "SAKBH-4567", "Saknr:987654", sendrev, "NO;NAV", "SED_P1000", "NAV1001", ""), BUC("BUC-5467-$sedid", "SAKBH-7890", "Saknr:23023", sendrev, "NO;NAV", "SED_P2000", "NAV1002", "")
        )
    }

}