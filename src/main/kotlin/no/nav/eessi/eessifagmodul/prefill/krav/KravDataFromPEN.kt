package no.nav.eessi.eessifagmodul.prefill.krav

import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.prefill.PensjonsinformasjonHjelper
import no.nav.eessi.eessifagmodul.prefill.Prefill
import no.nav.eessi.eessifagmodul.prefill.PrefillDataModel
import no.nav.eessi.eessifagmodul.prefill.vedtak.VedtakPensjonData
import no.nav.eessi.eessifagmodul.utils.NavFodselsnummer
import no.nav.eessi.eessifagmodul.utils.simpleFormat
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.sak.V1Sak
import no.nav.pensjon.v1.ytelsepermaaned.V1YtelsePerMaaned
import no.nav.pensjon.v1.ytelseskomponent.V1Ytelseskomponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

//@Component
/**
 * Hjelpe klasse for sak som fyller ut NAV-SED-P2000 med pensjondata fra PESYS.
 */
class KravDataFromPEN(private val dataFromPEN: PensjonsinformasjonHjelper) : VedtakPensjonData(), Prefill<Pensjon> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(KravDataFromPEN::class.java) }

    //K_SAK_T Kodeverk fra PESYS
    enum class KSAK {
        ALDER,
        UFOREP,
        GJENLEV,
        BARNEP;
    }


    fun getPensjoninformasjonFraSak(prefillData: PrefillDataModel): Pensjonsinformasjon {
        return dataFromPEN.hentMedFnr(prefillData)
    }

    fun getPensjonSak(prefillData: PrefillDataModel, pendata: Pensjonsinformasjon): V1Sak {
        return dataFromPEN.hentMedSak(prefillData, pendata)
    }

    override fun prefill(prefillData: PrefillDataModel): Pensjon {
        val pendata: Pensjonsinformasjon = getPensjoninformasjonFraSak(prefillData)
        val pensak = getPensjonSak(prefillData, pendata)

        return Pensjon(

                //4.1
                ytelser = createInformasjonOmYtelserList(prefillData, pensak)


        )

    }

    //4.1
    private fun createInformasjonOmYtelserList(prefillData: PrefillDataModel, pensak: V1Sak): List<YtelserItem> {
        logger.debug("4.1       Informasjon om ytelser")

        val ytelseprmnd = hentYtelsePerMaanedSortert(pensak)

        val ytelselist = mutableListOf<YtelserItem>()
        ytelseprmnd.forEach {
            ytelselist.add(createYtelserItem(prefillData, it, pensak))
        }

        return ytelselist

    }

    //4.1..
    private fun createYtelserItem(prefillData: PrefillDataModel, ytelsek: V1YtelsePerMaaned, pensak: V1Sak): YtelserItem {

        return YtelserItem(

                //4.1.10.2
                totalbruttobeloeparbeidsbasert = ytelsek.belop.toString(),

                //
                institusjon = null, //,

                //4.1.4.1
                pin = createInstitusjonPin(prefillData),

                //4.1.10.1
                mottasbasertpaa = createPensionBasedOn(prefillData, pensak),

                //4.1.1
                ytelse = null,

                //4.1.2.1
                annenytelse = ytelsek.vinnendeBeregningsmetode,

                //4.1.3
                status = createPensionStatus(prefillData, pensak),

                totalbruttobeloepbostedsbasert = null,

                startdatoretttilytelse = null,

                beloep = createYtelseItemBelop(ytelsek, ytelsek.ytelseskomponentListe),

                //TODO hva gjøre en hvis fom og tom er null??
                startdatoutbetaling = ytelsek.fom?.simpleFormat() ?: null,

                sluttdatoretttilytelse = ytelsek.tom?.simpleFormat() ?: null,

                sluttdatoutbetaling = null,


                ytelseVedSykdom = null //7.2 //P2100

        )

    }

    //4.1.3
    /**
    [01] Søkt
    [02] Innvilget
    [03] Avslått
    [04] Foreløpig
     */
    private fun createPensionStatus(prefillData: PrefillDataModel, pensak: V1Sak): String? {
        val status = pensak.status

        if (status == "INNV") {
            return "02" // Innvilget
        }
        return null
    }

    /*

    Fra PSAK. Det må settes opp forretningsregler. Foreløpig forslag:
    Hvis bruker har Dnr, hukes kun av for Working
    Hvis bruker har Fnr:
    Hvis UT: Hvis bruker har minsteytelse, velges kun Residence. Ellers velges både Residence og Working.
    Hvis AP: Hvis bruker mottar tilleggspensjon, velges både Residence og Working. Ellers velges kun Residence.
    Hvis GJP: Hvis bruker mottar tilleggspensjon, velges både Residence og Working. Ellers velges kun Residence.

     */
    //4.1.10.1
    private fun createPensionBasedOn(prefillData: PrefillDataModel, pensak: V1Sak): String? {
        val navfnr = NavFodselsnummer(prefillData.personNr)

        val sakType = KSAK.valueOf(pensak.sakType)

        if (navfnr.harDNummber()) {
            return "01" // Botid
        } else {
            if (sakType == KSAK.ALDER) {
                return "02" // Working
            }
            if (sakType == KSAK.UFOREP) {
                return "01" // Botid
            }
            if (sakType == KSAK.GJENLEV) {
                return "01" // Botid
            }
        }

        return null
    }

    //4.1.4.1
    private fun createInstitusjonPin(prefillData: PrefillDataModel): PinItem {
        logger.debug("4.1.4.1   Institusjon Pin")
        return PinItem(
                //4.1.4.1.3
                sektor = "alle",
                //4.1.4.1.2
                identifikator = prefillData.personNr,
                //4.1.4.1.1
                land = "NO",

                institusjon = Institusjon(
                        institusjonsid = "",
                        institusjonsnavn = ""
                )
        )
    }

    //4.1.9
    private fun createYtelseItemBelop(ytelsek: V1YtelsePerMaaned, ytelsekomp: List<V1Ytelseskomponent>): List<BeloepItem> {

        val list = mutableListOf<BeloepItem>()
        logger.debug("4.1.9     Beløp ${ytelsek.fom.simpleFormat()}")
        ytelsekomp.forEach {
            list.add(BeloepItem(

                    //4.1.9.2
                    valuta = "NOK",

                    //4.1.9.1
                    beloep = it.belopTilUtbetaling.toString(),

                    //4.1.9.3
                    gjeldendesiden = ytelsek.fom.simpleFormat(),

                    //4.1.9.4
                    betalingshyppighetytelse = "03",

                    //4.1.9.5
                    annenbetalingshyppighetytelse = null

            ))

        }
        return list
    }


    private fun hentYtelsePerMaanedSortert(pensak: V1Sak): List<V1YtelsePerMaaned> {
        val ytelseprmnd = pensak.ytelsePerMaanedListe
        val liste = ytelseprmnd.ytelsePerMaanedListe as List<V1YtelsePerMaaned>
        return liste.asSequence().sortedBy { it.fom.toGregorianCalendar() }.toList()
    }


}
