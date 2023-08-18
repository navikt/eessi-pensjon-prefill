package no.nav.eessi.pensjon.prefill.sed.krav

import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.*
import no.nav.eessi.pensjon.pensjonsinformasjon.KravHistorikkHelper.finnKravHistorikk
import no.nav.eessi.pensjon.pensjonsinformasjon.KravHistorikkHelper.finnKravHistorikkForDato
import no.nav.eessi.pensjon.pensjonsinformasjon.KravHistorikkHelper.hentKravHistorikkForsteGangsBehandlingUtlandEllerForsteGang
import no.nav.eessi.pensjon.pensjonsinformasjon.models.EPSaktype
import no.nav.eessi.pensjon.pensjonsinformasjon.models.PenKravtype
import no.nav.eessi.pensjon.pensjonsinformasjon.models.PenKravtype.*
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.Fodselsnummer
import no.nav.eessi.pensjon.utils.simpleFormat
import no.nav.pensjon.v1.kravhistorikk.V1KravHistorikk
import no.nav.pensjon.v1.sak.V1Sak
import no.nav.pensjon.v1.vedtak.V1Vedtak
import no.nav.pensjon.v1.ytelsepermaaned.V1YtelsePerMaaned
import no.nav.pensjon.v1.ytelseskomponent.V1Ytelseskomponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

const val kravdatoMeldingOmP2100TilSaksbehandler = "Kravdato fra det opprinnelige vedtak med gjenlevenderett er angitt i SED P2100"

/**
 * Hjelpe klasse for sak som fyller ut NAV-SED-P2000 med pensjondata fra PESYS.
 */
