package no.nav.eessi.eessifagmodul.models

import java.time.LocalDateTime


data class PersonTrygdeTid(
        val andrePerioder: List<StandardItem>? = null,
        val arbeidsledigPerioder: List<StandardItem>? = null,
        val boPerioder: List<StandardItem>? = null,
        val opplearingPerioder: List<StandardItem>? = null,
        val sykePerioder: List<StandardItem>? = null,
        val barnepassPerioder: List<BarnepassItem>? = null,
        val ansattSelvstendigPerioder: List<AnsattSelvstendigItem>? = null,
        val forsvartjenestePerioder: List<StandardItem>? = null,
        val foedselspermisjonPerioder: List<StandardItem>? = null,
        val frivilligPerioder: List<StandardItem>? = null
)
data class TrygdeTidPeriode (
        //med sluttdato
        val lukketPeriode: Periode? = null,
        //uten sluttdato
        val openPeriode: Periode? = null
)
data class AnsattSelvstendigItem (
        val jobbUnderAnsattEllerSelvstendig: String? = null,
        val annenInformasjon: String? = null,
        val adresseFirma: Adresse? = null,
        val periode: TrygdeTidPeriode? = null,
        val forsikkringEllerRegistreringNr: String? = null,
        val navnFirma: String? = null,
        val typePeriode: String? = null,
        val usikkerDatoIndikator: String? = null
)
data class BarnepassItem(
        val annenInformasjon: String? = null,
        val informasjonBarn: InformasjonBarn? = null,
        val periode: TrygdeTidPeriode? = null,
        val usikkerDatoIndikator: String? = null
)
data class InformasjonBarn (
        val fornavn: String? = null,
        val land: String? = null,
        val etternavn: String? = null,
        val foedseldato: String? = null
)
data class StandardItem (
        val land: String? = null,
        val annenInformasjon: String? = null,
        val periode: TrygdeTidPeriode? = null,
        val usikkerDatoIndikator: String? = null,
        //arbeidsledig -- //opplæring -- //sykedom
        val navnPaaInstitusjon: String? = null,
        //hva er denne til igjen?
        //hva var perioden i andreTyper? hva gjore du?
        val typePeriode: String? = null
)

