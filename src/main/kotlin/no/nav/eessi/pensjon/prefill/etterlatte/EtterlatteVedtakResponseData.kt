package no.nav.eessi.pensjon.prefill.etterlatte

import no.nav.eessi.pensjon.eux.model.sed.BeloepBrutto
import no.nav.eessi.pensjon.eux.model.sed.BeregningItem
import no.nav.eessi.pensjon.eux.model.sed.Periode
import no.nav.eessi.pensjon.eux.model.sed.VedtakItem


data class EtterlatteVedtakResponseData(
    private val vedtak: List<GjennyVedtak>
) {
    fun hentVedtakItems(): List<VedtakItem> {
        return vedtak.map { vedtak ->
            VedtakItem(
                virkningsdato = vedtak.virkningstidspunkt?.let { vedtak.virkningstidspunkt.toString() },
                resultat = vedtak.type?.value,
                type = "03", // Etterlatte pensjon
                beregning = vedtak.utbetaling?.map {
                    BeregningItem(
                        beloepBrutto = BeloepBrutto(beloep = it.beloep),
                        periode = Periode(
                            fom = it.fraOgMed.toString(),
                            tom = it.tilOgMed?.toString() ?: ""
                        )
                    )
                },
                iverksettelsesTidspunkt = vedtak.attestertTidspunkt?.toLocalDate(),
            )
        }
    }

    fun hentFørsteSakType(): String? = vedtak.firstOrNull { it.sakType != null }?.sakType
}