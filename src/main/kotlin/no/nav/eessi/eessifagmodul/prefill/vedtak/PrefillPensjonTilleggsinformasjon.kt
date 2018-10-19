package no.nav.eessi.eessifagmodul.prefill.vedtak

import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.utils.simpleFormat
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillPensjonTilleggsinformasjon: PensjonData() {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillPensjonTilleggsinformasjon::class.java) }

    init {
        logger.debug ("PrefillPensjonTilleggsinformasjon")
    }

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
                //TODO Hvor skal vi få denne listen/informasjon ifra? RINA?.
                andreinstitusjoner  = listOf(
                        createAndreinstitusjonerItem(pendata)
                ),

                //6.5.2 - 6.6  $pensjon.tilleggsinformasjon.artikkel48
                //05.10.2018 -
                //TODO Må fylles ut manuelt? i EP-11? eller Rina?
                artikkel48 = createArtikkel48(),

                //6.7.1.4
                //05.10.2018 - Nei
                annen = null, //  Annen(
                        //  $pensjon.tilleggsinformasjon.annen.institusjonsid
                        //institusjonsadresse = Institusjonsadresse())

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

                //6.5.1. Time limits for the review
                dato = pendata.vedtak.datoFattetVedtak.simpleFormat()
        )
    }



    //6.5.2.1
    private fun createAndreinstitusjonerItem(pendata: Pensjonsinformasjon): AndreinstitusjonerItem {
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
    private fun createOpphorerDato(pendata: Pensjonsinformasjon): String? {
        logger.debug("6.2       OpphorerDato")

        val resultatbegrunnelse  = hentVilkarsResultatHovedytelse(pendata)

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

        if ( v1Vedtak.isHovedytelseTrukket && v1Vedtak.kravGjelder == "F_BH_BO_UTL"  ) {
            return v1Vedtak.virkningstidspunkt.simpleFormat()
        }

        if ("ENDR_UTTAKSGRAD" == v1Vedtak.kravGjelder  && "0" == hentYtelseBelop(pendata))
            return v1Vedtak.virkningstidspunkt.simpleFormat()

        return null
    }


}