package no.nav.eessi.eessifagmodul.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * fra stash...
 */
class NavFodselsnummer(private val fodselsnummer: String) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(NavFodselsnummer::class.java) }

    private fun getValue(): String {
        return fodselsnummer
    }

    private fun getDayInMonth(): String {
        return parseDNumber(getValue()).substring(0, 2)
    }

    private fun getMonth(): String {
        return parseDNumber(getValue()).substring(2, 4)
    }

    private fun get2DigitBirthYear(): String {
        return getValue().substring(4, 6)
    }

    fun get4DigitBirthYear(): String {
        return getCentury() + get2DigitBirthYear()
    }

    private fun getCentury(): String {
        val individnummerInt = Integer.parseInt(getIndividnummer())
        val birthYear = Integer.parseInt(get2DigitBirthYear())

        logger.debug("IndividnummerInt: $individnummerInt  BirthYear: $birthYear ")

        return when {
            (individnummerInt <= 499) -> "19"
            (individnummerInt >= 900 && birthYear > 39) -> "19"
            (individnummerInt >= 500 && birthYear < 40) -> "20"
            (individnummerInt in 500..749 && birthYear > 54) -> "18"
            else -> throw IllegalArgumentException("Ingen gyldig årstall funnet")
        }
    }

    fun getBirthDate(): LocalDate {
        return LocalDate.of(get4DigitBirthYear().toInt(), getMonth().toInt(), getDayInMonth().toInt())
    }


    fun getValidPentionAge(): Boolean {
        val validAge = 67
        val nowDate = LocalDate.now()
        val resultAge = ChronoUnit.YEARS.between(getBirthDate(), nowDate).toInt()
        return (resultAge >= validAge)
    }

    fun getAge(): Int {
        logger.debug("BirthDate: ${getBirthDate()}  now: ${LocalDate.now()}")
        return ChronoUnit.YEARS.between(getBirthDate(), LocalDate.now()).toInt()
    }

    private fun getIndividnummer(): String {
        return getValue().substring(6, 9)
    }

    private fun parseDNumber(fodselsnummer: String): String {
        return if (!isDNumber(fodselsnummer)) {
            fodselsnummer
        } else {
            "" + (getFirstDigit(fodselsnummer) - 4) + fodselsnummer.substring(1)
        }
    }

    private fun isDNumber(fodselsnummer: String): Boolean {
        try {
            val firstDigit = getFirstDigit(fodselsnummer)
            if (firstDigit in 4..7) {
                return true
            }
        } catch (e: IllegalArgumentException) {
            return false
        }
        return false
    }

    private fun getFirstDigit(fodselsnummer: String): Int {
        return Integer.parseInt(fodselsnummer.substring(0, 1))
    }

}
