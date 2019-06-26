package no.nav.eessi.eessifagmodul.utils

import java.text.SimpleDateFormat
import java.util.*
import javax.xml.datatype.XMLGregorianCalendar

fun XMLGregorianCalendar.simpleFormat()=
        SimpleDateFormat("yyyy-MM-dd").format(this.toGregorianCalendar().time)

fun Date.simpleFormat() =
        SimpleDateFormat("yyyy-MM-dd").format(this)
