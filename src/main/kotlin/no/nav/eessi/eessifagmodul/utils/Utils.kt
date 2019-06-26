package no.nav.eessi.eessifagmodul.utils

import java.text.SimpleDateFormat
import java.util.*
import javax.xml.datatype.XMLGregorianCalendar

fun XMLGregorianCalendar.simpleFormat(): String {
    //rinaformat dd-MM-YYYY
    return SimpleDateFormat("yyyy-MM-dd").format(this.toGregorianCalendar().time)
}

fun Date.simpleFormat(): String {
    return SimpleDateFormat("yyyy-MM-dd").format(this)
}
