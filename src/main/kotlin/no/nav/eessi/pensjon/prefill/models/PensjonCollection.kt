package no.nav.eessi.pensjon.prefill.models

import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.prefill.models.pensjon.P15000overfoeringAvPensjonssakerTilEessiDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P6000MeldingOmVedtakDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P8000AnmodningOmTilleggsinformasjon

data class PensjonCollection (
    val vedtak: VedtakInterface? = null,
    val sak: SakInterface? = null,
    val sedType: SedType ? = null,
    val p6000Data: P6000MeldingOmVedtakDto? = null,
    val p8000Data: P8000AnmodningOmTilleggsinformasjon? = null,
    val p15000Data: P15000overfoeringAvPensjonssakerTilEessiDto? = null,
    val p2xxxMeldingOmPensjonDto: P2xxxMeldingOmPensjonDto? = null,
)