fun createPersonTrygdeTidMock(): PersonTrygdeTid {

    val personTrygdeTid = PersonTrygdeTid(
            foedselspermisjonPerioder = listOf(
                    StandardItem(
                            land = "NO",
                            usikkerDatoIndikator = "NO",
                            annenInformasjon= "førdeslperm i Norge",
                            periode = TrygdeTidPeriode(
                                        lukketPeriode = Periode(
                                                fom = "2000-01-01",
                                                tom = "2001-01-01"
                                        )
                            )
                    ),
                    StandardItem(
                            land = "FR",
                            usikkerDatoIndikator = "YES",
                            annenInformasjon= "fødselperm i frankrike",
                            periode = TrygdeTidPeriode(
                                    openPeriode = Periode (
                                            fom = "2002-01-01",
                                            extra = "endDatePeriodUnknown"
                                    )
                            )

                    )
            ),
            ansattSelvstendigPerioder = listOf(
                    AnsattSelvstendigItem(
                            typePeriode = "Employee",
                            jobbUnderAnsattEllerSelvstendig = "Kanin fabrikk ansatt",
                            annenInformasjon = "Noting else",
                            adresseFirma = Adresse(
                                    gate = "foo",
                                    postnummer = "23123",
                                    bygning = "Bygg",
                                    region = "Region",
                                    land = "NO",
                                    by = "Oslo"
                            ),
                            periode = TrygdeTidPeriode (
                                    lukketPeriode = Periode (
                                            tom = "1995-01-01",
                                            fom = "1990-01-01"
                                    )
                            ),
                            navnFirma = "Store Kaniner AS",
                            forsikkringEllerRegistreringNr = "12123123123123123",
                            usikkerDatoIndikator = "NO"
                    )
            ),
            andrePerioder = listOf(
                    StandardItem(
                            land = "SE",
                            usikkerDatoIndikator = "NO",
                            annenInformasjon= "ikkenoe",
                            typePeriode = "Ingen spesielt",
                            periode = TrygdeTidPeriode (
                                    lukketPeriode = Periode (
                                            fom = "2000-01-01",
                                            tom = "2001-01-01"
                                    )
                            )
                    ),
                    StandardItem(
                            land = "SE",
                            usikkerDatoIndikator = "NO",
                            annenInformasjon= "ikkenoemere",
                            typePeriode = "Leve og ha det gøy",
                            periode = TrygdeTidPeriode(
                                    openPeriode = Periode (
                                            fom = "2000-01-01",
                                            extra = "endDatePeriodUnknown"
                                    )
                            )
                    )
            ),
            boPerioder = listOf(
                    StandardItem(
                            land = "DK",
                            usikkerDatoIndikator = "NO",
                            annenInformasjon = "Deilig i Danmark",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            fom = "01-01-2003",
                                            tom = "01-01-2004"
                                    )
                            )
                    )
            ),
            arbeidsledigPerioder = listOf(
                    StandardItem(
                            land = "IT",
                            usikkerDatoIndikator = "NO",
                            annenInformasjon = "Arbeidsledig i Itelia for en kort periode.",
                            navnPaaInstitusjon = "NAV stønad for arbeidsledigetstrygd",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            fom = "01-01-2002",
                                            tom = "01-01-2004"
                                    )
                            )

                    )
            ),
            forsvartjenestePerioder = listOf(
                    StandardItem(
                            land = "SE",
                            usikkerDatoIndikator = "NO",
                            annenInformasjon = "Forsvar og mlitærtjeneste fullført i Svergige",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            fom = "01-01-2002",
                                            tom = "01-01-2004"
                                    )
                            )

                    )
            ),
            sykePerioder = listOf(
                    StandardItem(
                            land = "ES",
                            usikkerDatoIndikator = "NO",
                            annenInformasjon = "Sykdom og forkjølelse i Spania",
                            navnPaaInstitusjon = "Støtte for sykeophold NAV",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            fom = "01-01-2005",
                                            tom = "01-01-2007"
                                    )
                            )

                    )

            ),
            frivilligPerioder = listOf(
                    StandardItem(
                            land = "GR",
                            usikkerDatoIndikator = "NO",
                            annenInformasjon = "Frivilig hjelpemedarbeider i Helles",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            fom = "01-01-2006",
                                            tom = "01-01-2007"
                                    )
                            )

                    )
            ),
            opplearingPerioder = listOf(
                    StandardItem(
                            land = "SE",
                            usikkerDatoIndikator = "NO",
                            annenInformasjon = "Opplæring høyere utdanning i Sverige",
                            navnPaaInstitusjon = "Det Akademiske instutt for høgere lære, Stockholm",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            fom = "01-01-2000",
                                            tom = "01-01-2007"
                                    )
                            )

                    )
            ),
            barnepassPerioder = listOf(
                    BarnepassItem(
                            usikkerDatoIndikator = "NO",
                            annenInformasjon = "Pass av barn under opphold i Danmark",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            tom = "01-01-2008",
                                            fom = "01-01-2004"
                                    )
                            ),
                            informasjonBarn = InformasjonBarn(
                                    fornavn = "Ole",
                                    etternavn = "Olsen",
                                    foedseldato = "01-01-2002",
                                    land = "DK"
                            )
                    ),
                    BarnepassItem(
                            usikkerDatoIndikator = "NO",
                            annenInformasjon = "Pass av barn under opphold i Danmark",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            tom = "01-01-2008",
                                            fom = "01-01-2004"
                                    )
                            ),
                            informasjonBarn = InformasjonBarn(
                                    fornavn = "Teddy",
                                    etternavn = "Olsen",
                                    foedseldato = "01-01-2003",
                                    land = "DK"
                            )
                    )
            )
    )
    return personTrygdeTid
}



