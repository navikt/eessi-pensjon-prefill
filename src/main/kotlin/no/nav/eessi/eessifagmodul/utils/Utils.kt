package no.nav.eessi.eessifagmodul.utils

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

fun <K, V> Map<K, V>.reversed() = HashMap<V, K>().also { newMap ->
    entries.forEach { newMap.put(it.value, it.key) }
}

fun XMLGregorianCalendar.simpleFormat(): String {
    //rinaformat dd-MM-YYYY
    return SimpleDateFormat("yyyy-MM-dd").format(this.toGregorianCalendar().time)
}

fun createXMLCalendarFromString(dateStr: String): XMLGregorianCalendar {
    val time = LocalDate.parse(dateStr)
    val gcal = GregorianCalendar()
    gcal.timeInMillis = time.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    return DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal)
}


fun Date.simpleFormat(): String {
    return SimpleDateFormat("yyyy-MM-dd").format(this)
}
