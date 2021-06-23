package no.nav.eessi.pensjon.utils

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Norwegian national identity number
 *
 * The Norwegian national identity number is an 11-digit personal identifier.
 * Everyone on the Norwegian National Registry has a national identity number.
 *
 * @see <a href="https://www.skatteetaten.no/person/folkeregister/fodsel-og-navnevalg/barn-fodt-i-norge/fodselsnummer">Skatteetaten om fødselsnummer</a>
 */
class Fodselsnummer private constructor(@JsonValue val value: String) {
    private val controlDigits1 = intArrayOf(3, 7, 6, 1, 8, 9, 4, 5, 2)
    private val controlDigits2 = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)

    init {
        require(Regex("\\d{11}").matches(value)) { "Ikke et gyldig fødselsnummer: $value" }
        require(!(isHNumber() || isFhNumber())) { "Impelentasjonen støtter ikke H-nummer og FH-nummer" }
        require(validateControlDigits()) { "Ugyldig kontrollnummer" }
    }

    companion object {
        @JvmStatic
        @JsonCreator
        fun fra(fnr: String?): Fodselsnummer? {
            return try {
                Fodselsnummer(fnr!!.replace(Regex("[^0-9]"), ""))
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Sjekker om fødselsnummeret er av typen "Hjelpenummer".
     *
     * H-nummer er et hjelpenummer, en virksomhetsintern, unik identifikasjon av en person som
     * ikke har fødselsnummer eller D-nummer eller hvor dette er ukjent.
     */
    private fun isHNumber(): Boolean = Character.getNumericValue(value[2]) >= 4

    /**
     * Sjekker om fødselsnummeret er av typen "Felles Nasjonalt Hjelpenummer".
     *
     * Brukes av helsevesenet i tilfeller hvor de har behov for unikt å identifisere pasienter
     * som ikke har et kjent fødselsnummer eller D-nummer.
     */
    private fun isFhNumber(): Boolean = value[0].toInt() in 8..9

    /**
     * Validate control digits.
     */
    private fun validateControlDigits(): Boolean {
        val ks1 = Character.getNumericValue(value[9])

        val c1 = mod(controlDigits1)
        if (c1 == 10 || c1 != ks1) {
            return false
        }

        val c2 = mod(controlDigits2)
        if (c2 == 10 || c2 != Character.getNumericValue(value[10])) {
            return false
        }

        return true
    }

    /**
     * Control Digits 1:
     *  k1 = 11 - ((3 × d1 + 7 × d2 + 6 × m1 + 1 × m2 + 8 × å1 + 9 × å2 + 4 × i1 + 5 × i2 + 2 × i3) mod 11)
     *
     * Control Digits 2
     *  k2 = 11 - ((5 × d1 + 4 × d2 + 3 × m1 + 2 × m2 + 7 × å1 + 6 × å2 + 5 × i1 + 4 × i2 + 3 × i3 + 2 × k1) mod 11)
     */
    private fun mod(arr: IntArray): Int {
        val sum = arr.withIndex()
                .sumBy { (i, m) -> m * Character.getNumericValue(value[i]) }

        val result = 11 - (sum % 11)
        return if (result == 11) 0 else result
    }

    override fun equals(other: Any?): Boolean {
        return this.value == (other as Fodselsnummer?)?.value
    }

    override fun hashCode(): Int = this.value.hashCode()

    override fun toString(): String = this.value
}