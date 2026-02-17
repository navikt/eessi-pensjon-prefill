package no.nav.eessi.pensjon.prefill.sed.krav

import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.*
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.utils.toJson
import no.nav.pensjon.v1.sak.V1Sak
import no.nav.pensjon.v1.vedtak.V1Vedtak
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * preutfylling av NAV-P2200 SED for søknad krav om uforepensjon
 */
class PrefillP2200(private val prefillNav: PrefillPDLNav) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP2200::class.java) }

    fun prefill(
        prefillData: PrefillDataModel,
        personData: PersonDataCollection,
        sak: V1Sak?,
        vedtak: V1Vedtak? = null
    ) : P2200 {
        logger.debug("----------------------------------------------------------"
                + "\nSaktype                 : ${sak?.toJson()} "
                + "\nSøker etter SakId       : ${prefillData.penSaksnummer} "
                + "\nSøker etter aktoerid    : ${prefillData.bruker.aktorId} "
                + "\n------------------| Preutfylling [${prefillData.sedType}] START |------------------ ")

        val sedType = prefillData.sedType

        val pensjon = populerPensjonP2200(prefillData, sak)

        //henter opp persondata
        val nav = prefillNav.prefill(
            penSaksnummer = prefillData.penSaksnummer,
            bruker = prefillData.bruker,
            personData = personData,
            bankOgArbeid = prefillData.getBankOgArbeidFromRequest(),
            krav = pensjon?.kravDato,
            annenPerson = null
        )

        PrefillP2xxxPensjon.validerGyldigVedtakEllerKravtypeOgArsak(sak, sedType, vedtak)

        return P2200(
            nav = nav,
            pensjon = pensjon
        ).also {
            logger.debug("-------------------| Preutfylling [$sedType] END |------------------- ")
        }
    }

    fun populerPensjonP2200(
        prefillData: PrefillDataModel,
        sak: V1Sak?
    ): P2200Pensjon? {
        val andreInstitusjondetaljer = EessiInformasjon().asAndreinstitusjonerItem()

        logger.debug("""Prefilldata: ${prefillData.toJson()}""")

        //valider pensjoninformasjon,
        return try {
            val pensjonsInformasjon: MeldingOmPensjonP2200 = PrefillP2xxxPensjon.populerMeldinOmPensjon(
                prefillData.bruker.norskIdent,
                prefillData.penSaksnummer,
                sak,
                andreInstitusjondetaljer
            )

            logger.debug("""Pensjoninformasjon: ${pensjonsInformasjon.toJson()}""")

            if (prefillData.sedType != SedType.P6000) {
                val ytelser = pensjonsInformasjon.pensjon.ytelser?.first()
                val belop = ytelser?.beloep?.firstOrNull()

                P2200Pensjon(
                    kravDato = pensjonsInformasjon.pensjon.kravDato,
                    ytelser = listOf(
                        YtelserItem(
                            ytelse = pensjonsInformasjon.pensjon.ytelser?.first()?.ytelse,
                            status = ytelser?.status,
                            startdatoutbetaling = ytelser?.startdatoutbetaling.also { logger.debug("startdatoUtbetaling: $it") },
                            startdatoretttilytelse = ytelser?.startdatoretttilytelse.also { logger.debug("StartdatoretTilYtelseStatus: $it") },
                            mottasbasertpaa = settMottattBasertPaa(ytelser?.totalbruttobeloeparbeidsbasert),
                            totalbruttobeloepbostedsbasert = ytelser?.totalbruttobeloepbostedsbasert.also {
                                logger.debug(
                                    "totalbruttobeloepbostedsbasert: $it"
                                )
                            },
                            totalbruttobeloeparbeidsbasert = ytelser?.totalbruttobeloeparbeidsbasert.also {
                                logger.debug(
                                    "totalbruttobeloeparbeidsbasert: $it"
                                )
                            },
                            beloep = if (belop != null) listOf(belop) else null.also { logger.debug("beloep: $it") },
                        )
                    ),
                    forespurtstartdato = pensjonsInformasjon.pensjon.forespurtstartdato.also { logger.debug("forespurtstartdato: $it") },
                    etterspurtedokumenter = pensjonsInformasjon.pensjon.etterspurtedokumenter.also { logger.debug("etterspurtedokumenter: $it") },
                )
            } else pensjonsInformasjon.pensjon

        } catch (ex: Exception) {
            logger.error("Feilet ved preutfylling av pensjon, ${ex.message} ")
            null
            //hvis feiler lar vi SB få en SED i RINA
        }
    }

    private fun settMottattBasertPaa(totalBruttoArbBasert: String?): String? {
        return if (totalBruttoArbBasert.isNullOrEmpty() || totalBruttoArbBasert == "0") {
            BasertPaa.botid.name
        } else null
    }

}
