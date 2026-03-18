package no.nav.eessi.pensjon.prefill.sed.vedtak.helper

import no.nav.eessi.pensjon.eux.model.sed.BasertPaa
import no.nav.eessi.pensjon.eux.model.sed.Grunnlag
import no.nav.eessi.pensjon.eux.model.sed.Opptjening
import no.nav.eessi.pensjon.eux.model.sed.VedtakItem
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiKravGjelder
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiKravGjelder.F_BH_BO_UTL
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiKravGjelder.F_BH_MED_UTL
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiKravGjelder.MELLOMBH
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiKravGjelder.REVURD
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakStatus
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakStatus.AVSL
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakStatus.INNV
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType.AFP
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType.AFP_PRIVAT
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType.ALDER
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType.BARNEP
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType.FAM_PL
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType.GAM_YRK
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType.GENRL
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType.GJENLEV
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType.GRBL
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType.KRIGSP
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType.OMSORG
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType.UFOREP
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
        logger.info("Prefill Pensjon Reduksjon")
        logger.debug("4.1       VedtakItem")

        return VedtakItem(
                //4.1.1  $pensjon.vedtak[x].type
                type = createVedtakTypePensionWithRule(pesysPrefillData),

                //4.1.2  $pensjon.vedtak[x].basertPaa
                basertPaa = createVedtakGrunnlagPensionWithRule(pesysPrefillData),

                //4.1.3.1 $pensjon.vedtak[x].basertPaaAnnen
                basertPaaAnnen = createVedtakAnnenTypePensionWithRule(pesysPrefillData),

                //4.1.4 $pensjon.vedtak[x].resultat
                resultat = createTypeVedtakPensionWithRule(pesysPrefillData),

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
        logger.info("4.1.1         Vedtak TypePensjon: $sakType")

        return when (sakType) {
            ALDER ->  "01"
            UFOREP -> "02"
            BARNEP, GJENLEV -> "03"
            AFP -> TODO()
            AFP_PRIVAT -> TODO()
            FAM_PL -> TODO()
            GAM_YRK -> TODO()
            GENRL -> TODO()
            GRBL -> TODO()
            KRIGSP -> TODO()
            OMSORG -> TODO()
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
    private fun createVedtakGrunnlagPensionWithRule(pendata: P6000MeldingOmVedtakDto): BasertPaa? {
        logger.info("4.1.2         Vedtak Grunnlag Pensjon")

        val sakType = pendata.sakType
        logger.info("              Saktype: $sakType")

        //hvis avslag returner vi tomt verdi
        if (sjekkForVilkarsvurderingListeHovedytelseellerAvslag(pendata)) return null

        return if (sakType == BARNEP) BasertPaa.annet
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
    private fun createVedtakAnnenTypePensionWithRule(pendata: P6000MeldingOmVedtakDto): String? {

        logger.info("4.1.3.1       VedtakAnnenTypePensjon")
        if (createVedtakGrunnlagPensionWithRule(pendata) == BasertPaa.annet) {
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
    private fun createTypeVedtakPensionWithRule(pendata: P6000MeldingOmVedtakDto): String? {
        logger.info("4.1.4         TypeVedtakPension (vedtak.resultat")

        val sakType = pendata.sakType
        val kravGjelder = pendata.vedtak.kravGjelder

        val vedtaksresultat = hentVilkarsResultatHovedytelse(pendata)
        logger.debug("              vedtaksresultat: $vedtaksresultat")

        val erAvslag = vedtaksresultat == AVSL.name
        val erInnvilgelse = vedtaksresultat == INNV.name

        val erForsteGangBehandlingNorgeUtland = F_BH_MED_UTL.name == kravGjelder
        val erForsteGangBehandlingBosattUtland = F_BH_BO_UTL.name == kravGjelder
        val erMellombehandling = MELLOMBH.name == kravGjelder
        val erRevurdering = kravGjelder == REVURD.name

        if (UFOREP != sakType && erInnvilgelse
                && (erForsteGangBehandlingNorgeUtland || erMellombehandling || erForsteGangBehandlingBosattUtland)) {
                return "01"
        }

        if (erAvslag) return "02"
        if (erRevurdering) return "03"
        if (UFOREP == sakType && (erForsteGangBehandlingNorgeUtland || erMellombehandling)) return "04"
        if (UFOREP == sakType && erForsteGangBehandlingBosattUtland) return "01"

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
            ALDER -> "0"
            else -> "1"
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

        val resultatGjenlevendetillegg = pendata.vilkarsvurdering.firstOrNull()?.harResultatGjenlevendetillegg?: false
        val vinnendeMetode = hentVinnendeBergeningsMetode(pendata) ?: ""

        if ((ALDER == sakType || UFOREP == sakType) && !resultatGjenlevendetillegg) return "01"
        if (ALDER == sakType && vinnendeMetode != "RETT_TIL_GJT") return "01"
        if (ALDER == sakType) return "02"
        if (GJENLEV == sakType || BARNEP == sakType) return "03"

        return null
    }
}
