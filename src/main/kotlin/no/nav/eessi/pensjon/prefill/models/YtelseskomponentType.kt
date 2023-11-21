package no.nav.eessi.pensjon.prefill.models

/**
 * Hentet ytelseskomponentType fra YtelseKomponentTypeCode.java (PESYS)
 */
enum class YtelseskomponentType {

        GAP,                    //Garantipensjon
        GAT,                    //Garantitillegg
        GP,                     //Grunnpensjon
        IP,                     //Inntektspensjon
        ST,                     //Særtillegg
        PT,                     //Pensjonstillegg
        TP,                     //Tilleggspensjon
        MIN_NIVA_TILL_INDV,     // Minstenivåtillegg individuelt
        MIN_NIVA_TILL_PPAR,     // Minstenivåtillegg pensjonistpar
        AP_GJT_KAP19,           // Gjenlevendetillegg kapittel 19 (P6000)
        ;
}