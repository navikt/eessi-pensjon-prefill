package no.nav.eessi.pensjon.fagmodul.prefill.sed

import no.nav.eessi.pensjon.fagmodul.models.SEDType
import no.nav.eessi.pensjon.fagmodul.prefill.model.PersonData
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillSed
import no.nav.eessi.pensjon.fagmodul.sedmodel.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillP8000(private val prefillSed: PrefillSed)  {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP8000::class.java) }

    fun prefill(prefillData: PrefillDataModel, personData: PersonData): SED {

        val navsed = prefillSed.prefill(prefillData, personData)

        logger.debug("Tilpasser P8000 forenklet preutfylling")
        val person = navsed.nav?.bruker?.person
        val adresse = navsed.nav?.bruker?.adresse
        val eessielm = navsed.nav?.eessisak?.get(0)
        val perspin = navsed.nav?.bruker?.person?.pin?.get(0)
        val gjenlevende = navsed.pensjon?.gjenlevende

        val p8000 = SED(
                sed = SEDType.P8000.name,
                nav = Nav(
                        eessisak = listOf(EessisakItem(
                                land = eessielm?.land,
                                saksnummer = eessielm?.saksnummer
                        )),

                        bruker = Bruker(
                                person = Person(
                                        etternavn = person?.etternavn,
                                        fornavn = person?.fornavn,
                                        foedselsdato = person?.foedselsdato,
                                        kjoenn = person?.kjoenn,
                                        pin = listOf(
                                                PinItem(
                                                        identifikator = perspin?.identifikator,
                                                        land = perspin?.land
                                                )
                                        )
                                ),
                                adresse = Adresse(
                                        gate = adresse?.gate,
                                        by = adresse?.by,
                                        land = adresse?.land
                                )
                        ),
                        annenperson = utfyllAnnenperson(gjenlevende)
                ),
                pensjon = null
        )
        logger.info("Prefill P8000 forenklet preutfylling, Ferdig.")

        prefillData.sed = p8000

        return prefillData.sed
    }

    private fun utfyllAnnenperson(gjenlevende: Bruker?): Bruker? {
        if (gjenlevende == null) return null
        gjenlevende.person?.rolle = "01"
        return gjenlevende
    }
}