package no.nav.eessi.eessifagmodul.models

import java.time.LocalDateTime

data class TrygdehistorikkValg(

        val valgID: Long? = null,
        val valg: String? = null,
        val beskriv: String? = null,
        val aktiv: Boolean? = null

)


data class Trygdehistorikk (

    val trygdehistorikkID: Long? = null,

    val periode: List<TrygdehistorikkPeriode>? = null,

    val opprettetDato: String = LocalDateTime.now().toString(),
    val godkjent: Boolean? = null,
    val godkjentDato: String? = null
)

//P4000
data class TrygdehistorikkPeriode (
        val trygdehistorikkPeriodeID: Long? = null,
        var parent: Trygdehistorikk? = null,

        //hvilke valg er gjort
        val trygdehistorikkValgID: Long? = null,

        //felles dato 4.1.1.2.1.1
        val startdato: String? = null,
        //felles dato 4.1.1.2.1.2
        val sluttdato: String? = null,
        //4.1.1.3 -- 4.2.1.2 -- 4.4.1.2 --
        val usikredato: Boolean? = null,

        //kun for 4.3.1 (omsorg barn)
        val informasjonBarn: InformasjonBarn? = null,

        //kun for 4.1.1 (ansettelsesforhold)
        val informasjonArbeid: InformasjonArbeid? = null,

        //kun for 4.7.1
        val informasjoninstitusjon: Informasjoninstitusjon? = null,

        //felles alle
        //felles informasjon
        val land: String? = null,
        val tilleggsinformasjon: String? = null,

        //ekstra ta med vedlegg her under utfylling
        val vedlegg: List<VedleggItem>? = null,

        //data for oppreting/validering  - database)
        val opprettetDato: String = LocalDateTime.now().toString(),
        val endretDato: String? = null
)

data class InformasjonBarn (
        val fornavn: String? = null,
        val etternavn: String? = null,
        val foedseldato: String? = null
)

data class Informasjoninstitusjon(
        //4.7.1.3.
        val navn: String? = null
)

data class InformasjonArbeid(
    //4.1.1.1
    val type: String? = null,
    //4.1.1.4
    val yrkeaktivitet: String? = null,
    //4.1.1.5
    val forsikring_identifikasjon: String? = null,
    //4.1.1.6
    val selskapnavn: String? = null,

    val gate: String? = null,
    val bygning: String? = null,
    val by: String? = null,
    val region: String? = null
)


data class VedleggItem(
        val navn: String? = null,
        val data: Any? = null,
        val type: String? = null
)

fun createTrygdehistorikkMock(): Trygdehistorikk {

    val historikk = Trygdehistorikk(
        trygdehistorikkID = 10000,

        periode = listOf(
                TrygdehistorikkPeriode(
                        trygdehistorikkPeriodeID = 1001,
                        trygdehistorikkValgID = 1,
                        startdato = LocalDateTime.now().toString(),
                        sluttdato = LocalDateTime.now().toString(),
                        usikredato = false,
                        informasjonArbeid = InformasjonArbeid(
                                type = "Ansettelsesforhold",
                                yrkeaktivitet = "Arbeider",
                                forsikring_identifikasjon = "PBIN ID: 123456789012",
                                selskapnavn = "StoreSelskab AS",
                                gate = "Storeselskabersgate",
                                bygning = "101D",
                                by = "Gåseby",
                                region = "Jylland"
                        ),
                        land = "DK",
                        tilleggsinformasjon = "Det er ikke mye å si mer om dette arbeidfohold i Danmark det var kortvarig",
                        vedlegg = listOf(
                                VedleggItem(
                                        navn = "Ansettelse kontrakt",
                                        data = "@1",
                                        type = "Arbeid"
                                ),
                                VedleggItem(
                                        navn = "Leiekontrakt bosted",
                                        data = "@2",
                                        type = "Arbeid"
                                )
                        )
            ),
            TrygdehistorikkPeriode(
                    trygdehistorikkPeriodeID = 1002,
                    trygdehistorikkValgID = 2,
                    startdato = LocalDateTime.now().toString(),
                    sluttdato = null,
                    usikredato = false,
                    informasjonBarn = InformasjonBarn(
                            fornavn = "fornavn",
                            etternavn = "etternavn",
                            foedseldato = LocalDateTime.now().toString()
                    ),
                    land = "DE",
                    tilleggsinformasjon = "Det er ikke mye å si mer om dette opphold i Tyskland det var kortvarig",
                    vedlegg = listOf(
                            VedleggItem(
                                    navn = "Leiekontrakt bosted",
                                    data = "@2",
                                    type = "Annet"
                            ),
                            VedleggItem(
                                    navn = "Legeerklæring",
                                    data = "@3",
                                    type = "Arbeid"
                            )
                    )
            ),
            TrygdehistorikkPeriode(
                    trygdehistorikkPeriodeID = 1003,
                    trygdehistorikkValgID = 3,
                    startdato = LocalDateTime.now().toString(),
                    sluttdato = LocalDateTime.now().toString(),
                    usikredato = true,
                    land = "SE",
                    informasjoninstitusjon = Informasjoninstitusjon(
                            navn = "Store Svenske Nobel Institutt for Akademiet"
                    ),
                    tilleggsinformasjon = "Det er ikke mye å si mer om dette opphold i Sverige det var kortvarig",
                    vedlegg = listOf(
                            VedleggItem(
                                    navn = "Bostedkontrakt",
                                    data = "@1",
                                    type = "Arbeid"
                            ),
                            VedleggItem(
                                    navn = "Opplærings og undervisnings kontrakt",
                                    data = "@2",
                                    type = "Arbeid"
                           )
                    )
            )
            //end periode
        ),
        godkjent = true,
        godkjentDato = LocalDateTime.now().toString()
    )

    return historikk

}