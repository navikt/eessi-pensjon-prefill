package no.nav.eessi.pensjon.vedlegg

import no.nav.eessi.pensjon.vedlegg.client.*
import org.springframework.stereotype.Service

@Service
class VedleggService(private val safClient: SafClient,
                     private val euxVedleggClient: EuxVedleggClient) {

    fun hentDokumentMetadata(aktoerId: String): HentMetadataResponse {
        return safClient.hentDokumentMetadata(aktoerId)
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