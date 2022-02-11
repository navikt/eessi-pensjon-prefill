package no.nav.eessi.pensjon.services.pensjonsinformasjon

import com.fasterxml.jackson.annotation.JsonProperty

enum class Kravtype {
    REVURD,
    F_BH_MED_UTL,
    FORSTEG_BH,
    F_BH_BO_UTL,
    F_BH_KUN_UTL,

}

enum class Kravstatus {
    TIL_BEHANDLING,
    AVSL
}

//https://confluence.adeo.no/pages/viewpage.action?pageId=338181301
enum class KravArsak {
    GJNL_SKAL_VURD,
    TILST_DOD,
    NY_SOKNAD,
    `Ingen status`
    //GJENLEVENDERETT
    //GJENLEVENDETILLEGG
}

enum class EPSaktype {
    ALDER,
    UFOREP,
    BARNEP,
    GJENLEV;
}

enum class Sakstatus(val value: String) {
    INNV("INNV"),
    AVSL("AVSL"),
    INGEN_STATUS("Ingen status"),
    UKJENT("Ukjent"),
    TIL_BEHANDLING("TIL_BEHANDLING");

    companion object {
        fun byValue(value: String): Sakstatus {
            return values().first { sakstatus -> sakstatus.value == value }
        }
    }
}
