package no.nav.eessi.pensjon.fagmodul.prefill.sed

import no.nav.eessi.pensjon.fagmodul.models.SEDType
import no.nav.eessi.pensjon.fagmodul.prefill.model.PersonData
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillSed
import no.nav.eessi.pensjon.fagmodul.sedmodel.Adresse
import no.nav.eessi.pensjon.fagmodul.sedmodel.AnmodningOmTilleggsInfo
import no.nav.eessi.pensjon.fagmodul.sedmodel.Bruker
import no.nav.eessi.pensjon.fagmodul.sedmodel.EessisakItem
import no.nav.eessi.pensjon.fagmodul.sedmodel.Nav
import no.nav.eessi.pensjon.fagmodul.sedmodel.Pensjon
import no.nav.eessi.pensjon.fagmodul.sedmodel.Person
import no.nav.eessi.pensjon.fagmodul.sedmodel.PinItem
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillP8000(private val prefillSed: PrefillSed)  {

    private enum class PersonenRolle(val value: String) {
        SOEKER_ETTERRLATTEPENSJON("01")
    }
    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP8000::class.java) }

    fun prefill(prefillData: PrefillDataModel, personData: PersonData): SED {
        val navsed = prefillSed.prefill(prefillData, personData)
        logger.debug("Tilpasser P8000 forenklet preutfylling")
        val person = navsed.nav?.bruker?.person
        val adresse = navsed.nav?.bruker?.adresse
        val eessielm = navsed.nav?.eessisak?.firstOrNull()
        val perspin = navsed.nav?.bruker?.person?.pin?.firstOrNull()
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
                pensjon = utfyllReferanseTilPerson(prefillData)
        )
        logger.info("Prefill P8000 forenklet preutfylling, Ferdig.")

        prefillData.sed = p8000
        return p8000
    }

    private fun utfyllReferanseTilPerson(prefillData: PrefillDataModel): Pensjon? {
        val refTilperson = prefillData.refTilPerson ?: return null
        return Pensjon(anmodning = AnmodningOmTilleggsInfo(referanseTilPerson = refTilperson.verdi ))
    }

    private fun utfyllAnnenperson(gjenlevende: Bruker?): Bruker? {
        if (gjenlevende == null) return null
        gjenlevende.person?.rolle = PersonenRolle.SOEKER_ETTERRLATTEPENSJON.value
        return gjenlevende
    }
}