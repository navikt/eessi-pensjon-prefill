package no.nav.eessi.eessifagmodul.services.pensjonsinformasjon

import no.nav.eessi.eessifagmodul.services.pensjonsinformasjon.requesttransformer.RequestBuilder
import no.nav.eessi.eessifagmodul.services.pensjonsinformasjon.requesttransformer.documentToString
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.ResourceUtils
import org.springframework.web.client.RestTemplate
import org.w3c.dom.Document
import javax.xml.parsers.DocumentBuilderFactory

private val logger = LoggerFactory.getLogger(PensjonsinformasjonService::class.java)

@Service
class PensjonsinformasjonService(val pensjonsinformasjonRestTemplate: RestTemplate, val requestTransformer: RequestBuilder) {

    fun hentAlt(saksId: String) {
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
    }

    private fun getBaseDocument(): Document {
        val dbf = DocumentBuilderFactory.newInstance()
        dbf.isNamespaceAware = true
        val db = dbf.newDocumentBuilder()
        return db.parse(ResourceUtils.getFile("classpath:pensjonsinformasjonRequestTransformer/base.xsd"))
    }
}