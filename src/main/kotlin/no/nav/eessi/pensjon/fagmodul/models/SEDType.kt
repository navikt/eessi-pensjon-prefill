package no.nav.eessi.pensjon.fagmodul.models

enum class SEDType {
    P2000,
    P2100,
    P2200,
    P3000,
    P4000,
    P6000,
    P5000,
    P7000,
    P8000,
    P9000,
    P10000,
    P14000,
    P15000,
    X005,
    H020,
    H021,
    H070,
    H120,
    H121,
    P12000,
    P13000,
    P1000,
    P1100,
    P11000,
    P3000_FR,
    P3000_IE,
    P3000_HU,
    P3000_LT,
    P3000_IS,
    P3000_UK,
    P3000_NO,
    P3000_IT,
    P3000_SI,
    P3000_MT,
    P3000_BE,
    P3000_EE,
    P3000_AT,
    P3000_BG,
    P3000_LI,
    P3000_DK,
    P3000_SE,
    P3000_FI,
    P3000_PL,
    P3000_DE,
    P3000_ES,
    P3000_PT,
    P3000_LV,
    P3000_SK,
    P3000_NL;

    companion object {
        @JvmStatic
        fun isValidSEDType(input: String): Boolean {
            return try {
                valueOf(input)
                true
            } catch (ia: IllegalArgumentException) {
                false
            }
        }
    }
}