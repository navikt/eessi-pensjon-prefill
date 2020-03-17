package no.nav.eessi.pensjon.fagmodul.prefill.sed

import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillPerson
import no.nav.eessi.pensjon.fagmodul.sedmodel.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillP8000(private val prefillPerson: PrefillPerson) : Prefill<SED> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP8000::class.java) }

    override fun prefill(prefillData: PrefillDataModel): SED {

        val navsed = prefillPerson.prefill(prefillData)

        logger.debug("Tilpasser P8000 forenklet preutfylling")
        val person = navsed.nav?.bruker?.person
        val eessielm = navsed.nav?.eessisak?.get(0)
        val perspin = navsed.nav?.bruker?.person?.pin?.get(0)
        val gjenlevende = navsed.pensjon?.gjenlevende

        val p8000 = SED(
                sed = "P8000",
                nav = Nav(
                        eessisak = listOf(EessisakItem(
                                land = eessielm?.land,
                                saksnummer = eessielm?.saksnummer,
                                institusjonsid = eessielm?.institusjonsid,
                                institusjonsnavn = eessielm?.institusjonsnavn
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
                                                        land = perspin?.land,
                                                        institusjon = Institusjon(
                                                                institusjonsid = perspin?.institusjon?.institusjonsid,
                                                                institusjonsnavn = perspin?.institusjon?.institusjonsnavn
                                                        )

                                                )
                                        )
                                )
                        ),
                        annenperson = utfyllAnnenperson(gjenlevende)
                ),
                pensjon = null
        )

        logger.debug("Tilpasser P8000 forenklet preutfylling, Ferdig.")

        prefillData.sed = p8000

        return prefillData.sed
    }

    private fun utfyllAnnenperson(bruker: Bruker?): Bruker? {
        if (bruker == null) return null
        bruker.person?.rolle = "01"
        return bruker
    }
}