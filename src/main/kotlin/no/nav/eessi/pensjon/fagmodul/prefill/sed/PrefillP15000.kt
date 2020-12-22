package no.nav.eessi.pensjon.fagmodul.prefill.sed

import no.nav.eessi.pensjon.fagmodul.models.SEDType
import no.nav.eessi.pensjon.fagmodul.prefill.model.PersonData
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillSed
import no.nav.eessi.pensjon.fagmodul.sedmodel.Adresse
import no.nav.eessi.pensjon.fagmodul.sedmodel.Bruker
import no.nav.eessi.pensjon.fagmodul.sedmodel.Nav
import no.nav.eessi.pensjon.fagmodul.sedmodel.Pensjon
import no.nav.eessi.pensjon.fagmodul.sedmodel.Person
import no.nav.eessi.pensjon.fagmodul.sedmodel.PinItem
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.pensjon.v1.sak.V1Sak
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillP15000(private val prefillSed: PrefillSed) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP15000::class.java) }

        fun prefill(prefillData: PrefillDataModel, personData: PersonData, sak: V1Sak?): SED {
            val navsed = prefillSed.prefill(prefillData, personData)
            val eessielm = navsed.nav?.eessisak

            val gjenlevendeBruker: Bruker? = navsed.pensjon?.gjenlevende
            val forsikretBruker = navsed.nav?.bruker

            logger.debug("gjenlevendeBruker: ${gjenlevendeBruker?.person?.fornavn} PIN: ${gjenlevendeBruker?.person?.pin?.firstOrNull()?.identifikator} ")
            logger.debug("avDodBruker: ${forsikretBruker?.person?.fornavn} PIN: ${forsikretBruker?.person?.pin?.firstOrNull()?.identifikator} ")

            val forsikretPerson = forsikretBruker?.person
            val forsikretPersonPin = forsikretPerson?.pin?.firstOrNull()
            val adresse = forsikretBruker?.adresse

            val nav = Nav(
                eessisak = eessielm,
                bruker = Bruker(
                    person = Person(
                        etternavn = forsikretPerson?.etternavn,
                        fornavn = forsikretPerson?.fornavn,
                        foedselsdato = forsikretPerson?.foedselsdato,
                        kjoenn = forsikretPerson?.kjoenn,
                        pin = listOf(
                            PinItem(
                                identifikator = forsikretPersonPin?.identifikator,
                                land = forsikretPersonPin?.land
                            )
                        )
                    ),
                    adresse = Adresse(
                        gate = adresse?.gate,
                        by = adresse?.by,
                        land = adresse?.land
                    )
                )
            )
            val pensjon = Pensjon(gjenlevende = gjenlevendeBruker)

            return SED(SEDType.P15000.name, nav = nav, pensjon = pensjon)
        }

    }