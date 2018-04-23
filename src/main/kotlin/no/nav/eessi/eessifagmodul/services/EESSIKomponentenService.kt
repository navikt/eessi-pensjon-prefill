package no.nav.eessi.eessifagmodul.services

import no.nav.eessi.eessifagmodul.models.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.*

@Service
class EESSIKomponentenService(
        templateBuilder: RestTemplateBuilder,
        @Value("\${eessibasis.url}") url: String) {

    val restTemplate: RestTemplate = templateBuilder.rootUri(url).build()

    fun opprettEUFlyt(saksnummer: String, saksbehandler: String, forsikretPerson: String): OpprettEUFlytResponse? {

        val request = OpprettEUFlytRequest(
                KorrelasjonsID = UUID.randomUUID(),
                BUC = BUC(
                        flytType = "P_BUC_01",
                        saksnummerPensjon = saksnummer,
                        saksbehandler = saksbehandler,
                        Parter = SenderReceiver(
                                sender = Institusjon(landkode = "NO", navn = "NAV"),
                                receiver = listOf(Institusjon(landkode = "DK", navn = "ATP"))
                        )
                ),
                SED = SED(
                        SEDType = "P2000",
                        NAVSaksnummer = saksnummer,
                        ForsikretPerson = NavPerson(forsikretPerson),
                        Barn = listOf(NavPerson("123"), NavPerson("234")),
                        Samboer = NavPerson("345")
                )
        )

        return restTemplate.postForObject("/internal/testmethod", request, OpprettEUFlytResponse::class.java)
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
}
