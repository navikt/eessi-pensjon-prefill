package no.nav.eessi.pensjon.prefill.models.pensjon

import no.nav.eessi.pensjon.prefill.models.SakInterface
import no.nav.eessi.pensjon.prefill.models.VedtakInterface
import java.time.LocalDate

data class P2xxxMeldingOmPensjonDto(
    val vedtak: Vedtak?,
    val sak: Sak?,
) {
    data class Sak(
        val sakType: EessiFellesDto.EessiSakType,
        val forsteVirkningstidspunkt: LocalDate?,
        val kravHistorikk: List<KravHistorikk>,
        val ytelsePerMaaned: List<YtelsePerMaaned>,
        val status: EessiFellesDto.EessiSakStatus,
    ) : SakInterface

    data class YtelsePerMaaned(
        override val fom: LocalDate? = null, // P2000, P2200, P6000
        override val belop: Int? = null, // P2000, P2200, P6000
        override val ytelseskomponentListe: List<Ytelseskomponent>? = null, //P6000
    ) : YtelsePerMndBase(fom, belop,ytelseskomponentListe)

    data class Vedtak(
        val boddArbeidetUtland: Boolean? = null,
    ) : VedtakInterface

    // TODO: Valdiere senere
    data class KravHistorikk(
        val kravId: String? = null,
        val kravType: EessiFellesDto.EessiKravGjelder? = null,
        val kravStatus: EessiFellesDto.EessiSakStatus? = null,
        val kravArsak: EessiFellesDto.EessiKravAarsak? = null,
        val mottattDato: LocalDate? = null,
        val virkningstidspunkt: LocalDate? = null,
    )
}
