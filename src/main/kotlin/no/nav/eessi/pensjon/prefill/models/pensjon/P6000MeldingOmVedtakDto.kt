package no.nav.eessi.pensjon.prefill.models.pensjon

import no.nav.eessi.pensjon.prefill.models.VedtakInterface
import java.time.LocalDate

data class P6000MeldingOmVedtakDto(
    val avdod: Avdod?, //P6000
    val sakType: EessiFellesDto.EessiSakType, //P2000, P6000, sikkert flere (sakstype).
    val trygdeavtale: Trygdeavtale?, //P6000
    val trygdetidListe: List<Trygdetid>, // P6000
    val vedtak: Vedtak, // P2000, P2200, P6000
    val vilkarsvurderingListe: List<Vilkarsvurdering>, // P6000
    val ytelsePerMaanedListe: List<YtelsePerMaaned>, //P2000, P2200, P6000
) {

    data class Avdod(
        val avdod: String?,
        val avdodBoddArbeidetUtland: Boolean?, // P6000
        val avdodFarBoddArbeidetUtland: Boolean?, // P6000
        val avdodMorBoddArbeidetUtland: Boolean?, // P6000
    )

//    data class SakAlder(
//        val sakType: EessiFellesDto.EessiSakType, // P2000 blant mange andre
//    )

    data class Trygdeavtale(
        val erArt10BruktGP: Boolean?, // P6000
        val erArt10BruktTP: Boolean?, // P6000
    )

    data class Vilkarsvurdering(
        val fom: LocalDate, // P6000
        val vilkarsvurderingUforetrygd: VilkarsvurderingUforetrygd?, // P6000
        val resultatHovedytelse: String, // P6000
        val harResultatGjenlevendetillegg: Boolean, // P6000
        val avslagHovedytelse: String?, // P6000
    )

    data class YtelsePerMaaned(
        override val fom: LocalDate, // P2000, P2200, P6000
        val tom: LocalDate?, // P6000
        val mottarMinstePensjonsniva: Boolean, // P6000
        val vinnendeBeregningsmetode: String, //P6000
        override val belop: Int, // P2000, P2200, P6000
        override val ytelseskomponent: List<Ytelseskomponent>, //P6000
    ) : YtelsePerMndBase(fom, belop,ytelseskomponent)

    data class Trygdetid(
        val fom: LocalDate, //P6000
        val tom: LocalDate, //P6000
    )

    data class Vedtak(
        val virkningstidspunkt: LocalDate, // P6000
        val kravGjelder: String, // P6000
        val hovedytelseTrukket: Boolean, // P6000
        val boddArbeidetUtland: Boolean?, // P2000, P2200, P6000
        val datoFattetVedtak: LocalDate?, // P6000
    ) : VedtakInterface

    data class VilkarsvurderingUforetrygd(
        val alder: String?, // P6000
        val hensiktsmessigBehandling: String?, // P6000
        val hensiktsmessigArbeidsrettedeTiltak: String?, // P6000
        val nedsattInntektsevne: String?, // P6000
        val unntakForutgaendeMedlemskap: String?, // P6000
    )

}
