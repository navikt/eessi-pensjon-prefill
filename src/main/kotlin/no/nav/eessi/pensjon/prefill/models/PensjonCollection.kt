package no.nav.eessi.pensjon.prefill.models

import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.prefill.models.pensjon.P15000overfoeringAvPensjonssakerTilEessiDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P6000MeldingOmVedtakDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P8000AnmodningOmTilleggsinformasjon

data class PensjonCollection (
    val vedtakId: String? = null,
    val sedType: SedType ? = null,
    val p6000Data: P6000MeldingOmVedtakDto? = null,
    val p8000Data: P8000AnmodningOmTilleggsinformasjon? = null,
    val p15000Data: P15000overfoeringAvPensjonssakerTilEessiDto? = null,
    val p2xxxMeldingOmPensjonDto: P2xxxMeldingOmPensjonDto? = null
) {
    @Suppress("UNCHECKED_CAST")
    fun <T> hentVedtak(): T? {
        return when (sedType) {
            SedType.P6000 -> p6000Data?.vedtak as? T
            SedType.P2000 ->  p2xxxMeldingOmPensjonDto?.vedtak as? T
            SedType.P2100 ->  p2xxxMeldingOmPensjonDto?.vedtak as? T
            SedType.P2200 -> p2xxxMeldingOmPensjonDto?.vedtak as? T
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> hentSak(): T? {
        return when (sedType) {
            SedType.P6000 -> p6000Data?.sakAlder as? T
            SedType.P2000, SedType.P2100, SedType.P2200 -> p2xxxMeldingOmPensjonDto?.sak as? T
            else -> null
        }
    }
}