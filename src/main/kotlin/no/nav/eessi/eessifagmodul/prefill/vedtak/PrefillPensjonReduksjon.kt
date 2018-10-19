package no.nav.eessi.eessifagmodul.prefill.vedtak

import no.nav.eessi.eessifagmodul.models.ReduksjonItem
import no.nav.eessi.eessifagmodul.models.VirkningsdatoItem
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillPensjonReduksjon: PensjonData() {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillPensjonReduksjon::class.java) }

    init {
        logger.debug ("PrefillPensjonReduksjon")
    }

    //5.1
    fun createReduksjon(pendata: Pensjonsinformasjon): List<ReduksjonItem>? {

        logger.debug("5.1       Reduksjon")

        val reduksjon = ReduksjonItem(
                //5.1.1. - $pensjon.reduksjon[x].type
                type  = createReduksjonType(pendata),

                //5.1.3.1 - 5.1.2 - Nei
                aarsak = null, // Arsak(
                        //5.1.3.1 $pensjon`.reduksjon[x].aarsak.inntektAnnen - Nei
                        //inntektAnnen = null,
                        //5.1.2 - Nei!
                        //annenytelseellerinntekt = null
                //),

                //5.1.4 -- $pensjon.sak.reduksjon[x].artikkeltype
                artikkeltype = createReduksjonArtikkelType(pendata),

                //5.1.5 - Nei
                virkningsdato = null //listOf(
                        //createReduksjonDato(pendata)
                //)
        )
        if (reduksjon.type == null && reduksjon.artikkeltype == null) {
            return null
        }

        return listOf(reduksjon)

    }

    //5.1.5
    fun createReduksjonDato(pendata: Pensjonsinformasjon): VirkningsdatoItem {
        logger.debug("5.1.5         ReduksjonDato")
        //Nei
        return VirkningsdatoItem(
                //5.1.5.1  -- Nei
                startdato = null,
                //5.1.5.2  -- Nei
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
    private fun createReduksjonType(pendata: Pensjonsinformasjon): String? {
        logger.debug("5.1.1         ReduksjonType")
        val sakType = PensjonData.KSAK.valueOf(pendata.sak.sakType)

        if (sakType == PensjonData.KSAK.UFOREP && hentTilleggsPensjon(pendata))
            return "02"
        if (sakType == PensjonData.KSAK.GJENLEV && hentGrunnPerson(pendata) || hentTilleggsPensjon(pendata))
            return "02"
        if (sakType == PensjonData.KSAK.BARNEP && hentGrunnPerson(pendata))
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
    private fun createReduksjonArtikkelType(pendata: Pensjonsinformasjon): String? {
        logger.debug("5.1.4         ReduksjonArtikkelType")
        val sakType = PensjonData.KSAK.valueOf(pendata.sak.sakType)

        if (sakType == PensjonData.KSAK.UFOREP && hentVurdertBeregningsmetodeNordisk(pendata))
            return "02"
        if (sakType == PensjonData.KSAK.GJENLEV && hentGrunnPerson(pendata))
            return "02"
        if (sakType == PensjonData.KSAK.BARNEP && hentGrunnPerson(pendata))
            return "02"

        return null
    }


}