//data class Trygdehistorikk (
//    val trygdehistorikkID: Long? = null,
//    val periode: List<TrygdehistorikkPeriode>? = null,
//    val opprettetDato: String = LocalDateTime.now().toString(),
//    val godkjent: Boolean? = null,
//    val godkjentDato: String? = null
//)
//
////P4000
//data class TrygdehistorikkPeriode (
//        val trygdehistorikkPeriodeID: Long? = null,
//        var parent: Trygdehistorikk? = null,
//
//        //hvilke valg er gjort
//        val trygdehistorikkValgID: Long? = null,
//
//        //felles dato 4.1.1.2.1.1
//        val startdato: String? = null,
//        //felles dato 4.1.1.2.1.2
//        val sluttdato: String? = null,
//        //4.1.1.3 -- 4.2.1.2 -- 4.4.1.2 --
//        val usikredato: Boolean? = null,
//
//        //kun for 4.3.1 (omsorg barn)
//        val informasjonBarn: InformasjonBarn? = null,
//
//        //kun for 4.1.1 (ansettelsesforhold)
//        val informasjonArbeid: InformasjonArbeid? = null,
//
//        //kun for 4.7.1
//        val informasjoninstitusjon: Informasjoninstitusjon? = null,
//
//        //felles alle
//        //felles informasjon
//        val land: String? = null,
//        val tilleggsinformasjon: String? = null,
//
//        //ekstra ta med vedlegg her under utfylling
//        val vedlegg: List<VedleggItem>? = null,
//
//        //data for oppreting/validering  - database)
//        val opprettetDato: String = LocalDateTime.now().toString(),
//        val endretDato: String? = null
//)
//
////data class InformasjonBarn (
////        val fornavn: String? = null,
////        val etternavn: String? = null,
////        val foedseldato: String? = null
////)
//
//data class Informasjoninstitusjon(
//        //4.7.1.3.
//        val navn: String? = null
//)
//
//data class InformasjonArbeid(
//    //4.1.1.1
//    val type: String? = null,
//    //4.1.1.4
//    val yrkeaktivitet: String? = null,
//    //4.1.1.5
//    val forsikring_identifikasjon: String? = null,
//    //4.1.1.6
//    val selskapnavn: String? = null,
//
//    val gate: String? = null,
//    val bygning: String? = null,
//    val by: String? = null,
//    val region: String? = null
//)
//
//
//data class VedleggItem(
//        val navn: String? = null,
//        val data: Any? = null,
//        val type: String? = null
//)
//
//fun createTrygdehistorikkMock(): Trygdehistorikk {
//
//    val historikk = Trygdehistorikk(
//        trygdehistorikkID = 10000,
//
//        periode = listOf(
//                TrygdehistorikkPeriode(
//                        trygdehistorikkPeriodeID = 1001,
//                        trygdehistorikkValgID = 1,
//                        startdato = LocalDateTime.now().toString(),
//                        sluttdato = LocalDateTime.now().toString(),
//                        usikredato = false,
//                        informasjonArbeid = InformasjonArbeid(
//                                type = "Ansettelsesforhold",
//                                yrkeaktivitet = "Arbeider",
//                                forsikring_identifikasjon = "PBIN ID: 123456789012",
//                                selskapnavn = "StoreSelskab AS",
//                                gate = "Storeselskabersgate",
//                                bygning = "101D",
//                                by = "Gåseby",
//                                region = "Jylland"
//                        ),
//                        land = "DK",
//                        tilleggsinformasjon = "Det er ikke mye å si mer om dette arbeidfohold i Danmark det var kortvarig",
//                        vedlegg = listOf(
//                                VedleggItem(
//                                        navn = "Ansettelse kontrakt",
//                                        data = "@1",
//                                        type = "Arbeid"
//                                ),
//                                VedleggItem(
//                                        navn = "Leiekontrakt bosted",
//                                        data = "@2",
//                                        type = "Arbeid"
//                                )
//                        )
//            ),
//            TrygdehistorikkPeriode(
//                    trygdehistorikkPeriodeID = 1002,
//                    trygdehistorikkValgID = 2,
//                    startdato = LocalDateTime.now().toString(),
//                    sluttdato = null,
//                    usikredato = false,
//                    informasjonBarn = InformasjonBarn(
//                            fornavn = "fornavn",
//                            etternavn = "etternavn",
//                            foedseldato = LocalDateTime.now().toString()
//                    ),
//                    land = "DE",
//                    tilleggsinformasjon = "Det er ikke mye å si mer om dette opphold i Tyskland det var kortvarig",
//                    vedlegg = listOf(
//                            VedleggItem(
//                                    navn = "Leiekontrakt bosted",
//                                    data = "@2",
//                                    type = "Annet"
//                            ),
//                            VedleggItem(
//                                    navn = "Legeerklæring",
//                                    data = "@3",
//                                    type = "Arbeid"
//                            )
//                    )
//            ),
//            TrygdehistorikkPeriode(
//                    trygdehistorikkPeriodeID = 1003,
//                    trygdehistorikkValgID = 3,
//                    startdato = LocalDateTime.now().toString(),
//                    sluttdato = LocalDateTime.now().toString(),
//                    usikredato = true,
//                    land = "SE",
//                    informasjoninstitusjon = Informasjoninstitusjon(
//                            navn = "Store Svenske Nobel Institutt for Akademiet"
//                    ),
//                    tilleggsinformasjon = "Det er ikke mye å si mer om dette opphold i Sverige det var kortvarig",
//                    vedlegg = listOf(
//                            VedleggItem(
//                                    navn = "Bostedkontrakt",
//                                    data = "@1",
//                                    type = "Arbeid"
//                            ),
//                            VedleggItem(
//                                    navn = "Opplærings og undervisnings kontrakt",
//                                    data = "@2",
//                                    type = "Arbeid"
//                           )
//                    )
//            )
//            //end periode
//        ),
//        godkjent = true,
//        godkjentDato = LocalDateTime.now().toString()
//    )
//
//    return historikk
//
//}
