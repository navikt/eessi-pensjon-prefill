package no.nav.eessi.eessifagmodul.services.eux

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.utils.createErrorMessage
import no.nav.eessi.eessifagmodul.utils.mapJsonToAny
import no.nav.eessi.eessifagmodul.utils.typeRef
import no.nav.eessi.eessifagmodul.utils.typeRefs
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Description
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.io.IOException
import java.net.UnknownHostException

private val logger = LoggerFactory.getLogger(EuxService::class.java)

@Service
@Description("Service class for EuxBasis - EuxCpiServiceController.java")
class EuxService(private val euxOidcRestTemplate: RestTemplate) {

    private val EUX_MULIGEAKSJONER_TELLER_NAVN = "eessipensjon_fagmodul.euxmuligeaksjoner"
    private val EUX_MULIGEAKSJONER_TELLER_TYPE_VELLYKKEDE = counter(EUX_MULIGEAKSJONER_TELLER_NAVN, "vellykkede")
    private val EUX_MULIGEAKSJONER_TELLER_TYPE_FEILEDE = counter(EUX_MULIGEAKSJONER_TELLER_NAVN, "feilede")
    private val EUX_SENDSED_TELLER_NAVN = "eessipensjon_fagmodul.sendsed"
    private val EUX_SENDSED_TELLER_TYPE_VELLYKKEDE = counter(EUX_SENDSED_TELLER_NAVN, "vellykkede")
    private val EUX_SENDSED_TELLER_TYPE_FEILEDE = counter(EUX_SENDSED_TELLER_NAVN, "feilede")
    private val EUX_OPPRETTSED_TELLER_NAVN = "eessipensjon_fagmodul.opprettsed"
    private val EUX_OPPRETTSED_TELLER_TYPE_VELLYKKEDE = counter(EUX_OPPRETTSED_TELLER_NAVN, "vellykkede")
    private val EUX_OPPRETTSED_TELLER_TYPE_FEILEDE = counter(EUX_OPPRETTSED_TELLER_NAVN, "feilede")
    private val EUX_HENTSED_TELLER_NAVN = "eessipensjon_fagmodul.hentsed"
    private val EUX_HENTSED_TELLER_TYPE_VELLYKKEDE = counter(EUX_HENTSED_TELLER_NAVN, "vellykkede")
    private val EUX_HENTSED_TELLER_TYPE_FEILEDE = counter(EUX_HENTSED_TELLER_NAVN, "feilede")
    private val EUX_SLETTSED_TELLER_NAVN = "eessipensjon_fagmodul.slettsed"
    private val EUX_SLETTSED_TELLER_TYPE_VELLYKKEDE = counter(EUX_SLETTSED_TELLER_NAVN, "vellykkede")
    private val EUX_SLETTSED_TELLER_TYPE_FEILEDE = counter(EUX_SLETTSED_TELLER_NAVN, "feilede")
    private val EUX_OPPRETTBUCOGSED_TELLER_NAVN = "eessipensjon_fagmodul.opprettbucogsed"
    private val EUX_OPPRETTBUCOGSED_TELLER_TYPE_VELLYKKEDE = counter(EUX_OPPRETTBUCOGSED_TELLER_NAVN, "vellykkede")
    private val EUX_OPPRETTBUCOGSED_TELLER_TYPE_FEILEDE = counter(EUX_OPPRETTBUCOGSED_TELLER_NAVN, "feilede")

    fun counter(name: String, type: String): Counter {
        return Metrics.counter(name, "type", type)
    }

