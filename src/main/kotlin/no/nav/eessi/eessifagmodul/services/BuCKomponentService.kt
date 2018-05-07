package no.nav.eessi.eessifagmodul.services

import no.nav.eessi.eessifagmodul.models.BUC
import no.nav.eessi.eessifagmodul.models.Institusjon
import no.nav.eessi.eessifagmodul.models.PENBrukerData
import no.nav.eessi.eessifagmodul.models.SenderReceiver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class BuCKomponentService {

    private val logger: Logger = LoggerFactory.getLogger(BuCKomponentService::class.java)

    //BASIS urlpath
    val path : String = "/cpi"

    //@Autowired
    //lateinit var restTemplate: RestTemplate

    fun hentEnkelBuc(id : String) : BUC {
        val data = PENBrukerData(id, "DummyTester", "12345678")
        val buc = BUC(
                flytType = "P_BUC_01",
                saksnummerPensjon = data.saksnummer,
                saksbehandler = data.saksbehandler,
                Parter = SenderReceiver(
                        sender = Institusjon(landkode = "NO", navn = "NAV"),
                        receiver = listOf(Institusjon(landkode = "DK", navn = "ATP"))
                ),
                NAVSaksnummer =  "nav_saksnummer",
                SEDType = "SED_type",
                notat_tmp = "Temp fil for å se hva som skjer"
        )
        logger.debug("Buc : $buc")
        return buc
    }

}