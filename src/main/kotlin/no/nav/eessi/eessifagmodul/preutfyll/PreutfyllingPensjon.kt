package no.nav.eessi.eessifagmodul.preutfyll

import no.nav.eessi.eessifagmodul.models.Gjenlevende
import no.nav.eessi.eessifagmodul.models.Pensjon
import no.nav.eessi.eessifagmodul.models.Person
import no.nav.eessi.eessifagmodul.models.SED

class PreutfyllingPensjon{

    fun preutfullingPensjon(sed: SED): SED {

        //min krav for P6000
        sed.pensjon = Pensjon(
                gjenlevende = Gjenlevende(
                        person = Person(
                                fornavn = "Fornavn",
                                kjoenn = "f",
                                foedselsdato = "1967-12-01",
                                etternavn = "Etternavn"
                        )
                )
        )

        return sed
    }

}