    //Henter en liste over tilgjengelige aksjoner for den aktuelle RINA saken PK-51365"
    fun getPossibleActions(euSaksnr: String): List<RINAaksjoner> {
        val builder = UriComponentsBuilder.fromPath("/MuligeAksjoner")
                .queryParam("RINASaksnummer", euSaksnr)

        val httpEntity = HttpEntity("")

        val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.GET, httpEntity, typeRef<String>())
        val responseBody = response.body!!
        try {
            if (response.statusCode.isError) {
                EUX_MULIGEAKSJONER_TELLER_TYPE_FEILEDE.increment()
                throw createErrorMessage(responseBody)
            } else {
                EUX_MULIGEAKSJONER_TELLER_TYPE_VELLYKKEDE.increment()
                return mapJsonToAny(responseBody, typeRefs())
            }
        } catch (ex: IOException) {
            EUX_MULIGEAKSJONER_TELLER_TYPE_FEILEDE.increment()
            throw RuntimeException(ex.message)
        }
    }

    /*
        hjelpe funksjon for sendSED må hente ut dokumentID for valgt sed f.eks P2000
     */
    fun hentDocuemntID(euxCaseId: String, sed: String): String {
        val aksjon = "Send"
        val aksjoner = getPossibleActions(euxCaseId)
        aksjoner.forEach {
            if (sed == it.dokumentType && aksjon == it.navn) {
                return it.dokumentId ?: throw IkkeGyldigKallException("Ingen gyldig dokumentID funnet")
            }
        }
        throw IkkeGyldigKallException("Ingen gyldig dokumentID funnet")
    }

    //TODO: euxBasis hva finnes av metoder for:
    //TODO: euxBasis metode for å legge til flere mottakere (Institusjoner)
    //TODO: euxBasis metode for å fjenre en eller flere mottakere (Institusjoner)

    /**
     * call to send sed on rina document.
     *
     * @parem euxCaseID (rinaid)
     * @param korrelasjonID CorrelationId
     * @param sed (sed type vedtak, P2000)
     */
    fun sendSED(euxCaseId: String, sed: String, korrelasjonID: String): Boolean {
        val documentID = hentDocuemntID(euxCaseId, sed)
        if (documentID.isBlank()) {
            throw IkkeGyldigKallException("Kan ikke Sende valgt sed")
        }

        val builder = UriComponentsBuilder.fromPath("/SendSED")
                .queryParam("RINASaksnummer", euxCaseId)
                .queryParam("DokumentID", documentID)
                .queryParam("KorrelasjonsID", korrelasjonID)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val httpEntity = HttpEntity("", headers)

        logger.info("sendSED KorrelasjonsID : {}", korrelasjonID)
        val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.POST, httpEntity, String::class.java)
        logger.debug("Response SendSED på Rina: $euxCaseId, response:  $response")

        if (response.statusCodeValue == 200) {
            EUX_SENDSED_TELLER_TYPE_VELLYKKEDE.increment()
            return true
        }
        EUX_SENDSED_TELLER_TYPE_FEILEDE.increment()
        return false
    }


    /**
     * call to make new sed on existing rina document.
     *
     * @param sed (actual SED)
     * @parem euxCaseID (rina id)
     * @param korrelasjonID CorrelationId
     */
    //void no confirmaton?
    @Throws(UnknownHostException::class)
    fun createSEDonExistingRinaCase(sed: SED, euxCaseId: String, korrelasjonID: String): HttpStatus {

        val builder = UriComponentsBuilder.fromPath("/SED")
                .queryParam("RINASaksnummer", euxCaseId)
                .queryParam("KorrelasjonsID", korrelasjonID)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val httpEntity = HttpEntity(sed.toJson(), headers)

        logger.info("createSEDonExistingRinaCase KorrelasjonsID : {}", korrelasjonID)
        val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.POST, httpEntity, String::class.java)

        if(response.statusCode.is2xxSuccessful) {
            logger.debug("Response opprett SED på Rina: $euxCaseId, response:  $response")
            EUX_OPPRETTSED_TELLER_TYPE_VELLYKKEDE.increment()
        }
        logger.error("Opprettelse av SED på Rina feilet")
        EUX_OPPRETTSED_TELLER_TYPE_FEILEDE.increment()

        return response.statusCode
    }

    /**
     * call to fetch existing sed document on existing rina case.
     * @param euxCaseId (rina id)
     * @param documentId (sed documentid)
     */
    fun fetchSEDfromExistingRinaCase(euxCaseId: String, documentId: String): SED {

        val builder = UriComponentsBuilder.fromPath("/SED")
                .queryParam("RINASaksnummer", euxCaseId)
                .queryParam("DokumentID", documentId)

        val httpEntity = HttpEntity("")

        try {
            val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.GET, httpEntity, typeRef<String>())
            val responseBody = response.body ?: throw SedDokumentIkkeOpprettetException("Sed dokument ikke funnet")
            if (response.statusCode.isError) {
                EUX_HENTSED_TELLER_TYPE_FEILEDE.increment()
                throw SedDokumentIkkeLestException("Får ikke lest SED dokument fra Rina")
            } else {
                EUX_HENTSED_TELLER_TYPE_VELLYKKEDE.increment()
                return SED.fromJson(responseBody) //  mapJsonToAny(responseBody, typeRefs())
            }
        } catch (ex: IOException) {
            EUX_HENTSED_TELLER_TYPE_FEILEDE.increment()
            throw RuntimeException(ex.message)
        }
    }

    /**
     * call to delete existing sed document on existing rina case.
     * @param euxCaseId (rina id)
     * @param documentId (sed documentid)
     */
    fun deleteSEDfromExistingRinaCase(euxCaseId: String, documentId: String) {
        val builder = UriComponentsBuilder.fromPath("/SED")
                .queryParam("RINASaksnummer", euxCaseId)
                .queryParam("DokumentID", documentId)

        val httpEntity = HttpEntity("")
        try {
            val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.DELETE, httpEntity, typeRef<String>()).statusCode
            if(response.is2xxSuccessful){
                EUX_SLETTSED_TELLER_TYPE_VELLYKKEDE.increment()
            }
        } catch (ex: IOException) {
            EUX_SLETTSED_TELLER_TYPE_FEILEDE.increment()
            throw RuntimeException(ex.message)
        }
    }


    /**
     * Call the orchestrator endpoint with necessary information to create a case in RINA, set
     * its receiver, create a document and add attachments to it.
     *
     * The method is asynchronous and simply returns the new case ID after creating the case. The rest
     * of the logic is executed afterwards.
     *
     * if something goes wrong after caseid. no sed is shown on case.
     *
     * @param sed SED-document in NAV-format
     * @param bucType The RINA case type to create
     * @param fagSaknr local case number
     * @param mottaker The RINA ID of the organisation that is to receive the SED on a sned action
     * @param vedleggType File type of attachments
     * @param korrelasjonID CorrelationId
     * @return The ID of the created case
     */
    fun createCaseAndDocument(sed: SED, bucType: String, fagSaknr: String, mottaker: String, vedleggType: String = "", korrelasjonID: String): String {

        val builder = UriComponentsBuilder.fromPath("/OpprettBuCogSED")
                .queryParam("BuCType", bucType)
                .queryParam("FagSakNummer", fagSaknr)
                .queryParam("MottakerID", mottaker)
                .queryParam("Filtype", vedleggType)
                .queryParam("KorrelasjonsID", korrelasjonID)

        val map: MultiValueMap<String, Any> = LinkedMultiValueMap()
        val document = object : ByteArrayResource(sed.toJson().toByteArray()) {
            override fun getFilename(): String? {
                return "document"
            }
        }
        map.add("document", document)
        map.add("attachment", null)

        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA

        val httpEntity = HttpEntity(map, headers)

        logger.info("createCaseAndDocument KorrelasjonsID : {}", korrelasjonID)
        val response = euxOidcRestTemplate.exchange(builder.toUriString(), HttpMethod.POST, httpEntity, String::class.java)
        return response.body ?: throw RinaCasenrIkkeMottattException("Ikke mottatt RINA casenr, feiler ved opprettelse av BUC og SED")
    }
}