object PrefillP2xxxPensjon {
    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP2xxxPensjon::class.java) }


    /**
     * 9.1- 9.2
     *
     *  Fra PSAK, kravdato på alderspensjonskravet
     *  Fra PSELV eller manuell kravblankett:
     *  Fyller ut fra hvilket tidspunkt bruker ønsker å motta pensjon fra Norge.
     *  Det er et spørsmål i søknadsdialogen og på manuell kravblankett. Det er ikke nødvendigvis lik virkningstidspunktet på pensjonen.
     */
    private fun createKravDato(valgtKrav: V1KravHistorikk?): Krav? {
        logger.debug("9.1        Dato Krav (med korrekt data fra PESYS krav.virkningstidspunkt)")
        logger.debug("KravType   :  ${valgtKrav?.kravType}")
        logger.debug("mottattDato:  ${valgtKrav?.mottattDato}")
        logger.debug("--------------------------------------------------------------")

        logger.debug("Prøver å sette kravDato til Virkningstidpunkt: ${valgtKrav?.kravType} og dato: ${valgtKrav?.mottattDato}")

        if (valgtKrav != null && valgtKrav.mottattDato != null) {
            return Krav(dato = valgtKrav.mottattDato?.simpleFormat())
        }
        return null
    }

    /**
     *
     *  4.1
     *  Vi må hente informasjon fra PSAK:
     *  - Hvilke saker bruker har
     *  - Status på hver sak
     *  - Hvilke kravtyper det finnes på hver sak
     *  - Saksnummer på hver sak
     *  - første virk på hver sak
     *  Hvis bruker mottar en løpende ytelse, er det denne ytelsen som skal vises.
     *  Hvis bruker mottar både uføretrygd og alderspensjon, skal det vises alderspensjon.
     *  Hvis bruker ikke mottar løpende ytelse, skal man sjekke om han har søkt om en norsk ytelse.
     *  Hvis han har søkt om flere ytelser, skal man bruke den som det sist er søkt om.
     *  Det skal vises resultatet av denne søknaden, dvs om saken er avslått eller under behandling.
     *  For å finne om han har søkt om en norsk ytelse, skal man se om det finnes krav av typen:  «Førstegangsbehandling», «Førstegangsbehandling Norge/utland»,
     *  «Førstegangsbehandling bosatt utland» eller «Mellombehandling».
     *  Obs, krav av typen «Førstegangsbehandling kun utland» eller Sluttbehandling kun utland» gjelder ikke norsk ytelse.
     */
    fun populerMeldinOmPensjon(personNr: String,
                               penSaksnummer: String,
                               pensak: V1Sak?,
                               andreinstitusjonerItem: AndreinstitusjonerItem?,
                               gjenlevende: Bruker? = null,
                               kravId: String? = null): MeldingOmPensjon {

        logger.info("4.1           Informasjon om ytelser")

        val ytelselist = mutableListOf<YtelserItem>()

        val v1KravHistorikk = finnKravHistorikkForDato(pensak)
        val melding = opprettMeldingBasertPaaSaktype(v1KravHistorikk, kravId, pensak?.sakType)
        val krav = createKravDato(v1KravHistorikk)

        logger.info("Krav (dato) = $krav")

        when (pensak?.ytelsePerMaanedListe) {
            null -> {
                logger.info("forkortet ytelsebehandling ved ytelsePerMaanedListe = null, status: ${pensak?.status}")
                ytelselist.add(opprettForkortetYtelsesItem(pensak, personNr, penSaksnummer, andreinstitusjonerItem))
            }
            else -> {
                try {
                    logger.info("sakType: ${pensak.sakType}")
                    val ytelseprmnd = hentYtelsePerMaanedDenSisteFraKrav(hentKravHistorikkForsteGangsBehandlingUtlandEllerForsteGang(pensak.kravHistorikkListe, pensak.sakType), pensak)
                    ytelselist.add(createYtelserItem(ytelseprmnd, pensak, personNr, penSaksnummer, andreinstitusjonerItem))
                } catch (ex: Exception) {
                    logger.warn(ex.message, ex)
                    ytelselist.add(opprettForkortetYtelsesItem(pensak, personNr, penSaksnummer, andreinstitusjonerItem))
                }
            }
        }

        return MeldingOmPensjon(
            melding = melding,
            pensjon = Pensjon(
                ytelser = ytelselist,
                kravDato = krav,
                gjenlevende = gjenlevende
            )
        )
    }

    /**
     * Skal validere på kravtype og kravårrsak Krav SED P2000 Alder og P2200 Uførep
     * https://confluence.adeo.no/pages/viewpage.action?pageId=338181302
     *
     * FORSTEG_BH       Førstegangsbehandling (ingen andre) skal vi avslutte
     * F_BH_KUN_UTL     Førstegangsbehandling utland (ingen andre) skal vi avslutte
     * F_BH_BO_UTL      Førstegangsbehandling bosatt utland ikke finnes skal vi avslutte
     * F_BH_MED_UTL     Førstegangsbehandling Norge/utland ikke finnes sakl vi avslutte
     *
     */
    private fun validerGyldigKravtypeOgArsak(sak: V1Sak?, sedType: SedType, vedtak: V1Vedtak?) {
        logger.info("start på validering av $sedType")

        avsluttHvisKunDenneKravTypeIHistorikk(sak, sedType, FORSTEG_BH)

        val forsBehanBoUtlandTom = finnKravHistorikk(F_BH_BO_UTL, sak?.kravHistorikkListe).isNullOrEmpty()
        val forsBehanMedUtlandTom = finnKravHistorikk(F_BH_MED_UTL, sak?.kravHistorikkListe).isNullOrEmpty()
        val behandleKunUtlandTom = finnKravHistorikk(F_BH_KUN_UTL, sak?.kravHistorikkListe).isNullOrEmpty()
        val vedtakErTom = (vedtak == null)

        if (forsBehanBoUtlandTom and forsBehanMedUtlandTom and behandleKunUtlandTom and vedtakErTom) {
            logger.debug("forsBehanBoUtlanTom: $forsBehanBoUtlandTom, forsBehanMedUtlanTom: $forsBehanMedUtlandTom, behandleKunUtlandTom: $behandleKunUtlandTom")
            logger.warn("Kan ikke opprette krav-SED: $sedType da vedtak og førstegangsbehandling utland mangler. Dersom det gjelder utsendelse til avtaleland, se egen rutine for utsendelse av SED på Navet.")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Det finnes ingen iverksatte vedtak for førstegangsbehandling kun utland. Vennligst gå til EESSI-Pensjon fra vedtakskontekst.")
        }

        if (vedtak != null && vedtak.isBoddArbeidetUtland == false) {
            logger.warn("Du kan ikke opprette krav-SED $sedType hvis ikke \"bodd/arbeidet i utlandet\" er krysset av")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Du kan ikke opprette krav-SED $sedType hvis ikke \"bodd/arbeidet i utlandet\" er krysset av")
        }

        logger.info("avslutt på validering av $sedType, fortsetter med preutfylling")
    }

    fun validerGyldigVedtakEllerKravtypeOgArsak(sak:V1Sak?, sedType: SedType, vedtak: V1Vedtak?) {

        vedtak?.let {
            logger.info("Validering på vedtak bosatt utland ${it.isBoddArbeidetUtland}")
            if (it.isBoddArbeidetUtland) return
        }
        validerGyldigKravtypeOgArsak(sak, sedType, vedtak)
    }


    //felles kode for validering av P2000, P2100 og P2200
    fun avsluttHvisKunDenneKravTypeIHistorikk(sak: V1Sak?, sedType: SedType, kravType: PenKravtype) {
        if (kunDenneKravTypeIHistorikk(sak, kravType))  {
            logger.warn("Det er ikke markert for bodd/arbeidet i utlandet. Krav SED $sedType blir ikke opprettet")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Det er ikke markert for bodd/arbeidet i utlandet. Krav SED $sedType blir ikke opprettet")
        }
    }

    private fun kunDenneKravTypeIHistorikk(sak: V1Sak?, kravType: PenKravtype): Boolean {
        val historikkForKravtype = finnKravHistorikk(kravType, sak?.kravHistorikkListe)
        return (historikkForKravtype != null
                && historikkForKravtype.size == sak?.kravHistorikkListe?.kravHistorikkListe?.size)
    }

    internal fun opprettMeldingBasertPaaSaktype(kravHistorikk: V1KravHistorikk?, kravId: String?, saktype: String?): String {
        if (kravHistorikk?.kravId == kravId) return ""
            return when (saktype) {
                EPSaktype.ALDER.name, EPSaktype.UFOREP.name -> kravdatoMeldingOmP2100TilSaksbehandler
                else -> ""
            }
    }

    /**
     *  4.1 (for kun_uland,mangler inngangsvilkår)
     */
    internal fun opprettForkortetYtelsesItem(pensak: V1Sak?, personNr: String, penSaksnummer: String, andreinstitusjonerItem: AndreinstitusjonerItem?): YtelserItem {
        return YtelserItem(
                //4.1.1
                ytelse = settYtelse(pensak),
                //4.1.3 - fast satt til søkt
                status = "01",
                //4.1.4
                pin = createInstitusjonPin(personNr),
                //4.1.4.1.4
                institusjon = createInstitusjon(penSaksnummer, andreinstitusjonerItem)
        )
    }

    /**
     *  4.1.1
     *
     *  Ytelser
     */
    private fun settYtelse(pensak: V1Sak?): String {
        logger.debug("4.1.1         Ytelser")
        return mapSaktype(pensak?.sakType)
    }

    /**
     *  4.1
     *
     *  Informasjon om ytelser den forsikrede mottar
     */
    internal fun createYtelserItem(ytelsePrmnd: V1YtelsePerMaaned, pensak: V1Sak, personNr: String, penSaksnummer: String, andreinstitusjonerItem: AndreinstitusjonerItem?): YtelserItem {
        logger.debug("4.1   YtelserItem")
        return YtelserItem(

                //4.1.1
                ytelse = settYtelse(pensak),

                //4.1.3 (dekkes av pkt.4.1.1)
                status = createPensionStatus(pensak),
                //4.1.4
                pin = createInstitusjonPin(personNr),
                //4.1.4.1.4
                institusjon = createInstitusjon(penSaksnummer, andreinstitusjonerItem),

                //4.1.5
                startdatoutbetaling = ytelsePrmnd.fom?.simpleFormat(),

                //4.1.7 (sak - forstevirkningstidspunkt)
                startdatoretttilytelse = createStartdatoForRettTilYtelse(pensak),

                //4.1.9 - 4.1.9.5.1
                beloep = createYtelseItemBelop(ytelsePrmnd, ytelsePrmnd.ytelseskomponentListe),

                //4.1.10.1
                mottasbasertpaa = createPensionBasedOn(pensak, personNr),

                //4.1.10.3
                totalbruttobeloeparbeidsbasert = ytelsePrmnd.belop.toString(),
        )
    }

    internal fun hentYtelsePerMaanedDenSisteFraKrav(kravHistorikk: V1KravHistorikk, pensak: V1Sak): V1YtelsePerMaaned {
        val ytelser = pensak.ytelsePerMaanedListe.ytelsePerMaanedListe
        val ytelserSortertPaaFom = ytelser.sortedBy { it.fom.toGregorianCalendar() }

        logger.debug("-----------------------------------------------------")
        ytelserSortertPaaFom.forEach {
            logger.debug("Sammenligner ytelsePerMaaned: ${it.fom}  Med virkningtidpunkt: ${kravHistorikk.virkningstidspunkt}")
            if (it.fom.toGregorianCalendar() >= kravHistorikk.virkningstidspunkt.toGregorianCalendar()) {
                logger.debug("Return følgende ytelsePerMaaned: ${it.fom}")
                return it
            }
            logger.debug("-----------------------------------------------------")
        }
        return V1YtelsePerMaaned()
    }

    /**
     *  4.1.7
     *
     *  Start date of entitlement to benefits  - trenger ikke fylles ut
     */
    private fun createStartdatoForRettTilYtelse(pensak: V1Sak): String? {
        logger.debug("4.1.7         Startdato for ytelse (forsteVirkningstidspunkt) ")
        return pensak.forsteVirkningstidspunkt?.simpleFormat()
    }

    private fun createInstitusjon(penSaksnummer: String, andreinstitusjonerItem: AndreinstitusjonerItem?): Institusjon {
        logger.debug("4.1.4.1.4     Institusjon")
        return Institusjon(
                institusjonsid = andreinstitusjonerItem?.institusjonsid,
                institusjonsnavn = andreinstitusjonerItem?.institusjonsnavn,
                saksnummer = penSaksnummer
        )
    }

    /**
     *  4.1.9
     *
     *  4.1.9 Fra PSAK
     *  Denne seksjonen (4.1.9) er gjentakende.
     *  Vi skal vise beløpshistorikk 5 år tilbake i tid.
     *  Hvis bruker mottar en løpende ytelse med beløp større enn 0 kr, skal det nåværende beløpet vises her.
     *  Det skal gjentas et beløp for hver beløpsendring, inntil 5 år tilbake i tid.
     *  4.1.9.2 Currency Fra PSAK.
     *  Her fylles ut FOM-dato for hvert beløp i beløpshistorikk 5 år tilbake i tid.
     *  4.1.9.4 Payment frequency     Preutfylt med Månedlig
     *  OBS – fra år 2021 kan det bli aktuelt med årlige utbetalinger, pga da kan brukere få utbetalt kap 20-pensjoner med veldig små beløp (ingen nedre grense)
     *  4.1.9.5.1  nei
     */
    private fun createYtelseItemBelop(ytelsePrMnd: V1YtelsePerMaaned, ytelsekomp: List<V1Ytelseskomponent>): List<BeloepItem> {
        logger.debug("4.1.9         Beløp")
        return ytelsekomp.map {
            BeloepItem(
                    //4.1.9.1
                    beloep = it.belopTilUtbetaling.toString(),

                    //4.1.9.2
                    valuta = "NOK",

                    //4.1.9.3
                    gjeldendesiden = createGjeldendesiden(ytelsePrMnd),

                    //4.1.9.4
                    betalingshyppighetytelse = createBetalingshyppighet(),
            )
        }
    }

    /**
     *  4.1.9.3
     *
     *  Fra PSAK.
     *  Her fylles ut FOM-dato for hvert beløp i beløpshistorikk 5 år tilbake i tid.
     */
    private fun createGjeldendesiden(ytelsePrMnd: V1YtelsePerMaaned): String {
        logger.debug("4.1.9.3         Gjeldendesiden")
        return ytelsePrMnd.fom.simpleFormat()
    }

    /**
     *  4.1.9.4
     *
     *  Preutfylt med Månedlig
     *  OBS – fra år 2021 kan det bli aktuelt med årlige utbetalinger, pga da kan brukere få utbetalt kap 20-pensjoner med veldig små beløp (ingen nedre grense)
     *
     *  01: Årlig
     *  02: Kvartal
     *  03: Månded 12/år
     *  04: Måned 13/år
     *  05: Måned 14/år
     *  06: Ukentlig
     *  99: Annet
     */
    private fun createBetalingshyppighet(): String {
        logger.debug("4.1.9.4         Betalingshyppighetytelse")
        return "03"
    }

    /**
     *  4.1.10.1
     *
     *  Pensjonen mottas basert på
     *
     *  Fra PSAK. Det må settes opp forretningsregler. Foreløpig forslag:
     *  Hvis bruker har Dnr, hukes kun av for Working
     *  Hvis bruker har Fnr:
     *  Hvis UT: Hvis bruker har minsteytelse, velges kun Residence. Ellers velges både Residence og Working.
     *  Hvis AP: Hvis bruker mottar tilleggspensjon, velges både Residence og Working. Ellers velges kun Residence.
     *  Hvis GJP: Hvis bruker mottar tilleggspensjon, velges både Residence og Working. Ellers velges kun Residence.

     *  [01] Botid
     *  [02] I arbeid
     */
    private fun createPensionBasedOn(pensak: V1Sak, personNr: String): String? {
        logger.debug("4.1.10.1      Pensjon basertpå")
        val navfnr = Fodselsnummer.fra(personNr)

        if (navfnr != null && navfnr.isDNumber()) {
                return "01" // Botid
        }

        return when (pensak.sakType) {
            "ALDER" -> "01"
            "UFOREP" -> "02"
            else -> null
        }
    }

    /**
     *  4.1.4.1
     *
     *  4.1.4.1.1
     *  preutfylles med Norge.Dette kan gjøres av EESSI-pensjon
     *  4.1.4.1.2
     *  preutfylles med norsk PIN
     *  4.1.4.1.3
     *  kan preutfylles med Pensjon (men det er vel opplagt at pensjonsytelse tilhører sektor Pensjon)  Dette kan gjøres av EESSI-pensjon
     *  4.1.4.1.4
     *  nei
     *  4.1.4.1.4.1
     *  Preutfylles med NAV sin Institusjons-ID fra IR.    Dette kan gjøres av EESSI-pensjon
     *  4.1.4.1.4.2
     *  preutfylles med NAV Dette kan gjøres av EESSI-pensjon
     */
    private fun createInstitusjonPin(personNr: String): PinItem {
        logger.debug("4.1.4.1       Institusjon Pin")
        return PinItem(
                //4.1.4.1.1
                land = "NO",
                //4.1.4.1.2
                identifikator = personNr,
                //4.1.4.1.3
                sektor = "04", //(kun pensjon)
        )
    }

    /**
     *  4.1.3
     *
     *  Dekkes av kravene på pkt 4.1.1
     *  Her skal vises status på den sist behandlede ytelsen, dvs om kravet er blitt avslått, innvilget eller er under behandling.
     *  Hvis bruker mottar en løpende ytelse, skal det alltid vises Innvilget.
     */
    private fun createPensionStatus(pensak: V1Sak): String {
        logger.debug("4.1.3         Status")
        return mapSakstatus(pensak.status)
    }

    fun populerPensjon(
        prefillData: PrefillDataModel,
        sak: V1Sak?
    ): Pensjon? {
        val andreInstitusjondetaljer = EessiInformasjon().asAndreinstitusjonerItem()

        //valider pensjoninformasjon,
        return try {
            val meldingOmPensjon = populerMeldinOmPensjon(
                prefillData.bruker.norskIdent,
                prefillData.penSaksnummer,
                sak,
                andreInstitusjondetaljer
            )
            if (prefillData.sedType != SedType.P6000) {
                //vi skal ha blank pensjon ved denne toggle, men vi må ha med kravdato
                Pensjon(kravDato = meldingOmPensjon.pensjon.kravDato)
            } else {
                meldingOmPensjon.pensjon
            }
        } catch (ex: Exception) {
            logger.error(ex.message, ex)
            null
            //hvis feiler lar vi SB få en SED i RINA
        }
    }
}
