package no.nav.eessi.pensjon.fagmodul.prefill.vedtak

import no.nav.eessi.pensjon.fagmodul.models.AndreinstitusjonerItem
import no.nav.eessi.pensjon.fagmodul.models.Opphoer
import no.nav.eessi.pensjon.fagmodul.models.Tilleggsinformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.utils.simpleFormat
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillPensjonTilleggsinformasjon : VedtakPensjonData() {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillPensjonTilleggsinformasjon::class.java) }

    init {
        logger.debug("PrefillPensjonTilleggsinformasjon")
    }

    //6.2
    fun createTilleggsinformasjon(pendata: Pensjonsinformasjon, prefillData: PrefillDataModel): Tilleggsinformasjon {

        logger.debug("6.2           Tilleggsinformasjon")
        return Tilleggsinformasjon(

                //6.2-6.3
                opphoer = createOpphoer(pendata),

                //6.5.2.1
                //På logges EnhetID /NAV avsender (PENSJO, UTFORP, ETTERLATP)?
                andreinstitusjoner = createAndreinstitusjonerItem(pendata, prefillData),

                //6.5.2 - 6.6  $pensjon.tilleggsinformasjon.artikkel48
                //05.10.2018 -
                //TODO Må fylles ut manuelt? i EP-11? eller Rina?
                artikkel48 = createArtikkel48(),

                //6.7.1.4
                //05.10.2018 - Nei
                annen = null,

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
                person = null, //Person()

                //6.4 //Vedtakets utstedelsesdato (angitt på vedtaket
                dato = createTilleggsInfoDato(pendata)
        )
    }

    private fun createOpphoer(pendata: Pensjonsinformasjon): Opphoer? {
        logger.debug("6.2       Opphører..")

        val dato = createOpphorerDato(pendata)
        val annulleringdato = createAnnulleringDato(pendata)

        val opphorer = Opphoer(
                //6.2
                dato = dato,
                //6.3
                annulleringdato = annulleringdato
        )

        if (dato == null && annulleringdato == null) {
            return null
        }
        return opphorer
    }

    private fun createTilleggsInfoDato(pendata: Pensjonsinformasjon): String {
        //6.4 //
        logger.debug("6.4       Tilleggsinformasjon dato (dato vedtak)")
        return pendata.vedtak.datoFattetVedtak.simpleFormat()

    }


    //6.5.2.1
    private fun createAndreinstitusjonerItem(pendata: Pensjonsinformasjon, prefillData: PrefillDataModel): List<AndreinstitusjonerItem>? {
        logger.debug("6.5.2.1       AndreinstitusjonerItem (review address)")
        val data = prefillData.andreInstitusjon ?: return null
        return listOf(data)
        //return null
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
    private fun createOpphorerDato(pendata: Pensjonsinformasjon): String? {
        logger.debug("6.2       OpphorerDato")

        val resultatbegrunnelse = hentVilkarsResultatHovedytelse(pendata)

        if ("REVURD" == pendata.vedtak.kravGjelder && "ANNULERING" == resultatbegrunnelse)
            return pendata.vedtak.virkningstidspunkt.simpleFormat()

        return null
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
    private fun createAnnulleringDato(pendata: Pensjonsinformasjon): String? {
        logger.debug("6.x       AnnulleringDato")
        val v1Vedtak = pendata.vedtak

        if (v1Vedtak.isHovedytelseTrukket && v1Vedtak.kravGjelder == "F_BH_BO_UTL") {
            return v1Vedtak.virkningstidspunkt.simpleFormat()
        }

        if ("ENDR_UTTAKSGRAD" == v1Vedtak.kravGjelder && "0" == hentYtelseBelop(pendata))
            return v1Vedtak.virkningstidspunkt.simpleFormat()

        return null
    }


}