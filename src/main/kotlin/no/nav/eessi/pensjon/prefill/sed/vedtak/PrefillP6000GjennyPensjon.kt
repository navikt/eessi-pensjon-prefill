package no.nav.eessi.pensjon.prefill.sed.vedtak

import no.nav.eessi.pensjon.eux.model.sed.*
import no.nav.eessi.pensjon.prefill.EtterlatteService
import no.nav.eessi.pensjon.prefill.EtterlatteService.EtterlatteVedtakResponseData
import no.nav.eessi.pensjon.prefill.EtterlatteService.GjennyVedtak
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon

class PrefillP6000GjennyPensjon {

    fun prefillP6000GjennyPensjon(
        gjenlevende: Bruker?,
        etterlatteResponseData: EtterlatteVedtakResponseData?,
        eessiInformasjon: EessiInformasjon,
    ): P6000Pensjon? {
        if (etterlatteResponseData?.vedtak?.isEmpty() == true) return null
        val gjennyVedtak = etterlatteResponseData?.vedtak?.firstOrNull { it.sakType != null }?.sakType
        return P6000Pensjon(
            gjenlevende = gjenlevende,
            sak = Sak(
                kravtype = listOf(KravtypeItem(
                    krav = gjennyVedtak,
                    datoFrist = "six weeks from the date the decision is received"
                    )
                )),
            vedtak = hentVedtakItems(etterlatteResponseData?.vedtak),
            tilleggsinformasjon = Tilleggsinformasjon(andreinstitusjoner = listOf(eessiInformasjon.asAndreinstitusjonerItem()))
        )
    }

    private fun hentVedtakItems(vedtak: List<GjennyVedtak>?): List<VedtakItem>? {
        return vedtak?.map { vedtak ->
            VedtakItem(
                virkningsdato = vedtak.virkningstidspunkt.toString(),
                resultat = vedtak.type?.value,
                type = "03", //Etterlatte pensjon, //TODO: burde legges inn som en enum i ep-eux
                beregning = vedtak.utbetaling?.map {
                    BeregningItem(
                        beloepBrutto = BeloepBrutto(beloep = it.beloep),
                        periode = Periode(
                            fom = it.fraOgMed.toString(),
                            tom = if (it.tilOgMed == null) "" else it.tilOgMed.toString()
                        )
                    )
                }
            )
        }
    }
}


