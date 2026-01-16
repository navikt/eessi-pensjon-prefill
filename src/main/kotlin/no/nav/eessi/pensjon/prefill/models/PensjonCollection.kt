package no.nav.eessi.pensjon.prefill.models

import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.prefill.models.pensjon.P6000MeldingOmVedtakDto
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.sak.V1Sak
import no.nav.pensjon.v1.vedtak.V1Vedtak

data class PensjonCollection (
    val pensjoninformasjon: Pensjonsinformasjon? = null,
    val vedtak: V1Vedtak? = null,
    val sak: V1Sak? = null,
    val sedType: SedType,
    val p6000Data: P6000MeldingOmVedtakDto? = null,
)