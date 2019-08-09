package no.nav.eessi.pensjon.utils

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

fun XMLGregorianCalendar.simpleFormat(): String {
    if (this.year > 2500) {
        return ""
    }
    val date = SimpleDateFormat("yyyy-MM-dd").parse(this.toString())
    return SimpleDateFormat("yyyy-MM-dd").format(date)
}

fun Date.simpleFormat() = SimpleDateFormat("yyyy-MM-dd").format(this)

fun createXMLCalendarFromString(dateStr: String): XMLGregorianCalendar {
    val date = SimpleDateFormat("yyyy-MM-dd").parse(dateStr)
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
