package no.nav.eessi.pensjon.fagmodul.personoppslag

import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bostedsadresse
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Foedselsdato
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kjoenn
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kjoennstyper
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kommune
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Landkoder
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personstatus
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personstatuser
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Postnummer
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Statsborgerskap
import javax.xml.datatype.DatatypeFactory

object BrukerMock {
    internal fun createWith(landkoder: Boolean = true, fornavn: String = "Test", etternavn: String = "Testesen", fnr: String = "3123", erDod: Boolean? = false) :Bruker? {
            val fdatoaar = if  (erDod == null || erDod == false ) 1988 else 1921
            return Bruker()
            .withPersonnavn(Personnavn()
                    .withEtternavn(etternavn)
                    .withFornavn(fornavn)
                    .withSammensattNavn("$fornavn $etternavn"))
            .withKjoenn(Kjoenn().withKjoenn(Kjoennstyper().withValue("M")))
            .withPersonstatus(Personstatus().withPersonstatus(Personstatuser().withValue( if (erDod == null || erDod == false ) "" else "DØD")))
            .withStatsborgerskap(Statsborgerskap().withLand(Landkoder().withValue("NOR")))
            .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent(fnr)))
            .withFoedselsdato(Foedselsdato().withFoedselsdato(DatatypeFactory.newInstance().newXMLGregorianCalendarDate(fdatoaar,7, 12,10)))
            .withBostedsadresse(Bostedsadresse()
                    .withStrukturertAdresse(Gateadresse()
                            .withGatenavn("Oppoverbakken")
                            .withHusnummer(66)
                            .withPoststed(Postnummer().withValue("1920"))
                            .withLandkode(when(landkoder){
                                true -> Landkoder().withValue("NOR")
                                else -> null
                            })))
            .withGeografiskTilknytning(when(landkoder) {
                true -> Kommune().withGeografiskTilknytning("026123")
                else -> null
            })
    }
}