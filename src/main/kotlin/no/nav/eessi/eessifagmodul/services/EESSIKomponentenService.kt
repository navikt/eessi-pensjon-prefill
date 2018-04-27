package no.nav.eessi.eessifagmodul.services

import no.nav.eessi.eessifagmodul.models.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.nio.charset.Charset
import java.util.*

@Service
class EESSIKomponentenService(templateBuilder: RestTemplateBuilder, @Value("\${eessibasis.url}") url: String) {
    private val logger: Logger = LoggerFactory.getLogger(EESSIKomponentenService::class.java)// by lazy { LoggerFactory.getLogger(EESSIKomponentenService::class.java) }


    val restTemplate: RestTemplate = templateBuilder.rootUri(url).build()
    val header  = HttpHeaders()

    fun opprettBuCogSED(saksnummer: String, saksbehandler: String, forsikretPerson: String): OpprettBuCogSEDResponse? {

        val request = OpprettBuCogSEDRequest(
                KorrelasjonsID = UUID.randomUUID(),
                BUC = BUC(
                        flytType = "P_BUC_01",
                        saksnummerPensjon = saksnummer,
                        saksbehandler = saksbehandler,
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
                        NAVSaksnummer = saksnummer,
                        ForsikretPerson = NavPerson(forsikretPerson),
                        Barn = listOf(NavPerson("123"), NavPerson("234")),
                        Samboer = NavPerson("345")
                )
        )
        logger.debug("Request obj: " + request.toString())
        val response = restTemplate.postForObject("/", request, OpprettBuCogSEDResponse::class.java)
        logger.debug("Reponse obj:" + response.toString())
        return response
    }

    fun opprettBucogSEDrequest(data : PENBrukerData) : OpprettBuCogSEDRequest {
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

    fun opprettBuCogSEDresponse(request : OpprettBuCogSEDRequest) : OpprettBuCogSEDResponse? {
        val response = restTemplate.postForObject("/", request, OpprettBuCogSEDResponse::class.java)
        logger.debug("Reponse obj:" + response.toString())
        return response
    }

    data class OpprettBuCogSEDRequest(
            val KorrelasjonsID: UUID,
            val BUC: BUC?,
            val SED: SED?,
            val Vedlegg: List<Any>? = null
    )

/*
    fun opprettEUFlyt(saksnummer: String, saksbehandler: String, forsikretPerson: String): UUID? {


        //return restTemplate.postForObject("/internal/testmethod", request, OpprettBuCogSEDResponse::class.java)?.KorrelasjonsID
        //header.setContentType(MediaType.APPLICATION_JSON_UTF8)
        //restTemplate.getMessageConverters().add(0, StringHttpMessageConverter(Charset.forName("UTF-8")))

        val request = OpprettEUFlytRequest(
                KorrelasjonsID = UUID.randomUUID(),
                BUC = BUC(
                        flytType = "P_BUC_01",
                        saksnummerPensjon = saksnummer,
                        saksbehandler = saksbehandler,
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
                        NAVSaksnummer = saksnummer,
                        ForsikretPerson = NavPerson(forsikretPerson),
                        Barn = listOf(NavPerson("123"), NavPerson("234")),
                        Samboer = NavPerson("345")
                )
        )

        return restTemplate.postForObject("/internal/testmethod", request, OpprettEUFlytResponse::class.java)?.KorrelasjonsID
    }


    data class OpprettEUFlytRequest(
            val KorrelasjonsID: UUID,
            val BUC: BUC,
            val SED: SED?,
            val Vedlegg: List<Any>? = null
    )

    data class OpprettEUFlytResponse(
            val KorrelasjonsID: UUID
    )
    */
}
