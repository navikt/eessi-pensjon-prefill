package no.nav.eessi.pensjon.prefill.sed.krav

import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.*
import no.nav.eessi.pensjon.pensjonsinformasjon.KravHistorikkHelper
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.utils.toJson
import no.nav.pensjon.v1.sak.V1Sak
import no.nav.pensjon.v1.vedtak.V1Vedtak
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * preutfylling av NAV-P2000 SED for søknad krav om alderpensjon
 */
class PrefillP2000(private val prefillNav: PrefillPDLNav)  {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP2000::class.java) }

    fun prefillSed(prefillData: PrefillDataModel, personData: PersonDataCollection, sak: V1Sak?, vedtak: V1Vedtak? = null): SED {
        postPrefill(prefillData, sak, vedtak)

        val pensjon = populerPensjon(prefillData, sak)

        val nav = prefillPDLNav(prefillData, personData, pensjon?.kravDato)

        logger.info("kravdato : ${pensjon?.kravDato}")

        val sed = P2000(
            type = SedType.P2000,
            nav = nav,
            p2000pensjon = pensjon
        )

        validate(sed)
        return sed
    }

    private fun prefillPDLNav(prefillData: PrefillDataModel, personData: PersonDataCollection, krav: Krav?): Nav {
        return prefillNav.prefill(
            penSaksnummer = prefillData.penSaksnummer,
            bruker = prefillData.bruker,
            avdod = prefillData.avdod,
            personData = personData,
            bankOgArbeid = prefillData.getBankOgArbeidFromRequest(),
            krav = krav,
            annenPerson = null
        )
    }

    private fun postPrefill(prefillData: PrefillDataModel, sak: V1Sak?, vedtak: V1Vedtak?) {
        val SedType = SedType.P2000
        PrefillP2xxxPensjon.validerGyldigVedtakEllerKravtypeOgArsak(sak, SedType, vedtak)
        logger.debug("----------------------------------------------------------"
                + "\nSaktype                 : ${sak?.sakType} "
                + "\nSøker etter SakId       : ${prefillData.penSaksnummer} "
                + "\nSøker etter aktoerid    : ${prefillData.bruker.aktorId} "
                + "\n------------------| Preutfylling [$SedType] START |------------------ ")
    }

    private fun validate(sed: SED) {
        when {
            sed.nav?.bruker?.person?.etternavn == null -> throw ValidationException("Etternavn mangler")
            sed.nav?.bruker?.person?.fornavn == null -> throw ValidationException("Fornavn mangler")
            sed.nav?.bruker?.person?.foedselsdato == null -> throw ValidationException("Fødseldsdato mangler")
            sed.nav?.bruker?.person?.kjoenn == null -> throw ValidationException("Kjønn mangler")
            sed.nav?.krav?.dato == null -> {
                logger.warn("Kravdato mangler! Gjelder utsendelsen 'Førstegangsbehandling kun utland', se egen rutine på Navet.")
                throw ValidationException("Kravdato mangler\nGjelder utsendelsen \"Førstegangsbehandling kun utland\", se egen rutine på Navet.")
            }
        }
    }
    fun populerMeldinOmPensjon(personNr: String,
                               penSaksnummer: String?,
                               pensak: V1Sak?,
                               andreinstitusjonerItem: AndreinstitusjonerItem?,
                               gjenlevende: Bruker? = null,
                               kravId: String? = null): MeldingOmPensjonP2000 {

        val ytelselist = mutableListOf<YtelserItem>()

        val v1KravHistorikk = KravHistorikkHelper.finnKravHistorikkForDato(pensak)
        val melding = PrefillP2xxxPensjon.opprettMeldingBasertPaaSaktype(v1KravHistorikk, kravId, pensak?.sakType)
        val krav = PrefillP2xxxPensjon.createKravDato(v1KravHistorikk)

        when (pensak?.ytelsePerMaanedListe) {
            null -> {
                ytelselist.add(
                    PrefillP2xxxPensjon.opprettForkortetYtelsesItem(
                        pensak,
                        personNr,
                        penSaksnummer,
                        andreinstitusjonerItem
                    )
                )
            }
            else -> {
                try {
                    val ytelseprmnd = PrefillP2xxxPensjon.hentYtelsePerMaanedDenSisteFraKrav(
                        KravHistorikkHelper.hentKravHistorikkForsteGangsBehandlingUtlandEllerForsteGang(
                            pensak.kravHistorikkListe
                        ), pensak
                    )
                    ytelselist.add(
                        PrefillP2xxxPensjon.createYtelserItem(
                            ytelseprmnd,
                            pensak,
                            personNr,
                            penSaksnummer,
                            andreinstitusjonerItem
                        )
                    )
                } catch (ex: Exception) {
                    ytelselist.add(
                        PrefillP2xxxPensjon.opprettForkortetYtelsesItem(
                            pensak,
                            personNr,
                            penSaksnummer,
                            andreinstitusjonerItem
                        )
                    )
                }
            }
        }

        return MeldingOmPensjonP2000(
            melding = melding,
            pensjon = P2000Pensjon(
                ytelser = ytelselist,
                kravDato = krav,
                bruker = gjenlevende
            )
        )
    }
    fun populerPensjon(
        prefillData: PrefillDataModel,
        sak: V1Sak?
    ): P2000Pensjon? {
        val andreInstitusjondetaljer = EessiInformasjon().asAndreinstitusjonerItem()

        //valider pensjoninformasjon,
        return try {
            val pensjonsInformasjon = populerMeldinOmPensjon(
                prefillData.bruker.norskIdent,
                prefillData.penSaksnummer,
                sak,
                andreInstitusjondetaljer
            )

            logger.debug("Pensjoninformasjon: ${pensjonsInformasjon.toJson()}")

            if (prefillData.sedType != SedType.P6000) {
                val ytelser = pensjonsInformasjon.pensjon.ytelser?.first()
                val belop = ytelser?.beloep?.first()

                P2000Pensjon(
                    kravDato = pensjonsInformasjon.pensjon.kravDato,
                    ytelser = listOf(YtelserItem(
                        ytelse = pensjonsInformasjon.pensjon.ytelser?.first()?.ytelse,
                        status = ytelser?.status,
                        startdatoutbetaling = ytelser?.startdatoutbetaling.also { logger.debug("startdatoutbetaling: $it") },
                        startdatoretttilytelse = ytelser?.startdatoretttilytelse.also { logger.debug("Ststartdatoretttilytelseatus: $it") },
                        mottasbasertpaa = ytelser?.mottasbasertpaa.also { logger.debug("mottasbasertpaa: $it") },
                        totalbruttobeloepbostedsbasert = ytelser?.totalbruttobeloepbostedsbasert.also { logger.debug("totalbruttobeloepbostedsbasert: $it") },
                        totalbruttobeloeparbeidsbasert = ytelser?.totalbruttobeloeparbeidsbasert.also { logger.debug("totalbruttobeloeparbeidsbasert: $it") },
                        beloep = listOf(BeloepItem(
                            beloep = belop?.beloep,
                            valuta = belop?.valuta,
                            gjeldendesiden = belop?.gjeldendesiden,
                            utbetalingshyppighetAnnen = belop?.utbetalingshyppighetAnnen
                        )).also { logger.debug("beloep: $it") },
                    ))
                )
            } else pensjonsInformasjon.pensjon

        } catch (ex: Exception) {
            logger.error("Feilet ved preutfylling av pensjon, ${ex.message} ")
            null
            //hvis feiler lar vi SB få en SED i RINA
        }
    }
}

class ValidationException(message: String) : ResponseStatusException(HttpStatus.BAD_REQUEST, message)
