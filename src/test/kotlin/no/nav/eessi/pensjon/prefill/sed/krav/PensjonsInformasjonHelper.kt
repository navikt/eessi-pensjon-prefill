package no.nav.eessi.pensjon.prefill.sed.krav

import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import java.time.LocalDate
import java.util.*
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

//TODO: Laget som en enkel helper for generering av xml/java objekter for testing av bl.a. yteleser
object PensjonsInformasjonHelper {

    fun readJsonResponse(file: String): String {
        return javaClass.getResource(file)!!.readText()
    }
}