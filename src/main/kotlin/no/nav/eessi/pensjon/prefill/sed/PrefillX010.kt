package no.nav.eessi.pensjon.prefill.sed

import no.nav.eessi.pensjon.eux.model.sed.*
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.shared.api.BankOgArbeid
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.utils.toJson
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillX010(private val prefillNav: PrefillPDLNav)  {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillX010::class.java) }

    fun prefill(penSaksnummer: String?,
                bruker: PersonInfo,
                avdod: PersonInfo?,
                brukerinformasjon: BankOgArbeid?,
                personData: PersonDataCollection,
                x009: X009? = null): X010 {

        logger.info("Tilpasser X010 preutfylling med data fra X009")

        logger.info("*".repeat(26))
        logger.info("X009 paaminnelse: ${x009?.xnav?.sak?.paaminnelse?.toJson()}")
        logger.info("*".repeat(26))

        val navsed = prefillNav.prefill(
            penSaksnummer = penSaksnummer,
            bruker = bruker,
            avdod = avdod,
            personData = personData,
            bankOgArbeid = brukerinformasjon,
        )
        val gjenlevende = avdod?.let { prefillNav.createGjenlevende(personData.forsikretPerson, bruker) }

        val person =  gjenlevende?.person ?:  navsed.bruker?.person

        return X010 (
                xnav = XNav(
                        sak = Navsak(
                                kontekst = Kontekst(
                                        bruker = Bruker(
                                                person = Person(
                                                        fornavn = person?.fornavn,
                                                        etternavn = person?.etternavn,
                                                        foedselsdato = person?.foedselsdato,
                                                        kjoenn = person?.kjoenn
                                                )
                                        )
                                ),
                                paaminnelse = Paaminnelse(
                                    svar = Svar(
                                        informasjon = Informasjon(
                                            kommersenere = populerKommersenereFraX009(x009),
                                            ikketilgjengelig = populerIkkeTilgjengelig(x009)
                                        )
                                    )

                                )
                        )
                )
        ).also {
            logger.debug("Tilpasser X010 forenklet preutfylling, Ferdig.: ${it.toJsonSkipEmpty()}")
        }
    }

    //autogenerering av svar med data fra X009 (dersom detaljer 2.1.2) men at opplysninger mangler
    //filterer inn kun tomme detaljer
    private fun populerIkkeTilgjengelig(x009: X009?): List<IkkeTilgjengelig>? {
        return x009?.xnav?.sak?.paaminnelse?.sende
            ?.filter{ item -> item?.detaljer == null }
            ?.mapNotNull { sendtItem ->
                IkkeTilgjengelig(
                    type = sendtItem?.type,
                    opplysninger = "Missing details",
                    grunn = Grunn("annet","Missing details")
                )
            }
    }

    //autogenerering av svar med data fra X009
    //filterer bort tomme detaljer
    private fun populerKommersenereFraX009(x009: X009?): List<KommersenereItem>? {
        logger.debug("Hva finnes av x009 paaminnelse: ${x009?.xnav?.sak?.paaminnelse?.sende?.onEach{}}")
        return x009?.xnav?.sak?.paaminnelse?.sende
            ?.filterNot{ item -> item?.detaljer == null }
            ?.mapNotNull { sendtItem ->
                KommersenereItem(type = sendtItem?.type, opplysninger = sendtItem?.detaljer)
            }
    }
}
