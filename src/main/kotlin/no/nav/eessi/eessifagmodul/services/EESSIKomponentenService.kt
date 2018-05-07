package no.nav.eessi.eessifagmodul.services

import no.nav.eessi.eessifagmodul.models.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.*

@Service
class EESSIKomponentenService : KomponentService {

    private val logger: Logger by lazy { LoggerFactory.getLogger(EESSIKomponentenService::class.java)}

    @Autowired
    lateinit var restTemplate: RestTemplate

    override fun opprettBucogSEDrequest(data : PENBrukerData) : OpprettBuCogSEDRequest {
        val request = OpprettBuCogSEDRequest(
                KorrelasjonsID = UUID.randomUUID(),
                BUC = BUC(
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
                ),
                SED = SED(
                        SEDType = "P2000",
                        NAVSaksnummer = data.saksnummer,
                        ForsikretPerson = NavPerson(data.forsikretPerson),
                        Barn = listOf(NavPerson("123"), NavPerson("234")),
                        Samboer = NavPerson("345")
                )
        )
        logger.debug("/===========================================/")
        logger.debug("Request obj opprettt")
        logger.debug("Request obj: " + request.toString())
        logger.debug("/===========================================/")
        return request;
    }


    override fun opprettBuCogSEDresponse(request : OpprettBuCogSEDRequest) : OpprettBuCogSEDResponse? {
        val response = restTemplate.postForObject("/", request, OpprettBuCogSEDResponse::class.java)

        logger.debug("/===========================================/")
        logger.debug("Reponse obj opprettt")
        logger.debug("Reponse obj: $response")
        logger.debug("/===========================================/")
        return response
    }

    override fun opprettBuCogSED(requestData: PENBrukerData): OpprettBuCogSEDResponse? {
        return opprettBuCogSEDresponse(opprettBucogSEDrequest(requestData))
    }



}
