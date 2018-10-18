package no.nav.eessi.eessifagmodul.prefill.P6000

import no.nav.eessi.eessifagmodul.models.*
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillPensjonSak: PensjonData() {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillPensjonSak::class.java) }

    init {
        logger.debug ("PrefillPensjonReduksjon")
    }

    //6.1..
    fun createSak(pendata: Pensjonsinformasjon): Sak {

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
                                datoFrist = "six weeks from the date the decision is received",

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
        val ksakUfor = KSAK.valueOf(pendata.sak.sakType)

        if (KSAK.UFOREP == ksakUfor) {
            return "0"
        }
        return null
    }

}