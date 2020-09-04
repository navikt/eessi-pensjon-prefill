package no.nav.eessi.pensjon.vedlegg

import no.nav.eessi.pensjon.vedlegg.client.*
import org.springframework.stereotype.Service

@Service
class VedleggService(private val safClient: SafClient,
                     private val euxVedleggClient: EuxVedleggClient) {

    fun hentDokumentMetadata(aktoerId: String): HentMetadataResponse {
        return safClient.hentDokumentMetadata(aktoerId)
    }

    fun hentDokumentMetadata(aktoerId: String,
                             journalpostId : String,
                             dokumentInfoId: String) : Dokument? {
        val alleMetadataForAktoerId = safClient.hentDokumentMetadata(aktoerId)

        return alleMetadataForAktoerId.data.dokumentoversiktBruker.journalposter
                .filter { it.journalpostId == journalpostId }
                .flatMap { it.dokumenter }
                .filter{ it.dokumentInfoId == dokumentInfoId }
                .let { dokumentListe ->
                    if(dokumentListe.isEmpty()) {
                        return null
                    } else dokumentListe.first() // Det finnes bare en unik dokumentInfoId i hver journalpost
                }
    }

    fun hentDokumentInnhold(journalpostId: String,
                           dokumentInfoId: String,
                           variantFormat: String): HentdokumentInnholdResponse {
        return safClient.hentDokumentInnhold(journalpostId, dokumentInfoId, variantFormat)
    }

    fun leggTilVedleggPaaDokument(aktoerId: String,
                                  rinaSakId: String,
                                  rinaDokumentId: String,
                                  filInnhold: String,
                                  fileName: String,
                                  filtype: String) {
        euxVedleggClient.leggTilVedleggPaaDokument(aktoerId, rinaSakId, rinaDokumentId, filInnhold, fileName, filtype)
    }
}