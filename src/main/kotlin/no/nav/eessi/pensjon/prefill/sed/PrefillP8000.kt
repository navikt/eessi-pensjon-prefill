package no.nav.eessi.pensjon.prefill.sed

import no.nav.eessi.pensjon.eux.model.sed.*
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P8000AnmodningOmTilleggsinformasjon
import no.nav.eessi.pensjon.prefill.person.PrefillSed
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.api.ReferanseTilPerson
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillP8000(private val prefillSed: PrefillSed) {

    private enum class PersonenRolle(val value: String) {
        SOEKER_ETTERRLATTEPENSJON("01")
    }

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP8000::class.java) }

    fun prefill(prefillData: PrefillDataModel, personData: PersonDataCollection, sak: P8000AnmodningOmTilleggsinformasjon?): P8000 {
        val navsed = prefillSed.prefill(prefillData, personData)
        val eessielm = navsed.nav?.eessisak
        val gjenlevendeBruker: Bruker? = navsed.pensjon?.gjenlevende
        val avDodBruker = navsed.nav?.bruker
        logger.debug("gjenlevendeBruker: ${gjenlevendeBruker?.person?.fornavn} PIN: ${gjenlevendeBruker?.person?.pin?.firstOrNull()?.identifikator} ")
        logger.debug("avDodBruker: ${avDodBruker?.person?.fornavn} PIN: ${avDodBruker?.person?.pin?.firstOrNull()?.identifikator} ")

        val kravhistorikkGjenlev = sak?.harKravhistorikkGjenlevende

        logger.debug("*** SAK: ${sak?.sakType}, referanseTilPerson: ${prefillData.refTilPerson}, gjenlevende: ${gjenlevendeBruker!= null} ***")
        return if (prefillData.refTilPerson == ReferanseTilPerson.SOKER && sak?.sakType in listOf(EessiFellesDto.EessiSakType.ALDER, EessiFellesDto.EessiSakType.UFOREP) && gjenlevendeBruker != null) {
            logger.info("Prefill P8000 forenklet preutfylling for gjenlevende uten avdød, Ferdig.")
            sedP8000(eessielm, gjenlevendeBruker.person, gjenlevendeBruker.adresse, prefillData, null)
        } else if (prefillData.refTilPerson == ReferanseTilPerson.SOKER && sak?.sakType in listOf(EessiFellesDto.EessiSakType.ALDER, EessiFellesDto.EessiSakType.UFOREP) && kravhistorikkGjenlev != null) {
            logger.info("Prefill P8000 forenklet preutfylling for gjenlevende med revurdering uten avdød, Ferdig.")
            sedP8000(eessielm, gjenlevendeBruker?.person, gjenlevendeBruker?.adresse, prefillData, null)
        } else {
            logger.info("Prefill P8000 forenklet preutfylling med gjenlevende, Ferdig.")
            sedP8000(eessielm, avDodBruker?.person, avDodBruker?.adresse,  prefillData, utfyllAnnenperson(gjenlevendeBruker))
        }
    }

    private fun sedP8000(eessielm: List<EessisakItem>?, forsikretPerson: Person?, adresse: Adresse?, prefillData: PrefillDataModel, annenPerson: Bruker?): P8000 {
        logger.info("forsikretPerson: ${forsikretPerson != null} annenPerson: ${annenPerson != null}"  )
        return P8000(
                nav = Nav(
                        eessisak = eessielm,
                        bruker = Bruker(
                                person = Person(
                                        etternavn = forsikretPerson?.etternavn,
                                        fornavn = forsikretPerson?.fornavn,
                                        foedselsdato = forsikretPerson?.foedselsdato,
                                        kjoenn = forsikretPerson?.kjoenn,
                                        pin = forsikretPerson?.pin,
                                    kontakt = forsikretPerson?.kontakt),
                                adresse = Adresse(
                                    postnummer = adresse?.postnummer,
                                    gate = adresse?.gate,
                                    by = adresse?.by,
                                    land = adresse?.land,
                                    region = adresse?.region,
                                    bygning = adresse?.bygning
                                )
                        ),
                        annenperson = utfyllAnnenperson(annenPerson)
                ),
                p8000Pensjon = utfyllReferanseTilPerson(prefillData)
        )

    }

    private fun utfyllReferanseTilPerson(prefillData: PrefillDataModel): P8000Pensjon? {
        val refTilperson = prefillData.refTilPerson ?: return null
        return P8000Pensjon(anmodning = AnmodningOmTilleggsInfo(referanseTilPerson = refTilperson.verdi))
    }

    private fun utfyllAnnenperson(gjenlevende: Bruker?): Bruker? {
        if (gjenlevende == null) return null
        gjenlevende.person?.rolle = PersonenRolle.SOEKER_ETTERRLATTEPENSJON.value
        return gjenlevende
    }
}