package no.nav.eessi.eessifagmodul.controllers

import no.nav.eessi.eessifagmodul.models.BUC
import no.nav.eessi.eessifagmodul.models.PENBrukerData
import no.nav.eessi.eessifagmodul.services.BuCKomponentService
import org.jetbrains.annotations.TestOnly
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController("BuCcontroller")
@RequestMapping("/buc")
class BUCController {

    @Autowired
    lateinit var service : BuCKomponentService

    @GetMapping("/")
    fun hentAlleBuC() : List<BUC> {
        return service.hentAlleBuc()
    }

    @GetMapping("/byid/{bucid}")
    fun hentBuC(@PathVariable("bucid") bucid : String) : BUC {
        return service.hentEnkelBuc(bucid)
    }

    @RequestMapping("/bydata/{penbrukerData}")
    fun opprettBuCogSED(@PathVariable("penbrukerData") penbrukerData : PENBrukerData) : BUC {
        return service.hentEnkelBuc(penbrukerData)
    }

    @TestOnly
    @GetMapping("/test/byid/{id}")
    fun hentTestBuCById(@PathVariable("bucid") bucid : String) : BUC {
        return service.hentTestEnkelBuc(bucid)
    }

    @TestOnly
    @GetMapping("/test/all")
    fun hentTestAlleBuc() : List<BUC> {
        return service.hentTestAlleBuc()
    }

}