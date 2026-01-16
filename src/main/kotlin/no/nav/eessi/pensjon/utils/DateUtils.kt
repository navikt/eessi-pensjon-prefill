package no.nav.eessi.pensjon.utils

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

private const val sdfPattern = "yyyy-MM-dd"

fun XMLGregorianCalendar.simpleFormat(): String {
    if (this.year > 2500) {
        return ""
    }
    val date = SimpleDateFormat(sdfPattern).parse(this.toString())
    return SimpleDateFormat(sdfPattern).format(date)
}

fun Date.simpleFormat(): String = SimpleDateFormat(sdfPattern).format(this)

fun LocalDate.simpleFormat(): String = SimpleDateFormat(sdfPattern).format(this)


fun createXMLCalendarFromString(dateStr: String): XMLGregorianCalendar {
    val date = SimpleDateFormat(sdfPattern).parse(dateStr)
    val gcal = GregorianCalendar()
    gcal.timeInMillis = date.time
    return DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal)
}

fun convertToXMLocal(time: LocalDate): XMLGregorianCalendar {
    val gcal = GregorianCalendar()
    gcal.time = Date.from(time.atStartOfDay(ZoneId.systemDefault()).toInstant())
    return DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal)
}

fun toLocalDate(dateTime: String): LocalDate {
    //offse & zone
    if(dateTime.contains("+")){
        if(dateTime.substringAfter("+").contains(":")){
            return ZonedDateTime.parse(dateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .withZoneSameInstant(ZoneId.of("Europe/Paris")).toLocalDateTime().toLocalDate()
        }
        return ZonedDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"))
            .withZoneSameInstant(ZoneId.of("Europe/Paris"))
            .toLocalDateTime().toLocalDate()
    }
    if(isValidLocalDate(dateTime)){
        return LocalDate.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }
    return LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")).toLocalDate()
}

private fun isValidLocalDate(dateStr: String?): Boolean {
    try {
        LocalDate.parse(dateStr,  DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    } catch (e: DateTimeParseException) {
        return false
    }
    return true
}
