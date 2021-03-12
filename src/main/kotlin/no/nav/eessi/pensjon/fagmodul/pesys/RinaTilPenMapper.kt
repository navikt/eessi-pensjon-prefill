package no.nav.eessi.pensjon.fagmodul.pesys


object RinaTilPenMapper {
    //pensjon utatksgrad mapping fra P3000 til pesys verdi.
    fun parsePensjonsgrad(pensjonsgrad: String?): String? {
        return when (pensjonsgrad) {
            "01" -> "20"
            "02" -> "40"
            "03" -> "50"
            "04" -> "60"
            "05" -> "80"
            "06" -> "100"
            else -> null
        }
    }
}