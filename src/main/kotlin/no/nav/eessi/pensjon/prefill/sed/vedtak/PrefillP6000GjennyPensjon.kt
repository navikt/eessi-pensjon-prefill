package no.nav.eessi.pensjon.prefill.sed.vedtak

import no.nav.eessi.pensjon.eux.model.sed.*
import no.nav.eessi.pensjon.prefill.etterlatte.EtterlatteVedtakResponseData
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.utils.toJson
import java.time.format.DateTimeFormatter

class PrefillP6000GjennyPensjon {

    fun prefillP6000GjennyPensjon(
        gjenlevende: Bruker?,
        etterlatteResponseData: EtterlatteVedtakResponseData?,
        eessiInformasjon: EessiInformasjon,
    ): P6000Pensjon? {
        if (etterlatteResponseData?.hentVedtakItems()?.isEmpty() == true) return null
        val dato = etterlatteResponseData?.hentVedtakItems()?.map { it.iverksettelsesTidspunkt }?.sortedByDescending { it }?.firstOrNull()

        return P6000Pensjon(
            gjenlevende = gjenlevende,
            sak = Sak(
                kravtype = listOf(KravtypeItem(
                    krav = etterlatteResponseData?.hentFørsteSakType(),
                    datoFrist = "six weeks from the date the decision is received"
                    )
                )),
            vedtak = etterlatteResponseData?.hentVedtakItems(),
            tilleggsinformasjon = Tilleggsinformasjon(
                dato = dato?.format(DateTimeFormatter.ISO_DATE),
                andreinstitusjoner = listOf(eessiInformasjon.asAndreinstitusjonerItem())
            )
        )
    }
}


