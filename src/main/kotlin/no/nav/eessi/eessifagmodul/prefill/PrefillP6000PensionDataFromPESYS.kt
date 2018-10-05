package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.services.pensjonsinformasjon.PensjonsinformasjonService
import no.nav.eessi.eessifagmodul.utils.simpleFormat
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.sak.V1Sak
import no.nav.pensjon.v1.vedtak.V1Vedtak
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component


@Component
/**
 * Hjelpe klasse for P6000 som fyller ut NAV-SED med pensjondata fra PESYS.
 */
class PrefillP6000PensionDataFromPESYS(private val pensjonsinformasjonService: PensjonsinformasjonService): Prefill<Pensjon> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP6000PensionDataFromPESYS::class.java) }

    interface KENUM<T> {
        fun valid(value: String): T
    }
    //K_SAK_T Kodeverk fra PESYS
    enum class KSAK(val ksak: String): KENUM<Boolean> {
        ALDER("ALDER"),
        UFOREP("UFOREP"),
        GJENLEV("GJENLEV"),
        BARNEP("BARNEP");
        override fun valid(value: String): Boolean {
            return this.ksak == value
        }
    }

    //K_KRAV_VELG_T Kodeverk fra PESYS
    enum class KKRAV(val kkrav: String): KENUM<Boolean> {
        AVDOD_MOR("AVDOD_MOR"),
        AVDOD_FAR("AVDOD_FAR"),
        FORELDRELOS("FORELDRELOS"),
        MIL_INV("MIL_INV"),
        MIL_GJENLEV("MIL_GJENLEV"),
        MIL_BARNEP("MIL_BARNEP"),
        SIVIL_INV("SIVIL_INV"),
        SIVIL_GJENLEV("SIVIL_GJENLEV"),
        SIVIL_BARNEP("SIVIL_BARNEP"),
        FORELOPIG("FORELOPIG"),
        VARIG("VARIG"),
        UP("UP"),
        EP("EP"),
        BP("BP"),
        NSB("NSB");
        override fun valid(value: String): Boolean {
            return this.kkrav== value
        }
    }



    override fun prefill(prefillData: PrefillDataModel): Pensjon {

//        TODO: må ha mulighet for å sende inn saksnummer og vedtakid (hvor får vi vedtakid ifra?)
//        TODO: skal vi på P6000 spørre om et vedtak? eller hentAlt på P6000 og alle valgte vedtak?
//        TODO: se P6000 - 4.1 kan legge inn liste over vedtak (decision)

        logger.debug("----------------------------------------------------------")

        val sedId = prefillData.getSEDid()
        logger.debug("[$sedId] Preutfylling Utfylling Data")

        val vedtakId = if ( prefillData.vedtakId.isNotBlank())  prefillData.vedtakId else throw IllegalArgumentException("Mangler vedtakID")
        logger.debug(" vedtakId: $vedtakId")

        val starttime = System.currentTimeMillis()
        logger.debug(" henter pensjon data fra PESYS ")
        val pendata: Pensjonsinformasjon = pensjonsinformasjonService.hentAlt(prefillData.penSaksnummer) // ha med saknr og vedtak?
        val endtime = System.currentTimeMillis()
        val tottime = endtime - starttime
        logger.debug(" ferdig hentet pensjon data fra PESYS. Det tok $tottime ms")

        logger.debug("----------------------------------------------------------")

        return Pensjon(
                //4.1
                vedtak = listOf(
                        createVedtakItem(pendata)
                ),
                //5.1
                reduksjon = listOf(
                        createReduksjon(pendata)
                ),
                //6.1
                sak = createSak(pendata),
                //6.x
                tilleggsinformasjon = createTilleggsinformasjon(pendata)
        )

    }


    //4.1
    private fun createVedtakItem(pendata: Pensjonsinformasjon): VedtakItem {

        logger.debug("4.1       VedtakItem")

        return VedtakItem(

                //4.1.1  $pensjon.vedtak[x].type
                type = createVedtakTypePensionWithRule(pendata),

                //4.1.2  $pensjon.vedtak[x].basertPaa
                basertPaa = createVedtakGrunnlagPentionWithRule(pendata),

                //4.1.3.1 $pensjon.vedtak[x].basertPaaAnnen
                basertPaaAnnen = createVedtakAnnenTypePentionWithRule(pendata),

                //4.1.4 $pensjon.vedtak[x].resultat
                resultat = createTypeVedtakPentionWithRule(pendata),

                //4.1.5 $pensjon.vedtak[x].artikkel
                artikkel = createArtikkelInnvilgelseAvYtelseWithRule(pendata),

                //4.1.6  $pensjon.vedtak[x].virkningsdato
                virkningsdato = pendata.vedtak?.virkningstidspunkt?.simpleFormat(),

                //4.1.7 -- $pensjon.vedtak[x].beregning[x]..
                beregning = listOf(
                        createBeregningItem(pendata)
                ),

                //4.1.8  $pensjon.vedtak[x].kjoeringsdato -- datoFattetVedtak?
                kjoeringsdato = null, //TODO mangler (Sak.forsteVirkningstidspunkt)

                //4.1.10 - 4.1.12 $pensjon.vedtak[x].grunnlag
                grunnlag =  createGrunnlag(pendata),

                //Na
                mottaker =  null,      //ikke i bruk?
                ukjent = null,         //ikke i bruk?
                trekkgrunnlag = null,  //ikke i bruk?

                //4.1.13.2
                begrunnelseAnnen = null,

                //4.1.13.1 -- 4.1.13.2.1
                avslagbegrunnelse = listOf(
                        createAvlsagsBegrunnelse(pendata)
                    ),

                //4.1.14.1 // Ikke i bruk
                delvisstans = null
        )

    }

    //4.1.1
    /*
        HVIS søkt pensjon er Alderspensjon etter fylt 67 år, SÅ skal det vises «Old age»
        HVIS søkt pensjon er Alderspensjon før fylt 67 år, SÅ skal det vises «Early Old age»
        HVIS søkt pensjon er Uføretrygd, SÅ skal det vises «Invalidity»
        HVIS søkt pensjon er uføretrygd med gjenlevenderett, SÅ skal det vises «Invalidity»
        HVIS søkt pensjon er gjenlevendepensjon, SÅ skal det vises «Survivors»
        HVIS søkt pensjon er Alderspensjon etter fylt 67 år og med gjenlevenderettigheter, SÅ skal det vises «Old age»
        Hvis søkt pensjon er barnepensjon,  , SÅ skal det vises «Survivors»
     */
    fun createVedtakTypePensionWithRule(pendata: Pensjonsinformasjon): String {
        //04 og 05 benyttes ikke

        //v1sak fra PESYS
        val v1sak = pendata.sak as V1Sak

        //pensjon før 67 (ja/nei - sann/usann)
        val pensjonUttakTidligereEnn67 = v1sak.isUttakFor67

        //type fra K_SAK_T
        val type = v1sak.sakType


        val sakType = KSAK.valueOf(type)
        logger.debug("4.1.1         VedtakTypePension")

        return when(sakType) {

            KSAK.ALDER -> {
                return when(pensjonUttakTidligereEnn67) {
                    true -> "06" //"Førdtispensjon" //06
                    else -> "01"  //""Alderspensjon" //01
                }
            }

            KSAK.UFOREP -> "02" // "Uførhet" //"02"

            KSAK.BARNEP, KSAK.GJENLEV -> "03" // "Etterlatte" //"03"

        }
    }

    //4.1.2 P6000
    fun createVedtakGrunnlagPentionWithRule(pendata: Pensjonsinformasjon): String {
        //01-residece, 02-working, 99-other --> 4.1.3.1 --> other

        logger.debug("4.1.2         VedtakGrunnlagPention")
        val sakType = KSAK.valueOf(pendata.sak.sakType)

        return when(sakType) {

            KSAK.ALDER -> {
                return when(isMottarMinstePensjonsniva(pendata)) {
                    true -> "01"
                    false -> "02"
                }
            }
            KSAK.UFOREP -> {
                //"01-"02"
                "02"
            }
            KSAK.GJENLEV -> {
                "02"
            }
            KSAK.BARNEP -> {
                "02"
            }
        }
    }

    //4.1.3.1 P6000 (other type)
    fun createVedtakAnnenTypePentionWithRule(pendata: Pensjonsinformasjon): String? {

        logger.debug("4.1.3.1       VedtakAnnenTypePention")
        if (createVedtakGrunnlagPentionWithRule(pendata) == "99") {

            //TODO: Regler for annen bergeningtekst.

            //return "Ytelsen er beregnet etter spesielle beregningsregler for unge uføre"

            return "Ytelsen er beregnet etter regler for barnepensjon"

        }
        return null
    }

    //4.1.4
    fun createTypeVedtakPentionWithRule(pendata: Pensjonsinformasjon): String {
        //01> invilg , 02> avslag , 03> ny beregning , 04> foreløping utbeteling
        logger.debug("4.1.4         TypeVedtakPention")

        return "01"
    }

    //4.1.5
    fun createArtikkelInnvilgelseAvYtelseWithRule(pendata: Pensjonsinformasjon): String {
        //01=> 883/2004 art 52 A, 02=> 883/2004 art 52 B, 03=> 883/2004 art 57 2, 04=> 883/2004 art 57 3, 05=> 833/2004 art 60 2
        logger.debug("4.1.5         ArtikkelInnvilgelseAvYtelse")

        return "01"
    }


    //4.1.7 --
    fun createBeregningItem(pendata: Pensjonsinformasjon): BeregningItem {

        //pendata.ytelsePerMaanedListe

        logger.debug("4.1.7         BeregningItem")
        return BeregningItem(

            beloepNetto =  BeloepNetto(
                    beloep = null
                ),

            valuta = null,

            beloepBrutto = BeloepBrutto(
                    ytelseskomponentTilleggspensjon = null,
                    beloep = null,
                    ytelseskomponentGrunnpensjon = null,
                    ytelseskomponentAnnen = null
                ),

            utbetalingshyppighetAnnen = null,

            periode = Periode(
                    fom = null,
                    tom = null
                ),

            utbetalingshyppighet =  null
        )
    }

    //4.1.10 - 4.1.12
    fun createGrunnlag(pendata: Pensjonsinformasjon): Grunnlag {

        logger.debug("4.1.10        Grunnlag")
        return Grunnlag(

                //4.1.10 - Nei
                medlemskap = null,

                //4.1.11
                opptjening = Opptjening(
                        forsikredeAnnen = createOpptjeningForsikredeAnnen(pendata)
                    ),

                //4.1.12
                framtidigtrygdetid = null
        )

    }
    /*
        HVIS Sakstype er Alderspensjon uten gjenlevenderett
        ELLER Uføretrygd uten gjenlevenderett
        SÅ skal det velges alternativ "[01] Nei".

        HVIS Sakstype er (Alderspensjon med gjenlevenderett
        OG ytelse  uten gjenlevenderett er vinnende beregning)
        ELLER Uføretrygd med gjenlevendetillegg som er lik 0
        SÅ skal det velges alternativ "[01] Nei".

        HVIS Sakstype er (Alderspensjon med gjenlevenderett
        OG ytelse  med gjenlevenderett er vinnende beregning)
        ELLER Uføretrygd med gjenlevendetillegg som er større enn 0
        SÅ skal det velges alternativ "[02] Delvis".

        HVIS Sakstype er Gjenlevendepensjon
        ELLER Barnepensjon
        SÅ skal det velges alternativ "[03] Fullstendig".
     */
    //4.1.11
    fun createOpptjeningForsikredeAnnen(pendata: Pensjonsinformasjon): String? {
        logger.debug("4.1.11        OpptjeningForsikredeAnnen")

        pendata.sak.sakType
        pendata.vedtak.kravVelgType
        pendata.vedtak.kravGjelder

        val vilkarsvurderingListe = pendata.vilkarsvurderingListe.vilkarsvurderingListe
        vilkarsvurderingListe.forEach {

        }
        val trygdetidListe = pendata.trygdetidListe.trygdetidListe
        trygdetidListe.forEach {
        }

        return null
    }


    //4.1.13.1 - 4.1.13.2.1
    fun createAvlsagsBegrunnelse(pendata: Pensjonsinformasjon): AvslagbegrunnelseItem {

        logger.debug("4.1.13        AvlsagsBegrunnelse")
        return AvslagbegrunnelseItem(

                begrunnelse = null,

                annenbegrunnelse  = null
        )

    }

    //5.1
    fun createReduksjon(pendata: Pensjonsinformasjon): ReduksjonItem {

        logger.debug("5.1       Reduksjon")
        return ReduksjonItem(

        )

    }

    //pr 6.5.1
    private val timelimit_6_5_1 = "six weeks from the date the decision is received"


    //6.2
    fun createTilleggsinformasjon(pendata: Pensjonsinformasjon): Tilleggsinformasjon {

        logger.debug("6.2       Tilleggsinformasjon")
        return Tilleggsinformasjon(

            //6.2-6.3
            opphoer = Opphoer(
                    //6.2
                    dato = createOpphorerDato(pendata),
                    //6.3
                    annulleringdato = null
            ),

            //6.5.2.1
            //På logges EnhetID /NAV avsender (PENSJO, UTFORP, ETTERLATP)?
            andreinstitusjoner  = listOf(
                    AndreinstitusjonerItem()
            ),

            //6.5.2 - 6.6  $pensjon.tilleggsinformasjon.artikkel48
            //05.10.2018 -
            //TODO Må fylles ut manuelt? i EP-11? eller Rina?
            artikkel48 = null,

            //6.7.1.4
            //05.10.2018 - Nei
            annen = Annen(
                    //  $pensjon.tilleggsinformasjon.annen.institusjonsid
                    institusjonsadresse = Institusjonsadresse(

                    )
            ),
            //6.7.2
            //05.10.2018 Nei
            saksnummer = null,

            //6.8 $pensjon.tilleggsinformasjon.saksnummerAnnen
            //05.10.2018 Nei
            saksnummerAnnen = null,


            //6.9 other information
            //05.10.2018 Nei
            anneninformation = null,

            person = Person(),
            dato = null
        )
    }

    /*
        05.10.2018
        Må fylles ut manuelt
        TODO Må fylles ut manuelt? i EP-11? eller Rina?
        6.6  $pensjon.tilleggsinformasjon.artikkel48
        6.6. The decision has been given as a result of the review according to the Art. 48(2) of Regulation 987/2009
     */
    //6.6
    private fun createArtikkel48(pendata: Pensjonsinformasjon): String {
        logger.debug("6.6       Artikkel48")
        return ""
    }

    /**
        HVIS kravtype er Revurdering,
        OG Resultat er Opphør,
        OG Resultatbegrunnelse er Annullering,
        OG vedtak er attestert
        SÅ skal Antatt virkningsdato vises her.  Datoen skal vises i formatet DD-MM-YYYY
     **/
    //6.2
    private fun createOpphorerDato(pendata: Pensjonsinformasjon): String {
        /*
            Kravtype
            (Krav.kravGjelder)
            Opphør
            (Vedtak.vedtakType)
            Annulering
            (Vilkarsvedtak.begrunnelse)
            Antatt virkningsdato
        */
        //TODO mangler kravhode


        logger.debug("6.2       OpphorerDato")
        val kravgjelder = pendata.vedtak.kravGjelder

        return "01-01-1999"
    }


    //6.1..
    private fun createSak(pendata: Pensjonsinformasjon): Sak {

        logger.debug("6      Sak")
        return Sak(

                //6.1 --
                artikkel54  = createArtikkel54(pendata),

                reduksjon = listOf(
                        ReduksjonItem(
                                type = null,
                                virkningsdato  = listOf(
                                        VirkningsdatoItem(
                                                startdato = null,
                                                sluttdato= null
                                        )
                                ),
                                aarsak = Arsak(
                                        inntektAnnen = null,
                                        annenytelseellerinntekt = null
                                ),
                                artikkeltype = null

                        )

                ),
                //$pensjon.sak.kravtype[x]
                kravtype  = listOf(
                        KravtypeItem(

                                //6.5.1 $pensjon.sak.kravtype[x].datoFrist
                                datoFrist = null,

                                krav = null
                        )
                ),
                enkeltkrav = null
        )
    }
    /*
        6.1
        HVIS sakstyper er uføretrygd,
        SÅ skal det velges «[0] No»

        HVIS sakstype er alderspensjon eller gjenlevendepensjon,
        SÅ skal det ikke velges noen.
    */
    private fun createArtikkel54(pendata: Pensjonsinformasjon): String? {
        logger.debug("6.1           createArtikkel54")

        if (KSAK.UFOREP.valid(pendata.sak.sakType)) {
            return "0"
        }
        return null
    }

    private fun isMottarMinstePensjonsniva(pendata: Pensjonsinformasjon): Boolean {
        logger.debug("4.1.2         mottarMinstePensjonsniva")

        val ytelseprmnd = pendata.ytelsePerMaanedListe
        val liste = ytelseprmnd.ytelsePerMaanedListe

        liste.forEach {
            return it.isMottarMinstePensjonsniva
        }
        return false
    }

}

