package no.nav.eessi.pensjon.fagmodul.prefill.sed

import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillSed
import no.nav.eessi.pensjon.fagmodul.sedmodel.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillP7000(private val prefillSed: PrefillSed) : Prefill {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP7000::class.java) }

    override fun prefill(prefillData: PrefillDataModel): SED {

        val sed = prefillSed.prefill(prefillData)
        logger.debug("Tilpasser P7000 forenklet preutfylling")

        val person = sed.nav?.bruker?.person
        val perspin = sed.nav?.bruker?.person?.pin?.get(0)
        val eessielm = sed.nav?.eessisak?.get(0)

        val p7000 = SED(
                sed = "P7000",
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
                        //mappe om etternavn til mappingfeil
                        ektefelle = Ektefelle(person = Person(etternavn = sed.nav?.bruker?.person?.etternavn))
                ),
                //mappe om kjoenn for mappingfeil
                pensjon = Pensjon(
                        //TODO trenger vi denne lenger? er mapping ok eller fortsatt feil?
                        bruker = Bruker(person = Person(kjoenn = sed.nav?.bruker?.person?.kjoenn)),
                        gjenlevende = sed.pensjon?.gjenlevende
                )
        )

        logger.debug("Tilpasser P7000 forenklet preutfylling, Ferdig.")

        prefillData.sed = p7000
        return prefillData.sed
    }
}
