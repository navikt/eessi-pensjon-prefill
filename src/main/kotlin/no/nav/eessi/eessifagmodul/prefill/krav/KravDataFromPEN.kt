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
open class KravDataFromPEN(private val dataFromPEN: PensjonsinformasjonHjelper) : VedtakPensjonData(), Prefill<Pensjon> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(KravDataFromPEN::class.java) }

    //K_SAK_T Kodeverk fra PESYS
    enum class KSAK {
        ALDER,
        UFOREP,
        GJENLEV,
        BARNEP;
    }


    override fun prefill(prefillData: PrefillDataModel): Pensjon {
        val pendata: Pensjonsinformasjon = getPensjoninformasjonFraSak(prefillData)
        val pensak = getPensjonSak(prefillData, pendata)

        return Pensjon(

                //4.0
                ytelser = createInformasjonOmYtelserList(prefillData, pensak)

        )

    }

    //i bruk av tester også
    fun getPensjoninformasjonFraSak(prefillData: PrefillDataModel): Pensjonsinformasjon {
        return dataFromPEN.hentMedFnr(prefillData)
    }

    //i bruk av tester også
    fun getPensjonSak(prefillData: PrefillDataModel, pendata: Pensjonsinformasjon): V1Sak {
        return dataFromPEN.hentMedSak(prefillData, pendata)
    }

    //4.1
    private fun createInformasjonOmYtelserList(prefillData: PrefillDataModel, pensak: V1Sak): List<YtelserItem> {
        logger.debug("4.1           Informasjon om ytelser")

        val ytelseprmnd = hentYtelsePerMaanedSortert(pensak)

        val ytelselist = mutableListOf<YtelserItem>()
        ytelseprmnd.forEach {
            ytelselist.add(createYtelserItem(prefillData, it, pensak))
        }

        return ytelselist

    }

    //4.1..
    private fun createYtelserItem(prefillData: PrefillDataModel, ytelsePrmnd: V1YtelsePerMaaned, pensak: V1Sak): YtelserItem {

        return YtelserItem(

                //4.1.1
                ytelse = creatYtelser(prefillData, pensak),

                //4.1.3
                status = createPensionStatus(prefillData, pensak),

                //4.1.4.1
                pin = createInstitusjonPin(prefillData),

                //4.1.4.1.4
                institusjon = createInstitusjon(prefillData),

                //4.1.10.1
                mottasbasertpaa = createPensionBasedOn(prefillData, pensak),

                //4.1.2.1
                annenytelse = ytelsePrmnd.vinnendeBeregningsmetode,

                //4.1.10.2
                totalbruttobeloepbostedsbasert = null,

                //4.1.10.2
                totalbruttobeloeparbeidsbasert = ytelsePrmnd.belop.toString(),

                //4.1.7
                startdatoretttilytelse = null,

                //4.1.9
                beloep = createYtelseItemBelop(ytelsePrmnd, ytelsePrmnd.ytelseskomponentListe),

                //TODO hva gjøre en hvis fom og tom er null?
                startdatoutbetaling = ytelsePrmnd.fom?.let { it.simpleFormat() },

                sluttdatoretttilytelse = ytelsePrmnd.tom?.let { it.simpleFormat() },

                sluttdatoutbetaling = null,

                ytelseVedSykdom = null //7.2 //P2100

        )

    }


    private fun createInstitusjon(prefillData: PrefillDataModel): Institusjon? {
        logger.debug("4.1.4.1.4     Institusjon")
        return Institusjon(
                institusjonsid = prefillData.andreInstitusjon?.institusjonsid,
                institusjonsnavn = prefillData.andreInstitusjon?.institusjonsnavn,
                saksnummer = prefillData.penSaksnummer
        )

    }


    //4.1.1
    private fun creatYtelser(prefillData: PrefillDataModel, pensak: V1Sak): String? {
        logger.debug("4.1.1         Ytelser")
        val sakType = KSAK.valueOf(pensak.sakType)
        return when (sakType) {
            KSAK.ALDER -> "10"
            KSAK.GJENLEV -> "11"
            KSAK.UFOREP -> "08"
            else -> "07"
        }


    }

    //4.1.3
    /**
    [01] Søkt
    [02] Innvilget
    [03] Avslått
    [04] Foreløpig
     */
    private fun createPensionStatus(prefillData: PrefillDataModel, pensak: V1Sak): String? {
        logger.debug("4.1.3         Status")
        val status = pensak.status
        val temp = prefillData.personNr

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
        logger.debug("4.1.10.1      Pensjon basertpå")
        val navfnr = NavFodselsnummer(prefillData.personNr)

        val sakType = KSAK.valueOf(pensak.sakType)

        if (navfnr.isDNumber()) {
            return "01" // Botid
        }
        return when (sakType) {
            KSAK.ALDER -> "02"
            KSAK.UFOREP -> "01"
            KSAK.GJENLEV -> "01"
            else -> null
        }
    }

    //4.1.4.1
    private fun createInstitusjonPin(prefillData: PrefillDataModel): PinItem {
        logger.debug("4.1.4.1       Institusjon Pin")
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
        logger.debug("4.1.9         Beløp ${ytelsek.fom.simpleFormat()}")
        val list = mutableListOf<BeloepItem>()
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
