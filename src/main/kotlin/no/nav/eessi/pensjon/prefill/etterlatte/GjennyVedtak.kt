package no.nav.eessi.pensjon.prefill.etterlatte

import java.time.LocalDate
import java.time.LocalDateTime

data class GjennyVedtak(
    val sakId: Int,
    val sakType: String?,
    val virkningstidspunkt: LocalDate?,
    val type: VedtakStatus?,
    val utbetaling: List<GjennyUtbetaling>?,
    val iverksettelsesTidspunkt: LocalDateTime?,
)

data class GjennyUtbetaling(
    val fraOgMed : LocalDate?,
    val tilOgMed : LocalDate?,
    val beloep : String?,
)

enum class VedtakStatus(val value: String) {
    INNVILGELSE("01"),
    AVSLAG("02"),
    ENDRING("03"),
    NY_BEREGNING_OMREGNING("03"),
    FORELOPIG_UTBETALING("04");

    companion object {
        @JvmStatic
        fun fra(value: String?): VedtakStatus? {
            return if (value == null) null
            else values().firstOrNull { it.value == value }
        }
    }
}