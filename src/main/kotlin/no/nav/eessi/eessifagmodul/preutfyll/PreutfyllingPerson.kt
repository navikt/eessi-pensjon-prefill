package no.nav.eessi.eessifagmodul.preutfyll

import no.nav.eessi.eessifagmodul.models.*

class PreutfyllingPerson {

    fun preuytfyllingPerson(sed: SED): SED {

        //min krav for P6000
        sed.nav = Nav(
                bruker = Bruker(
                        person = Person(
                                fornavn = "Fornavn",
                                kjoenn = "f",
                                foedselsdato = "1957-12-01",
                                etternavn = "Etternavn"
                        )
                )
        )
        return sed

    }

}