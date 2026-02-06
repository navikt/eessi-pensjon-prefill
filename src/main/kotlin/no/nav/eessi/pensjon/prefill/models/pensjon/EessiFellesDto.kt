package no.nav.eessi.pensjon.prefill.models.pensjon

import java.time.LocalDate

object EessiFellesDto { // brukes kun for namespace.
    enum class EessiSakStatus { TIL_BEHANDLING, INGEN_STATUS, OPPRETTET, UKJENT, TRUKKET, AVBRUTT, AVSL, INNV, OPPHOR, VELG, VETIKKE, ANNET; }

    enum class EessiKravGjelder {
        F_BH_BO_UTL, F_BH_MED_UTL, F_BH_KUN_UTL, FORSTEG_BH, REVURD,  SLUTT_BH_UTL, ANNET
    }
//    enum class EessiKravGjelder { F_BH_BO_UTL, F_BH_MED_UTL, F_BH_KUN_UTL, FORSTEG_BH, SLUTT_BH_UTL, REVURD, ANNET }

    enum class EessiKravAarsak { GJNL_SKAL_VURD, TILST_DOD, NY_SOKNAD, ANNET }

    data class PensjonSakDto(val sakId: String, val sakType: EessiSakType, val sakStatus: EessiSakStatus)

    data class EessiAvdodDto(val avdod: String?, val avdodMor: String?, val avdodFar: String?)

    data class EessiUfoeretidspunktDto(val uforetidspunkt: LocalDate?, val virkningstidspunkt: LocalDate?)

    enum class EessiSakType {
        AFP, AFP_PRIVAT, ALDER, BARNEP, FAM_PL, GAM_YRK, GENRL, GJENLEV, GRBL, KRIGSP, OMSORG, UFOREP
    }
}
