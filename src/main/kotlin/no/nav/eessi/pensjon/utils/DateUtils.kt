package no.nav.eessi.pensjon.utils

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

fun XMLGregorianCalendar.simpleFormat() =
    if (this.year > 2500) "" else SimpleDateFormat("yyyy-MM-dd").format(this.toGregorianCalendar().time)


fun Date.simpleFormat() =
        SimpleDateFormat("yyyy-MM-dd").format(this)

fun createXMLCalendarFromString(dateStr: String): XMLGregorianCalendar {
    val time = LocalDate.parse(dateStr)
    val gcal = GregorianCalendar()
    gcal.timeInMillis = time.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    return DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal)
}