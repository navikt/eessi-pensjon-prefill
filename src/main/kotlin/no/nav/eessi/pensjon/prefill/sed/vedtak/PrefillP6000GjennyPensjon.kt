package no.nav.eessi.pensjon.prefill.sed.vedtak

import no.nav.eessi.pensjon.eux.model.sed.BeloepBrutto
import no.nav.eessi.pensjon.eux.model.sed.BeregningItem
import no.nav.eessi.pensjon.eux.model.sed.Bruker
import no.nav.eessi.pensjon.eux.model.sed.KravtypeItem
import no.nav.eessi.pensjon.eux.model.sed.P6000Pensjon
import no.nav.eessi.pensjon.eux.model.sed.Periode
import no.nav.eessi.pensjon.eux.model.sed.Sak
import no.nav.eessi.pensjon.eux.model.sed.VedtakItem
import no.nav.eessi.pensjon.prefill.EtterlatteService
import no.nav.eessi.pensjon.prefill.EtterlatteService.EtterlatteVedtakResponseData

class PrefillP6000GjennyPensjon {

    fun prefillP6000GjennyPensjon(
        gjenlevende: Bruker?,
        etterlatteResponseData: EtterlatteVedtakResponseData?,
    ): P6000Pensjon? {
        if (etterlatteResponseData?.vedtak?.isEmpty() == true) return null
        val gjennyVedtak = etterlatteResponseData?.vedtak?.firstOrNull { it.sakType != null }?.sakType
        return P6000Pensjon(
            gjenlevende = gjenlevende,
            sak = Sak(
                enkeltkrav = KravtypeItem(
                    krav = gjennyVedtak
                    )
                ),
            vedtak =hentVedtakItems(etterlatteResponseData?.vedtak),
        )
    }

    private fun hentVedtakItems(vedtak: List<EtterlatteService.GjennyVedtak>?): List<VedtakItem>? {
        return vedtak?.map { vedtak ->
            VedtakItem(
                virkningsdato = vedtak.virkningstidspunkt.toString(),
                type = vedtak.type?.value,
                beregning = vedtak.utbetaling?.map {
                    BeregningItem(
                        beloepBrutto = BeloepBrutto(beloep = it.beloep),
                        periode = Periode(
                            fom = it.fraOgMed.toString(),
                            tom = it.tilOgMed.toString()
                        )
                    )
                }
            )
        }
    }
}


