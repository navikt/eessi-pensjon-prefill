package no.nav.eessi.pensjon.prefill.sed.vedtak.helper

import no.nav.eessi.pensjon.eux.model.sed.BasertPaa
import no.nav.eessi.pensjon.eux.model.sed.Grunnlag
import no.nav.eessi.pensjon.eux.model.sed.Opptjening
import no.nav.eessi.pensjon.eux.model.sed.VedtakItem
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P6000MeldingOmVedtakDto
import no.nav.eessi.pensjon.prefill.sed.vedtak.helper.PrefillPensjonVedtaksavslag.createAvlsagsBegrunnelseItem
import no.nav.eessi.pensjon.prefill.sed.vedtak.helper.PrefillPensjonVedtaksavslag.sjekkForVilkarsvurderingListeHovedytelseellerAvslag
import no.nav.eessi.pensjon.prefill.sed.vedtak.helper.PrefillPensjonVedtaksbelop.createBeregningItemList
import no.nav.eessi.pensjon.prefill.sed.vedtak.helper.PrefillPensjonVedtaksbelop.createEkstraTilleggPensjon
import no.nav.eessi.pensjon.prefill.sed.vedtak.helper.VedtakPensjonDataHelper.hentVilkarsResultatHovedytelse
import no.nav.eessi.pensjon.prefill.sed.vedtak.helper.VedtakPensjonDataHelper.hentVinnendeBergeningsMetode
import no.nav.eessi.pensjon.prefill.sed.vedtak.helper.VedtakPensjonDataHelper.isMottarMinstePensjonsniva
import no.nav.eessi.pensjon.utils.simpleFormat
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object PrefillPensjonVedtak {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillPensjonVedtak::class.java) }

    /**
     *  4.1
     */
    fun createVedtakItem(pesysPrefillData: P6000MeldingOmVedtakDto): VedtakItem {
        logger.info("PrefillPensjonReduksjon")
        logger.debug("4.1       VedtakItem")

        return VedtakItem(

                //4.1.1  $pensjon.vedtak[x].type
                type = createVedtakTypePensionWithRule(pesysPrefillData),

                //4.1.2  $pensjon.vedtak[x].basertPaa
                basertPaa = createVedtakGrunnlagPentionWithRule(pesysPrefillData),

                //4.1.3.1 $pensjon.vedtak[x].basertPaaAnnen
                basertPaaAnnen = createVedtakAnnenTypePentionWithRule(pesysPrefillData),

                //4.1.4 $pensjon.vedtak[x].resultat
                resultat = createTypeVedtakPentionWithRule(pesysPrefillData),

                //4.1.6  $pensjon.vedtak[x].virkningsdato
                virkningsdato = pesysPrefillData.vedtak.virkningstidspunkt.simpleFormat(),

                //4.1.7 -- $pensjon.vedtak[x].beregning[x]..
                beregning = createBeregningItemList(pesysPrefillData),

                //4.1.9
                ukjent = createEkstraTilleggPensjon(pesysPrefillData),

                //4.1.10 - 4.1.12 $pensjon.vedtak[x].grunnlag
                grunnlag = createGrunnlag(pesysPrefillData),

                //4.1.13.1 -- 4.1.13.2.1 - $pensjon.vedtak[x].avslagbegrunnelse[x].begrunnelse
                avslagbegrunnelse = createAvlsagsBegrunnelseItem(pesysPrefillData),
        )

    }


    /**
     * 4.1.1 - vedtaktype
     *
     * 01 - Old age
     * 02 - Invalidity
     * 03 - Survivors
     *
     * 04, 05 og 06 benyttes ikke
     */
    fun createVedtakTypePensionWithRule(pendata: P6000MeldingOmVedtakDto): String {
        val sakType = pendata.sakType
        logger.info("4.1.1         VedtakTypePension: $sakType")

        return when (sakType) {
            EessiFellesDto.EessiSakType.ALDER ->  "01"
            EessiFellesDto.EessiSakType.UFOREP -> "02"
            EessiFellesDto.EessiSakType.BARNEP, EessiFellesDto.EessiSakType.GJENLEV -> "03"
            EessiFellesDto.EessiSakType.AFP -> TODO()
            EessiFellesDto.EessiSakType.AFP_PRIVAT -> TODO()
            EessiFellesDto.EessiSakType.FAM_PL -> TODO()
            EessiFellesDto.EessiSakType.GAM_YRK -> TODO()
            EessiFellesDto.EessiSakType.GENRL -> TODO()
            EessiFellesDto.EessiSakType.GRBL -> TODO()
            EessiFellesDto.EessiSakType.KRIGSP -> TODO()
            EessiFellesDto.EessiSakType.OMSORG -> TODO()
        }
    }

    /**
     * 4.1.2 vedtak
     *
     * [01] Based on residence
     * [02] Based on working
     * [99] Other
     *
     */
    private fun createVedtakGrunnlagPentionWithRule(pendata: P6000MeldingOmVedtakDto): BasertPaa? {
        logger.info("4.1.2         VedtakGrunnlagPention")

        val sakType = pendata.sakType
        logger.info("              Saktype: $sakType")

        //hvis avslag returner vi tomt verdi
        if (sjekkForVilkarsvurderingListeHovedytelseellerAvslag(pendata)) return null

        return if (sakType == EessiFellesDto.EessiSakType.BARNEP) BasertPaa.annet
        //TODO: Her må vi sjekke om dette blir riktig
        else {
            when (isMottarMinstePensjonsniva(pendata)) {
                true -> BasertPaa.botid
                false -> BasertPaa.i_arbeid
            }
        }
    }

    /**
     *  4.1.3.1 (other type)
     */
    private fun createVedtakAnnenTypePentionWithRule(pendata: P6000MeldingOmVedtakDto): String? {

        logger.info("4.1.3.1       VedtakAnnenTypePensjon")
        if (createVedtakGrunnlagPentionWithRule(pendata) == BasertPaa.annet) {
            return "Ytelsen er beregnet etter regler for barnepensjon"
        }
        return null
    }

    /**
     *  4.1.4
     *
     *  4.1.[1].4. Type vedtak
     *  [01] Innvilgelse
     *  [02] Avslag
     *  [03] Ny beregning / omregning
     *  [04] Foreløpig utbetaling eller forskudd
     *
     *  HVIS vedtaksresultat er Innvilgelse, OG sakstype IKKE er uføretrygd og kravtype er Førstegangsbehandling Norge/utland ELLER Mellombehandling SÅ skal det hukes for «[01] Award»
     *  HVIS vedtaksresultat er Avslag,  SÅ skal det automatisk hukes for «[02] Rejection»
     *  HVIS kravtype er Revurdering, SÅ skal det hukes for «[03] New calculation / recalculation»
     *  HVIS sakstype er Uføretrygd, OG kravtype er Førstegangsbehandling Norge/utland ELLER Mellombehandling, SÅ skal det hukes for «[04] Provisional or advance payment»
     *  Opphør - må håndteres Se pkt 6.2
     */
    private fun createTypeVedtakPentionWithRule(pendata: P6000MeldingOmVedtakDto): String? {
        logger.info("4.1.4         TypeVedtakPention (vedtak.resultat")

        val sakType = pendata.sakType
        val kravGjelder = pendata.vedtak.kravGjelder

        val vedtaksresultat = hentVilkarsResultatHovedytelse(pendata)
        logger.debug("              vedtaksresultat: $vedtaksresultat")

        val erAvslag = vedtaksresultat == "AVSL"
        val erInnvilgelse = vedtaksresultat == "INNV"

        val erForsteGangBehandlingNorgeUtland = "F_BH_MED_UTL" == kravGjelder
        val erForsteGangBehandlingBosattUtland = "F_BH_BO_UTL" == kravGjelder
        val erMellombehandling = "MELLOMBH" == kravGjelder
        val erRevurdering = kravGjelder == "REVURD"

        if (EessiFellesDto.EessiSakType.UFOREP != sakType && erInnvilgelse
                && (erForsteGangBehandlingNorgeUtland || erMellombehandling || erForsteGangBehandlingBosattUtland)) {
                return "01"
        }
        if (erAvslag)
            return "02"

        if (erRevurdering)
            return "03"

        if (EessiFellesDto.EessiSakType.UFOREP == sakType && (erForsteGangBehandlingNorgeUtland || erMellombehandling))
            return "04"

        if (EessiFellesDto.EessiSakType.UFOREP == sakType && erForsteGangBehandlingBosattUtland)
            return "01"

        logger.debug("              Ingen verdier funnet. (null)")
        return null
    }

    /**
     * 4.1.10 - 4.1.12
     */
    private fun createGrunnlag(pendata: P6000MeldingOmVedtakDto): Grunnlag {

        logger.info("4.1.10        Grunnlag")

        if (sjekkForVilkarsvurderingListeHovedytelseellerAvslag(pendata)) return Grunnlag()

        return Grunnlag(
                //4.1.11
                opptjening = Opptjening(forsikredeAnnen = createOpptjeningForsikredeAnnen(pendata)),

                //4.1.12   $pensjon.vedtak[x].grunnlag.framtidigtrygdetid
                framtidigtrygdetid = createFramtidigtrygdetid(pendata)
        )
    }

    /**
     *  4.1.12. Credited period
     *
     *  [1] Yes
     *  [0] No
     */
    private fun createFramtidigtrygdetid(pendata: P6000MeldingOmVedtakDto): String {
        logger.info("4.1.12        Framtidigtrygdetid ${pendata.sakType}")

        return when (pendata.sakType) {
            EessiFellesDto.EessiSakType.ALDER -> "0"
            else -> {
                "1"
            }
        }
    }

    /**
     *  4.1.11 - OpptjeningForsikredeAnnen
     *
     * [01] - Nei
     * [02] - Delvis
     * [03] - Fullstendig
     *
     */
    private fun createOpptjeningForsikredeAnnen(pendata: P6000MeldingOmVedtakDto): String? {
        logger.info("4.1.11        OpptjeningForsikredeAnnen, sakType: ${pendata.sakType}")

        val sakType = pendata.sakType

        val resultatGjenlevendetillegg = pendata.vilkarsvurderingListe.firstOrNull()?.harResultatGjenlevendetillegg?: false
        val vinnendeMetode = hentVinnendeBergeningsMetode(pendata) ?: ""

        if ((EessiFellesDto.EessiSakType.ALDER == sakType || EessiFellesDto.EessiSakType.UFOREP == sakType) && !resultatGjenlevendetillegg)
            return "01"

        if (EessiFellesDto.EessiSakType.ALDER == sakType && resultatGjenlevendetillegg && vinnendeMetode != "RETT_TIL_GJT")
            return "01"

        if (EessiFellesDto.EessiSakType.ALDER == sakType && resultatGjenlevendetillegg && "RETT_TIL_GJT" == vinnendeMetode)
            return "02"

        if (EessiFellesDto.EessiSakType.GJENLEV == sakType || EessiFellesDto.EessiSakType.BARNEP == sakType) {
            return "03"
        }

        return null
    }
}
