package no.nav.eessi.pensjon.utils

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

fun XMLGregorianCalendar.simpleFormat(): String {
    if (this.year > 2500) {
        return ""
    }
    println("XMLGregorianCalendar: date: ${this.toString()}")
    val date = SimpleDateFormat("yyyy-MM-dd").parse(this.toString())
    println("Date: ${date.toString()}")
    return SimpleDateFormat("yyyy-MM-dd").format(date)
}

fun Date.simpleFormat() = SimpleDateFormat("yyyy-MM-dd").format(this)

fun createXMLCalendarFromString(dateStr: String): XMLGregorianCalendar {
    println(dateStr)
    val date = SimpleDateFormat("yyyy-MM-dd").parse(dateStr)
    println(date)
    //val time = LocalDate.parse(dateStr)
    val gcal = GregorianCalendar()
    //gcal.timeInMillis = time.atStartOfDay (ZoneId.systemDefault()).toInstant().toEpochMilli()
     gcal.timeInMillis = date.time
    return DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal)
}

fun convertToXMLocal(time: LocalDate): XMLGregorianCalendar {
    val gcal = GregorianCalendar()
    gcal.setTime(Date.from(time.atStartOfDay(ZoneId.systemDefault()).toInstant()))
    val xgcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal)
    return xgcal
}
