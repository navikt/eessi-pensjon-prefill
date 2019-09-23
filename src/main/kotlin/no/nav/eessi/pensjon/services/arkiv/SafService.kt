package no.nav.eessi.pensjon.services.arkiv

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.*

@Service
class SafService(private val safGraphQlOidcRestTemplate: RestTemplate,
                 private val safRestOidcRestTemplate: RestTemplate) {

    private val logger = LoggerFactory.getLogger(SafService::class.java)

    private val hentDokumentMetadata_teller_navn = "eessipensjon_fagmodul.hentDokumentMetadata"
    private val hentDokumentMetadata_teller_type_vellykkede = counter(hentDokumentMetadata_teller_navn, "vellykkede")
    private val hentDokumentMetadata_teller_type_feilede = counter(hentDokumentMetadata_teller_navn, "feilede")
    private val hentDokumentInnhold_teller_navn = "eessipensjon_fagmodul.hentDokumentInnhold"
    private val hentDokumentInnhold_teller_type_vellykkede = counter(hentDokumentInnhold_teller_navn, "vellykkede")
    private val hentDokumentInnhold_teller_type_feilede = counter(hentDokumentInnhold_teller_navn, "feilede")
    private val mapper = jacksonObjectMapper()

    private final val TILLEGGSOPPLYSNING_RINA_SAK_ID_KEY = "eessi_pensjon_bucid"

    private final fun counter(name: String, type: String): Counter {
        return Metrics.counter(name, "type", type)
    }

    // Vi trenger denne konstruktøren for å kunne bruke @Spy med mockito
    constructor() : this(RestTemplate(), RestTemplate())

    fun hentDokumentMetadata(aktoerId: String) : HentMetadataResponse {
        logger.info("Henter dokument metadata for aktørid: $aktoerId")
        try {
             val headers = HttpHeaders()
             headers.contentType = MediaType.APPLICATION_JSON
             val httpEntity = HttpEntity(genererQuery(aktoerId), headers)
             val response = safGraphQlOidcRestTemplate.exchange("/",
                     HttpMethod.POST,
                     httpEntity,
                     String::class.java)
             if (response.statusCode.is2xxSuccessful) {
                 val mappedResponse = mapper.readValue(response.body!!, HentMetadataResponse::class.java)
                 hentDokumentMetadata_teller_type_vellykkede.increment()
                 return mappedResponse
             } else {
                 hentDokumentMetadata_teller_type_feilede.increment()
                 throw SafException("En feil oppstod under henting av dokument metadata fra SAF: ${response.statusCode}", response.statusCode)
             }
         } catch(ex: SafException) {
             logger.error("En feil oppstod under henting av dokument metadata fra SAF: $ex")
             hentDokumentMetadata_teller_type_feilede.increment()
             throw ex
         } catch(ex: Exception) {
             logger.error("En feil oppstod under henting av dokument metadata fra SAF: $ex")
             hentDokumentMetadata_teller_type_feilede.increment()
             throw SafException("En feil oppstod under henting av dokument metadata fra SAF: $ex", HttpStatus.INTERNAL_SERVER_ERROR)
         }
    }

    fun hentDokumentInnhold(journalpostId: String,
                            dokumentInfoId: String,
                            variantFormat: VariantFormat) : HentdokumentInnholdResponse {
        try {
            logger.info("Henter dokumentinnhold for journalpostId: $journalpostId, dokumentInfoId: $dokumentInfoId, variantformat: $variantFormat")
            val path = "/$journalpostId/$dokumentInfoId/$variantFormat"
            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_PDF

            val response = safRestOidcRestTemplate.exchange(path,
                    HttpMethod.GET,
                    HttpEntity("/", headers),
                    Resource::class.java)
            if (response.statusCode.is2xxSuccessful) {
                hentDokumentMetadata_teller_type_vellykkede.increment()
                val filnavn = response.headers.contentDisposition.filename
                val contentType = response.headers.contentType!!.toString()
                hentDokumentInnhold_teller_type_vellykkede.increment()

                val dokumentInnholdBase64 = String(Base64.getEncoder().encode(      response.body!!.inputStream.readBytes()))
                return HentdokumentInnholdResponse(dokumentInnholdBase64, filnavn!!, contentType)
            } else {
                throw SafException("En feil oppstod under henting av dokumentinnhold fra SAF: ${response.statusCode}", response.statusCode)
            }
        } catch(ex: SafException) {
            logger.error("En feil oppstod under henting av dokumentInnhold fra SAF: $ex")
            hentDokumentInnhold_teller_type_feilede.increment()
            throw ex
        } catch(ex: Exception) {
            logger.error("En feil oppstod under henting av dokumentInnhold fra SAF: $ex")
            hentDokumentInnhold_teller_type_feilede.increment()
            throw SafException("En feil oppstod under henting av dokumentinnhold fra SAF", HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    private fun genererQuery(aktoerId: String): String {
        val request = SafRequest(variables = Variables(BrukerId(aktoerId, BrukerIdType.AKTOERID), 10000))
        return request.toJson()
    }

    /**
     * Returnerer en distinct liste av rinaSakIDer basert på tilleggsinformasjon i journalposter for en aktør
     * @param metadata journalpostmetadata fra JOARK datamodellen
     */
    fun hentRinaSakIderFraDokumentMetadata(aktoerId: String): List<String> {
        val metadata = hentDokumentMetadata(aktoerId)
        val rinaSakIder = mutableListOf<String>()
        metadata.data.dokumentoversiktBruker.journalposter.forEach { journalpost ->
            journalpost.tilleggsopplysninger.forEach { tilleggsopplysning ->
                if(tilleggsopplysning["nokkel"].equals(TILLEGGSOPPLYSNING_RINA_SAK_ID_KEY)) {
                    rinaSakIder.add(tilleggsopplysning["verdi"].toString())
                }
            }
        }
        return rinaSakIder.distinct()
    }
}


