package no.nav.eessi.eessifagmodul.services

import no.nav.eessi.eessifagmodul.models.BUC
import no.nav.eessi.eessifagmodul.models.Institusjon
import no.nav.eessi.eessifagmodul.models.PENBrukerData
import no.nav.eessi.eessifagmodul.models.SenderReceiver
import org.jetbrains.annotations.TestOnly
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class BuCKomponentService {

    private val logger: Logger = LoggerFactory.getLogger(BuCKomponentService::class.java)

    private val path ="/cpi/buc/"

    @Autowired
    lateinit var eessiRest: EESSIRest

    fun hentAlleBuc() : List<BUC> {
        val list : MutableList<BUC> = mutableListOf()
        list.add(hentEnkelBuc("100"))
        list.add(hentEnkelBuc("200"))
        list.add(hentEnkelBuc("300"))
        list.add(hentEnkelBuc("400"))
        return list
    }

    fun hentEnkelBuc(id : String) : BUC {
        val data = PENBrukerData(id, "DummyTester", "123_567_$id")
        val buc = hentEnkelBuc(data)
        logger.debug("Buc : $buc")
        return buc
    }

    fun hentEnkelBuc(data: PENBrukerData) : BUC {
        val response = eessiRest.getRest().postForEntity(path,  data, BUC::class.java)
        logger.debug("Response : $response")
        if (response.statusCode != HttpStatus.OK) {
            val parter = SenderReceiver(
                    sender = Institusjon(landkode = "ERROR", navn = "ERROR"),
                    receiver = listOf(Institusjon(landkode = "ERROR", navn = "ERROR"))
            )
            return BUC("ERROR","ERROR","ERROR",parter,"ERROR","ERROR","ERROR","ERROR")
        }
        return response.body!!
    }

    @TestOnly
    fun hentTestEnkelBuc(id : String) : BUC {
        val data = PENBrukerData(id, "DummyTester", "123_567_$id")
        val buc = hentTestEnkelBuc(data)
        logger.debug("Buc : $buc")
        return buc
    }

    @TestOnly
    fun hentTestAlleBuc() : List<BUC> {
        val list : MutableList<BUC> = mutableListOf()
        list.add(hentEnkelBuc("100"))
        list.add(hentEnkelBuc("200"))
        list.add(hentEnkelBuc("300"))
        list.add(hentEnkelBuc("400"))
        return list
    }

    @TestOnly
    fun hentTestEnkelBuc(data: PENBrukerData) : BUC {
        val buc = BUC(
                flytType = "P_BUC_${data.saksnummer}",
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