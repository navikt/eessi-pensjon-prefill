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
        override val ytelseskomponentListe: List<EessiFellesDto.Ytelseskomponent>? = null, //P6000
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

//enum class Kravstatus { TIL_BEHANDLING, AVSL }
//
//    enum class EessiSakStatus { TIL_BEHANDLING, INGEN_STATUS, OPPRETTET, UKJENT, TRUKKET, AVBRUTT, AVSL, INNV, OPPHOR, VELG, VETIKKE }
//
//    enum class EessiKravStatus { TIL_BEHANDLING, INGEN_STATUS, OPPRETTET, UKJENT, TRUKKET, AVBRUTT, AVSL, INNV, OPPHOR, VELG, VETIKKE }
//
//    enum class EessiKravGjelder { F_BH_BO_UTL, F_BH_MED_UTL, F_BH_KUN_UTL, FORSTEG_BH, SLUTT_BH_UTL, REVURD, ANNET }
//
//    enum class EessiKravAarsak { GJNL_SKAL_VURD, TILST_DOD, NY_SOKNAD, `Ingen status` }
//
//    data class PensjonSakDto(val sakId: String, val sakType: String, val sakStatus: EessiSakStatus)
//
//    data class EessiAvdodDto(val avdod: String?, val avdodMor: String?, val avdodFar: String?)
//
//    data class EessiUfoeretidspunktDto(val uforetidspunkt: LocalDate?, val virkningstidspunkt: LocalDate?)
//
//    enum class EessiSakType { AFP, AFP_PRIVAT, ALDER, BARNEP, FAM_PL, GAM_YRK, GENRL, GJENLEV, GRBL, KRIGSP, OMSORG, UFOREP }
