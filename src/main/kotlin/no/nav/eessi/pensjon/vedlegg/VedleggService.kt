package no.nav.eessi.pensjon.vedlegg

import no.nav.eessi.pensjon.vedlegg.client.Dokument
import no.nav.eessi.pensjon.vedlegg.client.EuxVedleggClient
import no.nav.eessi.pensjon.vedlegg.client.HentMetadataResponse
import no.nav.eessi.pensjon.vedlegg.client.HentdokumentInnholdResponse
import no.nav.eessi.pensjon.vedlegg.client.SafClient
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
                .firstOrNull { it.dokumentInfoId == dokumentInfoId }
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

    fun hentRinaSakIderFraMetaData(aktoerId: String) =  safClient.hentRinaSakIderFraDokumentMetadata(aktoerId)
}