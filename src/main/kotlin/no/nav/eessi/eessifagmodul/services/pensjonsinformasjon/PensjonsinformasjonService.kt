package no.nav.eessi.eessifagmodul.services.pensjonsinformasjon

import no.nav.eessi.sed.v1.px000.Pensjonsinformasjon
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.ResourceUtils
import org.springframework.web.client.RestTemplate
import org.w3c.dom.Document
import javax.xml.parsers.DocumentBuilderFactory

private val logger = LoggerFactory.getLogger(PensjonsinformasjonService::class.java)

@Service
class PensjonsinformasjonService(val pensjonsinformasjonOidcRestTemplate: RestTemplate, val requestTransformer: RequestBuilder) {

    fun hentAlt(saksId: String): Pensjonsinformasjon {
        val document = getBaseDocument()
        requestTransformer.addPensjonsinformasjonElement(document, InformasjonsType.AVDOD)
        requestTransformer.addPensjonsinformasjonElement(document, InformasjonsType.INNGANG_OG_EXPORT)
        requestTransformer.addPensjonsinformasjonElement(document, InformasjonsType.PERSON)
        requestTransformer.addPensjonsinformasjonElement(document, InformasjonsType.SAK)
        requestTransformer.addPensjonsinformasjonElement(document, InformasjonsType.TRYGDEAVTALE)
        requestTransformer.addPensjonsinformasjonElement(document, InformasjonsType.TRYGDETID_AVDOD_FAR_LISTE)
        requestTransformer.addPensjonsinformasjonElement(document, InformasjonsType.TRYGDETID_AVDOD_LISTE)
        requestTransformer.addPensjonsinformasjonElement(document, InformasjonsType.TRYGDETID_AVDOD_MOR_LISTE)
        requestTransformer.addPensjonsinformasjonElement(document, InformasjonsType.TRYGDETID_LISTE)
        requestTransformer.addPensjonsinformasjonElement(document, InformasjonsType.VEDTAK)
        requestTransformer.addPensjonsinformasjonElement(document, InformasjonsType.VILKARSVURDERING_LISTE)
        requestTransformer.addPensjonsinformasjonElement(document, InformasjonsType.YTELSE_PR_MAANED_LISTE)
        logger.debug("\n" + documentToString(document))
        return Pensjonsinformasjon()
    }

    private fun getBaseDocument(): Document {
        val dbf = DocumentBuilderFactory.newInstance()
        dbf.isNamespaceAware = true
        val db = dbf.newDocumentBuilder()
        return db.parse(ResourceUtils.getFile("classpath:schemas/pensjonsinformasjonRequestBuilder/baseRequest.template"))
    }
}