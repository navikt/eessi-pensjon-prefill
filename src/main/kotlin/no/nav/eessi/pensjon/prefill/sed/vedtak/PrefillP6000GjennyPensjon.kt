package no.nav.eessi.pensjon.prefill.sed.vedtak

import no.nav.eessi.pensjon.eux.model.sed.Bruker
import no.nav.eessi.pensjon.eux.model.sed.P6000Pensjon
import no.nav.eessi.pensjon.eux.model.sed.VedtakItem
import no.nav.eessi.pensjon.prefill.EtterlatteService

class PrefillP6000GjennyPensjon {

    fun prefillP6000GjennyPensjon(
        gjenlevende: Bruker?,
        listeMedVedtak: List<VedtakItem>?,
    ): P6000Pensjon? {
        if (listeMedVedtak.isNullOrEmpty()) return null
        return P6000Pensjon(
            gjenlevende = gjenlevende,
            vedtak = listeMedVedtak
        )
    }

}
