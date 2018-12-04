package no.nav.eessi.eessifagmodul.services.pensjonsinformasjon


import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.sak.V1Sak
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import org.springframework.web.util.UriTemplateHandler
import java.io.StringReader
import javax.xml.bind.JAXBContext
import javax.xml.transform.stream.StreamSource

private val logger = LoggerFactory.getLogger(PensjonsinformasjonService::class.java)

@Service
class PensjonsinformasjonService(val pensjonsinformasjonOidcRestTemplate: RestTemplate, val requestBuilder: RequestBuilder) {

    fun hentAltPaaSak(sakId: String = "", pendata: Pensjonsinformasjon): V1Sak {
        logger.debug("Pendata: $pendata")
        if (sakId.isNotBlank()) {
            pendata.brukersSakerListe.brukersSakerListe.forEach {
                if (sakId == it.sakId.toString())
                    return it
            }
        }
        return V1Sak()
    }

    fun hentAltPaaFnr(fnr: String): Pensjonsinformasjon {
        //APIet skal ha urlen {host}:{port}/pensjon-ws/api/pensjonsinformasjon/v1/{ressurs}?sakId=123+fom=2018-01-01+tom=2018-28-02.

        val informationBlocks = listOf(
                InformasjonsType.BRUKERS_SAKER_LISTE
        )
        val document = requestBuilder.getBaseRequestDocument()

        informationBlocks.forEach {
            requestBuilder.addPensjonsinformasjonElement(document, it)
        }

        //logger.debug("Requestbody:\n${document.documentToString()}")

        val sakHandler = PensjoninformasjonUriHandler("https://wasapp-t5.adeo.no/pensjon-ws/api/pensjonsinformasjon/v1")
        val response = doRequest("/fnr/", fnr, document.documentToString(), sakHandler)
        validateResponse(informationBlocks, response)
        //logger.debug("Response: $response")
        return response
    }


    fun hentAltPaaVedtak(vedtaksId: String): Pensjonsinformasjon {

        val informationBlocks = listOf(
                InformasjonsType.AVDOD,
                InformasjonsType.INNGANG_OG_EXPORT,
                InformasjonsType.PERSON,
                InformasjonsType.SAKALDER,
                InformasjonsType.TRYGDEAVTALE,
                InformasjonsType.TRYGDETID_AVDOD_FAR_LISTE,
                InformasjonsType.TRYGDETID_AVDOD_LISTE,
                InformasjonsType.TRYGDETID_AVDOD_MOR_LISTE,
                InformasjonsType.TRYGDETID_LISTE,
                InformasjonsType.VEDTAK,
                InformasjonsType.VILKARSVURDERING_LISTE,
                InformasjonsType.YTELSE_PR_MAANED_LISTE
        )

        val document = requestBuilder.getBaseRequestDocument()

        informationBlocks.forEach {
            requestBuilder.addPensjonsinformasjonElement(document, it)
        }
        //logger.debug("Requestbody:\n${document.documentToString()}")

        val sakHandler = PensjoninformasjonUriHandler("https://wasapp-t5.adeo.no/pensjon-ws/api/pensjonsinformasjon/v1")
        val response = doRequest("/vedtak", vedtaksId, document.documentToString(), sakHandler)
        validateResponse(informationBlocks, response)
        //logger.debug("Response: $response")
        return response
    }

    private fun validateResponse(informationBlocks: List<InformasjonsType>, response: Pensjonsinformasjon) {
        // TODO: Hva skal vi egentlig validere? Skal vi validere noe mer enn at vi fikk en gyldig xml-response, som skjer ved JAXB-marshalling?
    }

    private fun doRequest(path: String, id: String, requestBody: String, uriHandler: UriTemplateHandler = PensjoninformasjonUriHandler("https://wasapp-t4.adeo.no/pensjon-ws/api/pensjonsinformasjon")): Pensjonsinformasjon {
        val headers = HttpHeaders()
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
        val requestEntity = HttpEntity(requestBody, headers)

        val uriBuilder = UriComponentsBuilder.fromPath(path).pathSegment(id)

        pensjonsinformasjonOidcRestTemplate.uriTemplateHandler = uriHandler
        val responseEntity = pensjonsinformasjonOidcRestTemplate.exchange(

                uriBuilder.toUriString(),
                HttpMethod.POST,
                requestEntity,
                String::class.java)

        if (responseEntity.statusCode.isError) {
            logger.error("Received ${responseEntity.statusCode} from pensjonsinformasjon")
            if (responseEntity.hasBody()) {
                logger.error(responseEntity.body.toString())
            }
            throw RuntimeException("Received ${responseEntity.statusCode} ${responseEntity.statusCode.reasonPhrase} from pensjonsinformasjon")
        }

        //logger.debug("Responsebody:\n\n${responseEntity.body}\n\n")
        val context = JAXBContext.newInstance(Pensjonsinformasjon::class.java)
        val unmarshaller = context.createUnmarshaller()

        val res = unmarshaller.unmarshal(StreamSource(StringReader(responseEntity.body)), Pensjonsinformasjon::class.java)
        return res.value as Pensjonsinformasjon
    }

}

