package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.prefill.nav.PrefillPerson
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillP7000(private val prefillPerson: PrefillPerson) : Prefill<SED> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP7000::class.java) }

    override fun prefill(prefillData: PrefillDataModel): SED {

        val p7000 = SED.create("P7000")

        val navsed = prefillPerson.prefill(prefillData)

        logger.debug("Tilpasser P7000 forenklet preutfylling")
        val person = navsed.nav?.bruker?.person
        val eessielm = navsed.nav?.eessisak?.get(0)
        val perspin = navsed.nav?.bruker?.person?.pin?.get(0)

        p7000.nav = Nav(
                eessisak = listOf(EessisakItem(
                        land = eessielm?.land,
                        saksnummer = eessielm?.saksnummer
                )),

                //bruker = Bruker(person = navsed.nav?.bruker?.person)
                bruker = Bruker(
                        person = Person(
                                etternavn = person?.etternavn,
                                fornavn = person?.fornavn,
                                foedselsdato = person?.foedselsdato,
                                kjoenn = person?.kjoenn
                        )
                )
        )
        p7000.nav?.bruker?.person?.pin = listOf(
                PinItem(
                        identifikator = perspin?.identifikator,
                        land = perspin?.land,
                        institusjon = Institusjon(
                                institusjonsid = perspin?.institusjon?.institusjonsid,
                                institusjonsnavn = perspin?.institusjon?.institusjonsnavn
                        )

                )
        )

//        p7000.pensjon = Pensjon(
//                sak = Sak(
//                        kravtype = listOf(
//                                KravtypeItem(
//                                        datoFrist = "12 månder innen frist for klage"
//                                )
//                        )
//                ),
//                samletVedtak = SamletMeldingVedtak(
//                        vedtaksammendrag = "",
//                        tildeltepensjoner = TildeltePensjoner(
//                                pensjonType = "",
//                                vedtakPensjonType = "",
//                                addressatForRevurdering = "Nav\nPostboks 6600 Etterstad\n0607 Oslo, Norge",
//                                institusjon = Institusjon(
//                                        personNr = perspin?.identifikator,
//                                        land = perspin?.land
//                                )
//                        )
//                )
//        )
        p7000.pensjon = null

        logger.debug("Tilpasser P7000 forenklet preutfylling, Ferdig.")

        prefillData.sed = p7000

        return prefillData.sed
    }

}