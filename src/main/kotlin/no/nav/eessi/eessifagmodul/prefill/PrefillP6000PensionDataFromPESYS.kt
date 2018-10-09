package no.nav.eessi.eessifagmodul.prefill

import com.google.common.base.Preconditions
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.prefill.kravsak.KravSakFullService
import no.nav.eessi.eessifagmodul.services.pensjonsinformasjon.PensjonsinformasjonService
import no.nav.eessi.eessifagmodul.utils.simpleFormat
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.sak.V1Sak
import no.nav.pensjon.v1.trygdetidliste.V1TrygdetidListe
import no.nav.pensjon.v1.vilkarsvurderinguforetrygd.V1VilkarsvurderingUforetrygd
import no.nav.pensjon.v1.ytelsepermaaned.V1YtelsePerMaaned
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.ZoneId
import java.time.temporal.ChronoUnit


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

    private val kravservice = KravSakFullService()


    fun getPensjoninformasjonFraVedtak(vedtakId: String): Pensjonsinformasjon {
        val pendata: Pensjonsinformasjon = pensjonsinformasjonService.hentAlt(vedtakId) // ha med saknr og vedtak?
        /*
            logger.debug("Pensjonsinformasjon: $pendata")
            logger.debug("Pensjonsinformasjon.vedtak: ${pendata.vedtak}")
            logger.debug("Pensjonsinformasjon.vedtak.virkningstidspunkt: ${pendata.vedtak.virkningstidspunkt}")
            logger.debug("Pensjonsinformasjon.sak: ${pendata.sak}")
            logger.debug("Pensjonsinformasjon.trygdetidListe: ${pendata.trygdetidListe}")
            logger.debug("Pensjonsinformasjon.vilkarsvurderingListe: ${pendata.vilkarsvurderingListe}")
            logger.debug("Pensjonsinformasjon.ytelsePerMaanedListe: ${pendata.ytelsePerMaanedListe}")
            logger.debug("Pensjonsinformasjon.trygdeavtale: ${pendata.trygdeavtale}")
            logger.debug("Pensjonsinformasjon.person: ${pendata.person}")
            logger.debug("Pensjonsinformasjon.person.pin: ${pendata.person.pid}")
        */
        return pendata
    }


    override fun prefill(prefillData: PrefillDataModel): Pensjon {

//        TODO: P6000 krever vedtakId. da den omhandler alt om Vedtaket. (Hvor kommer vedtakid ifra?)
//        TODO: må ha mulighet for å sende inn  vedtakid (hvor får vi vedtakid ifra?)
//        TODO: skal vi på P6000 spørre om et vedtak? eller hentAlt på P6000 og alle valgte vedtak?
//        TODO: se P6000 - 4.1 kan legge inn liste over vedtak (decision)

        logger.debug("----------------------------------------------------------")

        val sedId = prefillData.getSEDid()
        logger.debug("[$sedId] Preutfylling Utfylling Data")

        val vedtakId = if ( prefillData.vedtakId.isNotBlank() ) prefillData.vedtakId else throw IllegalArgumentException("Mangler vedtakID")
        logger.debug(" vedtakId: $vedtakId")

        val starttime = System.currentTimeMillis()
        logger.debug(" henter pensjon data fra PESYS ")

        val pendata = getPensjoninformasjonFraVedtak(prefillData.vedtakId)
        val endtime = System.currentTimeMillis()
        val tottime = endtime - starttime

        logger.debug(" ferdig hentet pensjon data fra PESYS. Det tok $tottime ms")

        if (!harBoddArbeidetUtland(pendata)) throw IllegalArgumentException("Har ikke bod i utlandet! avslutter!")

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
    fun createVedtakItem(pendata: Pensjonsinformasjon): VedtakItem {

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
                virkningsdato = pendata.vedtak.virkningstidspunkt.simpleFormat(),

                //4.1.7 -- $pensjon.vedtak[x].beregning[x]..
                beregning = createBeregningItemList(pendata),

                //4.1.8  $pensjon.vedtak[x].kjoeringsdato -- datoFattetVedtak?
                kjoeringsdato = null, //TODO mangler (Sak.forsteVirkningstidspunkt)

                //4.1.10 - 4.1.12 $pensjon.vedtak[x].grunnlag
                grunnlag =  createGrunnlag(pendata),

                //Na
                mottaker =  null,      //ikke i bruk?
                ukjent = null,         //ikke i bruk?
                trekkgrunnlag = null,  //ikke i bruk?

                //4.1.13.2 Nei
                begrunnelseAnnen = null,

                //4.1.13.1 -- 4.1.13.2.1 - $pensjon.vedtak[x].avslagbegrunnelse[x].begrunnelse
                avslagbegrunnelse = listOf( createAvlsagsBegrunnelseItem(pendata) ),

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
                when(pensjonUttakTidligereEnn67) {
                    true -> "06" //"Førdtispensjon" //06
                    else -> "01"  //""Alderspensjon" //01
                }
            }

            KSAK.UFOREP -> "02" // "Uførhet" //"02"

            KSAK.BARNEP, KSAK.GJENLEV -> "03" // "Etterlatte" //"03"

        }
    }

    //4.1.2
    /*
        HVIS sakstype er Alderspensjon OG beregnet pensjon utgjør minste pensjonsnivå,
        SÅ skal det hukes for [01] Based on residence

        HVIS sakstype er Alderspensjon OG beregnet pensjon er over minste pensjonsnivå,
        SÅ skal det hukes for [02] Based on working

        HVIS sakstype er Uføretrygd og beregnet ytelse er på minstenivå (minsteytelse),
        SÅ skal det hukes for [01] Based on residence (hva hvis det også er egenopptjent UT?)

        HVIS sakstype er Uføretrygd OG beregnet ytelse er  ikke minsteytelse OG bruker ikke er ung ufør,
        SÅ skal det hukes for [02] Based on working

        HVIS sakstype er Uføretrygd OG bruker er ung ufør, OG ja for minsteytelse
        SÅ skal det hukes for [99] Other

        HVIS sakstype er Uføretrygd OG bruker er ung ufør, OG nei for minsteytelse
        SÅ skal det hukes for [02] Based on working

        HVIS sakstype er Gjenlevendepensjon og beregnet ytelse er på minstenivå,
        SÅ skal det hukes for [01] Based on residence

        HVIS sakstype er Gjenlevendepensjon og beregnet ytelse er over minstenivå,
        SÅ skal det hukes for [02] Based on work

        HVIS sakstype er Barnepensjon OG barnet er ikke foreldreløs,
        SÅ skal det hukes for [99] Other

        Det må lages flere regler for UT og for etterlattepensjon
     */
    //4.1.2 P6000
    fun createVedtakGrunnlagPentionWithRule(pendata: Pensjonsinformasjon): String {
        //01-residece, 02-working, 99-other --> 4.1.3.1 --> other

        logger.debug("4.1.2         VedtakGrunnlagPention")
        val sakType = KSAK.valueOf(pendata.sak.sakType)

        return when(sakType) {

            KSAK.ALDER -> {
                println("KsakType: $sakType")
                when(isMottarMinstePensjonsniva(pendata)) {
                    true -> "01"
                    false -> "02"
                }
            }
            KSAK.UFOREP -> {
                println("KsakType: $sakType")
                when(isMottarMinstePensjonsniva(pendata)) {
                    true -> "01"
                    false -> {
                        "02"
                    }
                }
            }
            KSAK.GJENLEV -> {
                println("KsakType: $sakType")
                when(isMottarMinstePensjonsniva(pendata)) {
                    true -> "01"
                    false -> {
                        "02"
                    }
                }
            }
            KSAK.BARNEP -> {
                println("KsakType: $sakType")
                when(isForeldelos(pendata)) {
                    false -> "99"
                    true -> "99"
                }
            }
        }
    }

    //4.1.3.1 (other type)
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
    /*
        HVIS vedtaksresultat er Innvilgelse, OG sakstype IKKE er uføretrygd og kravtype er Førstegangsbehandling Norge/utland ELLER Mellombehandling SÅ skal det hukes for «[01] Award»
        HVIS vedtaksresultat er Avslag,  SÅ skal det automatisk hukes for «[02] Rejection»
        HVIS kravtype er Revurdering, SÅ skal det hukes for «[03] New calculation / recalculation»
        HVIS sakstype er Uføretrygd, OG kravtype er Førstegangsbehandling Norge/utland ELLER Mellombehandling, SÅ skal det hukes for «[04] Provisional or advance payment»
        Opphør - må håndteres Se pkt 6.2
     */
    fun createTypeVedtakPentionWithRule(pendata: Pensjonsinformasjon): String {
        //01> invilg , 02> avslag , 03> ny beregning , 04> foreløping utbeteling
        logger.debug("4.1.4         TypeVedtakPention")

        //IKKE U
        if (KSAK.UFOREP.ksak !=  pendata.sak.sakType) {
            pendata.vedtak.kravVelgType
        }
        return "01"
    }

    //4.1.5
    fun createArtikkelInnvilgelseAvYtelseWithRule(pendata: Pensjonsinformasjon): String {
        //01=> 883/2004 art 52 A, 02=> 883/2004 art 52 B, 03=> 883/2004 art 57 2, 04=> 883/2004 art 57 3, 05=> 833/2004 art 60 2
        logger.debug("4.1.5         ArtikkelInnvilgelseAvYtelse")

        return "01"
    }


    //4.1.7 --
    fun createBeregningItemList(pendata: Pensjonsinformasjon): List<BeregningItem> {

        val ytelsePerMaaned = pendata.ytelsePerMaanedListe.ytelsePerMaanedListe
        val resultList = mutableListOf<BeregningItem>()

        ytelsePerMaaned.forEach {
            resultList.add(createBeregningItem(it))
        }

        return resultList
    }

    private fun createBeregningItem(ytelsePrMnd: V1YtelsePerMaaned): BeregningItem {
        logger.debug("4.1.7         BeregningItem")
        return BeregningItem(

                beloepNetto =  BeloepNetto(
                        beloep = ytelsePrMnd.belop.toString()
                ),

                //4.1.7.4 Currency automatisk hukes for "NOK" norway krone.
                valuta = "NOK",

                beloepBrutto = BeloepBrutto(
                        ytelseskomponentTilleggspensjon = null,
                        beloep = null,
                        ytelseskomponentGrunnpensjon = null,
                        ytelseskomponentAnnen = null
                ),

                //4.1.7.6.1     - Nei
                utbetalingshyppighetAnnen = null  ,

                //4.1.8
                periode = createBeregningItemPeriode(ytelsePrMnd),

                //4.1.7.5                //montly 12/year
                utbetalingshyppighet =  "03"
        )
    }

    //4.1.8
    fun createBeregningItemPeriode(ytelsePrMnd: V1YtelsePerMaaned): Periode {

        logger.debug("4.1.8         BeregningItemPeriode")
        var tomstr: String? = null
        var fomstr: String? = null

        //not needed..
        val fom = ytelsePrMnd.fom
        if (fom != null)
            fomstr = fom.simpleFormat()

        //needed..
        val tom = ytelsePrMnd.tom
        if (tom != null)
            tomstr = tom.simpleFormat()

        return Periode(
                fom = fomstr,
                tom = tomstr
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

                //4.1.12   $pensjon.vedtak[x].grunnlag.framtidigtrygdetid
                framtidigtrygdetid = createFramtidigtrygdetid(pendata)
        )
    }

    //4.1.12 - creditedPeriodIndicator -
    /*
        Hvis sakstype er Uføretrygd ELLER Gjenlevendepensjon ELLER Barnepensjon OG er innvilget OG
        beregningsmetode er "Folketrygd" OG "faktisk trygdetid i Norge" (skjermbilde "Beregne trygdetid") er mindre enn 40 år SÅ skal det velges alternativ "[1] Ja"

        Hvis sakstype er Uføretrygd ELLER Gjenlevendepensjon ELLER Barnepensjon OG er innvilget
        OG beregningsmetode er "Folketrygd" OG "faktisk trygdetid i Norge" (skjermbilde "Beregne trygdetid") er 40 år SÅ skal det velges alternativ "[2] Nei"

        Hvis sakstype er Uføretrygd ELLER Gjenlevendepensjon ELLER Barnepensjon OG er innvilget OG
        beregningsmetode er "EØS Pro rata" SÅ skal det velges alternativ "[2] Nei"

        Hvis sakstype er Alderspensjon SÅ skal det velges alternativ "[2] Nei"
     */
    fun createFramtidigtrygdetid(pendata: Pensjonsinformasjon): String {
        logger.debug("4.1.12        Framtidigtrygdetid")
        val key = "FOLKETRYGD"
        //val key2 = "EØS Pro rata"

        var vinnendeBeregningsmetode = ""
        pendata.ytelsePerMaanedListe.ytelsePerMaanedListe.forEach {
            vinnendeBeregningsmetode = it.vinnendeBeregningsmetode

        }

        val sakType = KSAK.valueOf(pendata.sak.sakType)
        return when(sakType) {
            KSAK.ALDER -> "2" //nei
            else -> {
                when (vinnendeBeregningsmetode) {
                    key -> "1" //ja

                    else -> "2" //nei
                }
            }
        }

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


        return null
    }


    //4.1.13.1 - 4.1.13.2.1
    fun createAvlsagsBegrunnelseItem(pendata: Pensjonsinformasjon): AvslagbegrunnelseItem {

        logger.debug("4.1.13        AvlsagsBegrunnelseItem")
        return AvslagbegrunnelseItem(

                begrunnelse = createAvlsagsBegrunnelse(pendata),

                //4.1.13.2 Other - Nei
                annenbegrunnelse  = null
        )

    }

    //4.1.13.1
    /*
        Hvis sakstype er alderspensjon,
        OG kravhode er markert med «Bodd/arbeidet i utlandet»
        OG skjermbilde Registrere trygdetid viser 00 trygdetid,
        OG skjermbilde Vilkårsprøving viser Avslag
        SÅ skal det velges alternativ «[01]» i feltet 4.1.13.1. Rejection reasons.


        Hvis sakstype er alderspensjon,
        OG kravhode er markert med «Bodd/arbeidet i utlandet»
        OG skjermbilde Registrere trygdetid viser trygdetid 30 > < 360 dager,
        OG skjermbilde Vilkårsprøving viser Avslag
        SÅ skal det velges alternativ «[02]» i feltet 4.1.13.1. Rejection reasons.

        HVIS sakstype er alderspensjon,
        OG Kravhode er markert med «Bodd/arbeidet i utlandet»,
        OG Resultat i Vilkårsprøving viser «Avslag»
        OG Vedtaksbegrunnelse viser «Pensjon for lav v/67 år»
        SÅ skal det velges alternativ «[03] Krav til perioden eller andre kvalifiseringskrav er ikke oppfylt» i feltet 4.1.13.1. Rejection reasons.


        HVIS sakstype er alderspensjon,
        OG Kravhode er markert med «Bodd/arbeidet i utlandet»,
        OG Resultat i Vilkårsprøving viser «Avslag»
        OG Vedtaksbegrunnelse viser «Bruker er under 62 år»
        SÅ skal det velges alternativ «[06] Pension age not yet reached» i feltet 4.1.13.1. Rejection reasons.


        Hvis sakstype er uføretrygd,
        OG kravhode er markert med «Bodd/arbeidet i utlandet»
        OG skjermbilde Registrere trygdetid viser 00 trygdetid,
        OG skjermbilde Vilkårsvurdering uføre viser Nei i «12-2 Forutgående medlemskap»,
        OG skjermbilde Vilkårsprøving viser Avslag
        SÅ skal det velges alternativ «[01]» i feltet 4.1.13.1. Rejection reasons.


        Hvis sakstype er uføretrygd,
        OG kravhode er markert med «Bodd/arbeidet i utlandet»
        OG skjermbilde Registrere trygdetid viser trygdetid 30 > < 360 dager,
        OG skjermbilde Vilkårsvurdering uføre viser Nei i «12-2 Forutgående medlemskap»,
        OG skjermbilde Vilkårsprøving viser Avslag
        SÅ skal det velges alternativ «[02]» i feltet 4.1.13.1. Rejection reasons.


        Hvis sakstype er uføretrygd,
        OG kravhode er markert med «Bodd/arbeidet i utlandet»
        OG skjermbilde Vilkårsvurdering uføre viser "Ikke opppfylt" i «§ 12-7, 12-17 Nedsatt inntektsevne»
        OG skjermbilde Vilkårsprøving viser Avslag
        SÅ skal det velges alternativ «[04]» i feltet 4.1.13.1. Rejection reasons.


        HVIS sakstype er uføretrygd,
        OG kravhode er markert med «Bodd/arbeidet i utlandet»
        OG skjermbilde Vilkårsvurdering uføre viser "Ikke opppfylt" i «§ 12-5 Hensiktsmessig behandling» OG/ELLER «§ 12-5 Hensiktsmessige arbeidsrettede tiltak» OG/ELLER "§ 12-6 Sykdom, skade eller lyte - krav til årsakssammenheng"
        OG skjermbilde Vilkårsprøving viser Avslag
        SÅ skal det velges alternativ «[08]» i feltet 4.1.13.1. Rejection reasons.

        HVIS sakstype er uføretrygd,
        OG kravhode er markert med «Bodd/arbeidet i utlandet»
        OG skjermbilde Vilkårsvurdering uføre viser "Ikke opppfylt" i «§ 12-4 Alder, samt vurdering ved krav fremsatt etter fylte 62 år"
        OG skjermbilde Vilkårsprøving viser Avslag
        SÅ skal det velges alternativ «[03]» i feltet 4.1.13.1. Rejection reasons.

        HVIS sakstype er barnepensjon eller gjenlevendeytelse,
        OG kravhode er markert med «Bodd/arbeidet i utlandet»
        OG skjermbilde Registrere trygdetid viser 00 trygdetid på både gjenlevende og avdøde,
        OG skjermbilde Vilkårsprøving viser Avslag
        SÅ skal det velges alternativ «[01]» i feltet 4.1.13.1. Rejection reasons.


        Hvis sakstype er barnepensjon eller gjenlevendeytelse,
        OG kravhode er markert med «Bodd/arbeidet i utlandet»
        OG skjermbilde Registrere trygdetid viser trygdetid 30 > < 36 dager på både gjenlevende og avdøde,
        OG skjermbilde Vilkårsprøving viser Avslag
        SÅ skal det velges alternativ «[02]» i feltet 4.1.13.1. Rejection reasons.


        HVIS sakstype er alderspensjon, uføretrygd?, barnepensjon eller gjenlevendeytelse,
        Og kravhode er markert med «Bodd/arbeidet i utlandet»,
        OG resultat i Vilkårsprøving viser «Avslag»,
        OG Vedtaksbegrunnelse viser «Ikke mottatt dokumentasjon»,
        SÅ skal det velges alternativ «[07] Manglende informasjon fra søkeren» i feltet 4.1.13.1. Rejection reasons.

     */
    fun createAvlsagsBegrunnelse(pendata: Pensjonsinformasjon): String? {
        //Preconditions.checkArgument(pendata.vilkarsvurderingListe != null, "VilkarsvurderingListe er null")
        //Preconditions.checkArgument(pendata.vilkarsvurderingListe.vilkarsvurderingListe != null, "VilkarsvurderingListe er null")
        if (pendata.vilkarsvurderingListe == null || pendata.vilkarsvurderingListe.vilkarsvurderingListe == null) {
            return null
        }

        logger.debug("4.1.13.1          AvlsagsBegrunnelse")

        val sakType = pendata.sak.sakType
        val harBoddArbeidetUtland = harBoddArbeidetUtland(pendata)
        val erTrygdetidListeTom = pendata.trygdetidListe.trygdetidListe.isEmpty()

        val erLAVT_TIDLIG_UTTAK = isVilkarsvurderingAvslagHovedytelseSamme("LAVT_TIDLIG_UTTAK", pendata)
        val erUNDER_62 = isVilkarsvurderingAvslagHovedytelseSamme("UNDER_62", pendata)
        val erIKKE_MOTTATT_DOK = "IKKE_MOTTATT_DOK" == hentVilkarsProvingAvslagHovedYtelse(pendata)
        val erFORUT_MEDL = "FORUT_MEDL" == hentVilkarsvurderingUforetrygd(pendata).unntakForutgaendeMedlemskap
        val erHENS_ARBRETT_TILTAK =  "HENS_ARBRETT_TILTAK" == hentVilkarsvurderingUforetrygd(pendata).hensiktsmessigArbeidsrettedeTiltak
        val erHENSIKTSMESSIG_BEH = "HENSIKTSMESSIG_BEH" == hentVilkarsvurderingUforetrygd(pendata).hensiktsmessigBehandling
        val erNEDSATT_INNT_EVNE = "NEDSATT_INNT_EVNE" == hentVilkarsvurderingUforetrygd(pendata).nedsattInntektsevne
        val erALDER = "ALDER" == hentVilkarsvurderingUforetrygd(pendata).alder

        //TODO: Hvor finner man verdien Vilkårsprøving viser «Avslag»!!!!
        val avslag_vilkarsproving = true // Må hentes fra VilkarsProvingListe (som vi få inn i systemet)

        //pkt1 og pkt.9
        if ((KSAK.ALDER.valid(sakType) || KSAK.BARNEP.valid(sakType) || KSAK.GJENLEV.valid(sakType))  && harBoddArbeidetUtland && erTrygdetidListeTom && avslag_vilkarsproving)
            return "01"

        //pkt.2 og pkt.10
        if ((KSAK.ALDER.valid(sakType) || KSAK.BARNEP.valid(sakType) || KSAK.GJENLEV.valid(sakType)) && harBoddArbeidetUtland && erTrygdeTid(pendata) && avslag_vilkarsproving)
            return "02"

        if (KSAK.ALDER.valid(sakType) && harBoddArbeidetUtland && erLAVT_TIDLIG_UTTAK && avslag_vilkarsproving)
            return "03"

        if (KSAK.ALDER.valid(sakType) && harBoddArbeidetUtland && erUNDER_62 && avslag_vilkarsproving)
            return "06"
        //hentVilkarsvurderingUforetrygd
        if (KSAK.UFOREP.valid(sakType) && harBoddArbeidetUtland && erALDER && avslag_vilkarsproving)
            return "03"

        if (KSAK.UFOREP.valid(sakType) && harBoddArbeidetUtland && (erHENSIKTSMESSIG_BEH || erHENS_ARBRETT_TILTAK) && avslag_vilkarsproving)
            return "08"
        //
        if (KSAK.UFOREP.valid(sakType) && harBoddArbeidetUtland && erNEDSATT_INNT_EVNE && avslag_vilkarsproving)
            return "04"
        //
        if (KSAK.UFOREP.valid(sakType) && harBoddArbeidetUtland && erTrygdeTid(pendata) && erFORUT_MEDL && avslag_vilkarsproving)
            return "02"
        //pkt.5
        if (KSAK.UFOREP.valid(sakType) && harBoddArbeidetUtland && pendata.trygdetidListe.trygdetidListe.isEmpty() && erFORUT_MEDL && avslag_vilkarsproving)
            return "01"

        //siste..   pendata.sak.sakType alle..
        if (harBoddArbeidetUtland && erIKKE_MOTTATT_DOK && avslag_vilkarsproving)
            return "07"


        logger.debug("              -- Ingen avslagsbegrunnelse")
        return null
    }

    fun harBoddArbeidetUtland(pendata: Pensjonsinformasjon): Boolean {
        Preconditions.checkArgument(pendata.vedtak != null, "Vedtak er null")
        return pendata.vedtak.isBoddArbeidetUtland
    }

    fun isVilkarsvurderingAvslagHovedytelseSamme(key: String, pendata: Pensjonsinformasjon): Boolean {
        return key == hentVilkarsProvingAvslagHovedYtelse(pendata)
    }

    //TODO - summere opp i ant. dager . trygdetidListe.fom - tom.
    fun erTrygdeTid(pendata: Pensjonsinformasjon, storreEnn: Int=30, mindreEnn: Int=360): Boolean {
        Preconditions.checkArgument(pendata.trygdetidListe != null, "trygdetidListe er Null")
        Preconditions.checkArgument(pendata.trygdetidListe.trygdetidListe != null, "trygdetidListe er Null")
        val trygdeListe = pendata.trygdetidListe
        val days = summerTrygdeTid(trygdeListe)
        //trygdetid viser trygdetid 30 > < 360 dager
        println("$storreEnn > $days && $days < $mindreEnn")
        //days =  70 -> 30 > 70 && 70 < 360   - true
        //days =  15 -> 30 > 15 && 15 < 360   - false
        //days = 500 -> 30 > 500 && 500 < 360 - false
        return days > storreEnn && days < mindreEnn
    }

    fun summerTrygdeTid(trygdeListe: V1TrygdetidListe): Int {
        Preconditions.checkArgument(trygdeListe.trygdetidListe != null, "trygdetidListe er Null")
        var days: Long = 0
        trygdeListe.trygdetidListe.forEach {
            val fom = it.fom.toGregorianCalendar().time.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val tom = it.tom.toGregorianCalendar().time.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            days += ChronoUnit.DAYS.between(fom,tom)
        }
        return days.toInt()
    }


    fun hentVilkarsvurderingUforetrygd(pendata: Pensjonsinformasjon): V1VilkarsvurderingUforetrygd {
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.forEach {
            return it.vilkarsvurderingUforetrygd //
        }
        return V1VilkarsvurderingUforetrygd()
    }

    //TODO -         //Kodeverk K_RESULT_BEGR 2017
    fun hentVilkarsProvingAvslagHovedYtelse(pendata: Pensjonsinformasjon): String {
        pendata.vilkarsvurderingListe.vilkarsvurderingListe.forEach {
            return it.avslagHovedytelse // UNDER_62 -- LAVT_TIDLIG_UTTAK osv..
        }
        return ""
    }

    //5.1
    fun createReduksjon(pendata: Pensjonsinformasjon): ReduksjonItem {

        logger.debug("5.1       Reduksjon")

        return ReduksjonItem(
            //5.1.1. - $pensjon.reduksjon[x].type
            type  = createReduksjonType(pendata),


            aarsak = Arsak(
                //5.1.3.1 $pensjon`.reduksjon[x].aarsak.inntektAnnen - Nei
                inntektAnnen = null,
                //5.1.2 - Nei!
                annenytelseellerinntekt = null
            ),

            //5.1.4 -- $pensjon.sak.reduksjon[x].artikkeltype
            artikkeltype = createReduksjonArtikkelType(pendata),

            //5.1.5 - Nei
            virkningsdato = listOf(
                createReduksjonDato(pendata)
            )

        )

    }

    //5.1.5
    fun createReduksjonDato(pendata: Pensjonsinformasjon): VirkningsdatoItem {
        logger.debug("5.1.5         ReduksjonDato")
        //Nei
        return VirkningsdatoItem(
                //5.1.5.1
                startdato = null,
                //5.1.5.2
                sluttdato = null
        )
    }

    //5.1.1
    /*
        HVIS Sakstype er Uføretrygd
        OG Vilkårsprøving Detaljer Trygdeavtale
        Skal artikkel 10 anvendes på trygdetid
        SÅ skal det hukes av for "[02] Ytelse som fastsettes på grunnlag av en godskrevet periode"

        HVIS Sakstype er Gjenlevendepensjon
        OG Vilkårsprøving Detaljer Trygdeavtale Skal artikkel 10 anvendes på grunnpensjon
        OG/ELLER  Skal artikkel 10 anvendes på tilleggspensjon
        SÅ skal det hukes av for "[02] Ytelse som fastsettes på grunnlag av en godskrevet periode"

        HVIS Sakstype er barnepensjon
        OG Vilkårsprøving Detaljer Trygdeavtale Skal artikkel 10 anvendes på grunnpensjon
        SÅ skal det hukes av for "[02] Ytelse som fastsettes på grunnlag av en godskrevet periode"
     */
    fun createReduksjonType(pendata: Pensjonsinformasjon): String? {
        logger.debug("5.1.1         ReduksjonType")

        val sakType = KSAK.valueOf(pendata.sak.sakType)

        if (KSAK.UFOREP==sakType)
            return "02"
        if (KSAK.GJENLEV==sakType)
            return "02"
        if (KSAK.BARNEP==sakType)
            return "02"

        return null
    }

    //5.1.4
    /*
        Hvis sakstype er Uføretrygd,
        OG det er svart JA i  Vilkårsprøving/Detaljer trygdeavtale/Nordisk trygdeavtale/Skal artikkel 10 anvendes på trygdetid,
        SÅ skal det hukes for «[02] 883/2004: Art. 54(2)b»

        HVIS Sakstype er Gjenlevendepensjon
        OG Vilkårsprøving Detaljer Trygdeavtale Skal artikkel 10 anvendes på grunnpensjon
        OG/ELLER  Skal artikkel 10 anvendes på tilleggspensjon
        SÅ skal det hukes av for "[02] 883/2004: Art. 54(2)b»

        HVIS Sakstype er barnepensjon
        OG Detaljer trygdeavtale Skal artikkel 10 anvendes på grunnpensjon
        SÅ skal det hukes av for "[02] 883/2004: Art. 54(2)b»
     */
    fun createReduksjonArtikkelType(pendata: Pensjonsinformasjon): String? {
        logger.debug("5.1.4         ReduksjonArtikkelType")
        val sakType = KSAK.valueOf(pendata.sak.sakType)

        if (sakType == KSAK.UFOREP)
            return "02"
        if (sakType == KSAK.GJENLEV)
            return "02"
        if (sakType == KSAK.BARNEP)
            return "02"

        return null
    }




    //pr 6.5.1
    private val timelimit_6_5_1 = "six weeks from the date the decision is received"


    //6.2
    fun createTilleggsinformasjon(pendata: Pensjonsinformasjon): Tilleggsinformasjon {

        logger.debug("6.2           Tilleggsinformasjon")
        return Tilleggsinformasjon(

            //6.2-6.3
            opphoer = Opphoer(
                    //6.2
                    dato = createOpphorerDato(pendata),
                    //6.3
                    annulleringdato = createAnnulleringDato(pendata)
            ),

            //6.5.2.1
            //På logges EnhetID /NAV avsender (PENSJO, UTFORP, ETTERLATP)?
            andreinstitusjoner  = listOf(
                    AndreinstitusjonerItem()
            ),

            //6.5.2 - 6.6  $pensjon.tilleggsinformasjon.artikkel48
            //05.10.2018 -
            //TODO Må fylles ut manuelt? i EP-11? eller Rina?
            artikkel48 = createArtikkel48(),

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

            //??
            person = Person(),

            //
            dato = null
        )
    }

    //6.5.2.1
    private fun createAndreinstitusjonerItem(): AndreinstitusjonerItem {
        //På logges EnhetID /NAV avsender (PENSJO, UTFORP, ETTERLATP)?
        logger.debug("6.5.2.1       AndreinstitusjonerItem")
        return AndreinstitusjonerItem(
            institusjonsid = "NAV",
            institusjonsnavn  = "NAV",
            institusjonsadresse  = null,
            postnummer  = null,
            bygningsnr = null,
            land = null,
            region = null,
            poststed = null
        )
    }

    //6.6
    /*
        05.10.2018
        Må fylles ut manuelt
        TODO Må fylles ut manuelt? i EP-11? eller Rina?

        6.6  $pensjon.tilleggsinformasjon.artikkel48
        6.6. The decision has been given as a result of the review according to the Art. 48(2) of Regulation 987/2009
     */
    private fun createArtikkel48(): String? {
        logger.debug("6.6           Artikkel48  (Må fylles ut manuelt!!)")
        return null
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
        logger.debug("6.2       OpphorerDato")

        //val kravgjelder = pendata.vedtak.kravGjelder
        return "01-01-1999"
    }

    /**
        HVIS kravtype er Førstegangsbehandling,
        OG status på kravet er endret fra «Til behandling» til «Trukket»,
        SÅ skal Antatt virkningsdato vises her.

        HVIS kravtype er Endring uttaksgrad,
        OG uttaksgrad er endret til null,
        OG vedtak er attestert,
        SÅ skal Antatt virkningsdato vises her.
        Datoen skal vises i formatet DD-MM-YYYY
     */
    //6.3
    private fun createAnnulleringDato(pendata: Pensjonsinformasjon): String {
        logger.debug("6.x       AnnulleringDato")

        return "YYYY-MM-DD"
    }


    //6.1..
    private fun createSak(pendata: Pensjonsinformasjon): Sak {

        logger.debug("6         Sak")
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
        logger.debug("6.1       createArtikkel54")

        if (KSAK.UFOREP.valid(pendata.sak.sakType)) {
            return "0"
        }
        return null
    }

    fun isForeldelos(pendata: Pensjonsinformasjon): Boolean {
        val avdodpinfar : String? = pendata.avdod.avdodFar ?: "INGEN"
        val avdodpinmor : String? = pendata.avdod.avdodMor ?: "INGEN"
        if (avdodpinfar != "INGEN" && avdodpinmor != "INGEN") {
            return true
        }
        return false
    }


    //hjelpe funkjson for isMottarMinstePensjonsniva

    //Uføretrygd og beregnet ytelse er på minstenivå (minsteytelse)
    private fun isMottarMinstePensjonsniva(pendata: Pensjonsinformasjon): Boolean {
        logger.debug(" +            isMottarMinstePensjonsniva")

        val ytelseprmnd = pendata.ytelsePerMaanedListe
        val liste = ytelseprmnd.ytelsePerMaanedListe

        liste.forEach {
            return it.isMottarMinstePensjonsniva
        }
        return false
    }

}

