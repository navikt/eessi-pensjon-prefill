package no.nav.eessi.eessifagmodul.services.pensjonsinformasjon.requesttransformer

import no.nav.eessi.eessifagmodul.services.pensjonsinformasjon.InformasjonsType
import org.springframework.stereotype.Component
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.StringWriter
import javax.xml.XMLConstants
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

private const val XS = "http://www.w3.org/2001/XMLSchema"

@Component
class RequestBuilder {

    fun addPensjonsinformasjonElement(document: Document, informasjonsType: InformasjonsType) {
        val baseElement = document.documentElement
        addNsToRootElement(baseElement, informasjonsType)
        addImportElement(document, informasjonsType, baseElement)
        addToElementList(document, informasjonsType, baseElement)
    }

    /* Legger til det nye namespacet i rot-elementet */
    private fun addNsToRootElement(baseElement: Element, informasjonsType: InformasjonsType) {
        baseElement.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:${informasjonsType.elementName()}", informasjonsType.namespace())
    }

    /* Legger til <import> i \schema */
    private fun addImportElement(document: Document, informasjonsType: InformasjonsType, baseElement: Element) {
        val importElement = document.createElementNS(XS, "xs:import")
        importElement.setAttribute("namespace", informasjonsType.namespace())
        importElement.setAttribute("schemaLocation", informasjonsType.schemaLocation())
        baseElement.insertBefore(importElement, baseElement.firstChild)
    }

    /* Legger til <element> i \schema\Pensjonsinformasjon\all */
    private fun addToElementList(document: Document, informasjonsType: InformasjonsType, baseElement: Element) {
        val elementElement = document.createElementNS(XS, "xs:element")
        elementElement.setAttribute("name", informasjonsType.elementName())
        elementElement.setAttribute("type", "${informasjonsType.elementName()}:${informasjonsType.typeName()}")
        val allList = baseElement.getElementsByTagNameNS(XS, "all").item(0) as Element
        allList.appendChild(elementElement)
    }
}

fun documentToString(xml: Document): String {
    val tf = TransformerFactory.newInstance().newTransformer().apply {
        setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        setOutputProperty(OutputKeys.INDENT, "yes")
        setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
    }
    val out = StringWriter()
    tf.transform(DOMSource(xml), StreamResult(out))
    return out.toString()
}