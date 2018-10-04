package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.Pensjon
import no.nav.eessi.eessifagmodul.models.VedtakItem
import no.nav.eessi.eessifagmodul.services.pensjonsinformasjon.PensjonsinformasjonService
import no.nav.eessi.eessifagmodul.utils.simpleFormat
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.sak.V1Sak
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component


@Component
/**
 * Hjelpe klasse som fyller ut NAV-SED med data fra PESYS.
 * TODO: Nå kun P6000!
 * TODO: Kanskje ha egene klasse for hver SED som henter data fra PESYS da dette kan være for spesefikk pr. SED?
 */
class PrefillPensionDataFromPESYS(private val pensjonsinformasjonService: PensjonsinformasjonService): Prefill<Pensjon> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillPensionDataFromPESYS::class.java) }

    //K_SAK_T Kodeverk fra PESYS
    enum class KSAK(val ksak: String) {
        ALDER("ALDER"),
        UFOREP("UFOREP"),
        GJENLEV("GJENLEV"),
        BARNEP("BARNEP");
        //AFP("AFP"),
        //KRIGSP("KRIGSP");
    }

    override fun prefill(prefillData: PrefillDataModel): Pensjon {

        /*
        TODO: hent alt for en P6000? også andre metoder for hentalt for P2000,P2100 osv..??
        TODO: må ha mulighet for å sende inn saksnummer og vedtakid (hvor får vi vedtakid ifra?)
        */

        //TODO: skal vi på P6000 spørre om et vedtak? eller hentAlt på P6000 og alle valgte vedtak?
        //TODO: se P6000 - 4.1 kan legge inn liste over vedtak (decision)

        val pendata: Pensjonsinformasjon = pensjonsinformasjonService.hentAlt(prefillData.penSaksnummer) // ha med saknr og vedtak?

        return Pensjon(
                vedtak = listOf(
                        createVedtakItem(pendata)
                )
        )

    }

    private fun createVedtakItem(pendata: Pensjonsinformasjon): VedtakItem {

        return VedtakItem(

                //4.1.1  $pensjon.vedtak[x].type
                type = decisionTypePensionWithRule(pendata),

                //4.1.2  $pensjon.vedtak[x].basertPaa
                basertPaa = typeOfPentionWithRule(pendata),

                //4.1.3.1 $pensjon.vedtak[x].basertPaaAnnen
                basertPaaAnnen = null,


                //4.1.6  $pensjon.vedtak[x].virkningsdato
                virkningsdato = pendata.vedtak?.virkningstidspunkt?.simpleFormat(),


                //4.1.8  $pensjon.vedtak[x].kjoeringsdato
                kjoeringsdato = null,

                //$pensjon.vedtak[x].grunnlag
                grunnlag =  null,

                //$pensjon.vedtak[x].artikkel
                artikkel = null,

                trekkgrunnlag = null,

                mottaker =  null,

                begrunnelseAnnen = null,

                ukjent = null,

                resultat = null,

                beregning = null,

                avslagbegrunnelse = null,

                delvisstans = null
        )

    }

    //4.1.1 P6000
    fun decisionTypePensionWithRule(pendata: Pensjonsinformasjon): String {
        //04 og 05 benyttes ikke

        //v1sak fra PESYS
        val v1sak = pendata.sak as V1Sak

        //pensjon før 67
        val uttakfor67 = v1sak.isUttakFor67

        //type fra K_SAK_T
        val type = v1sak.sakType


        val sakType = createKsak(type)
        return when(sakType) {

            KSAK.ALDER -> {
                return when(uttakfor67) {
                    true -> "06"
                    else -> "01"
                }
            }

            KSAK.UFOREP -> "02"

            KSAK.BARNEP, KSAK.GJENLEV -> "03"

        }
    }

    //4.1.2 P6000
    fun typeOfPentionWithRule(pendata: Pensjonsinformasjon): String {
        //01-residece, 02-working, 99-other --> 4.1.3.1 --> other


        val sakType = createKsak(pendata.sak.sakType)

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
    fun typeOfPentionOtherWithRule(pendata: Pensjonsinformasjon): String {
        //"Ytelsen er beregnet etter spesielle beregningsregler for unge uføre"
        return "Ytelsen er beregnet etter regler for barnepensjon"

    }

    private fun isMottarMinstePensjonsniva(pendata: Pensjonsinformasjon): Boolean {

        val ytelseprmnd = pendata.ytelsePerMaanedListe
        val liste = ytelseprmnd.ytelsePerMaanedListe

        liste.forEach {
            return it.isMottarMinstePensjonsniva
        }
        return false
    }

    private fun createKsak(verdi: String): KSAK {
        return KSAK.valueOf(verdi)
    }

}

