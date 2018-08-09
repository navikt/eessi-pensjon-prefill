package no.nav.eessi.eessifagmodul.models


/**
 * Nav SED for P4000
 */
data class PersonTrygdeTid (
        val andrePerioder: List<StandardItem>? = null,
        val arbeidsledigPerioder: List<StandardItem>? = null,
        val boPerioder: List<StandardItem>? = null,
        val opplaeringPerioder: List<StandardItem>? = null,
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
        //for 'andreperioder' ektra info.
        val typePeriode: String? = null
)

/**
 * flyttes snart til test.
 */
fun createPersonTrygdeTidMock(): PersonTrygdeTid {

    val personTrygdeTid = PersonTrygdeTid(
            foedselspermisjonPerioder = listOf(
                    StandardItem(
                            land = "NO",
                            usikkerDatoIndikator = "1",
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
                            usikkerDatoIndikator = "0",
                            annenInformasjon= "fødselperm i frankrike",
                            periode = TrygdeTidPeriode(
                                    openPeriode = Periode (
                                            fom = "2002-01-01",
                                            extra = "98"
                                    )
                            )

                    )
            ),
            ansattSelvstendigPerioder = listOf(
                    AnsattSelvstendigItem(
                            typePeriode = "01",
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
                            usikkerDatoIndikator = "1"
                    )
            ),
            andrePerioder = listOf(
                    StandardItem(
                            land = "SE",
                            usikkerDatoIndikator = "1",
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
                            usikkerDatoIndikator = "1",
                            annenInformasjon= "ikkenoemere",
                            typePeriode = "Leve og ha det gøy",
                            periode = TrygdeTidPeriode(
                                    openPeriode = Periode (
                                            fom = "2000-01-01",
                                            extra = "01"
                                    )
                            )
                    )
            ),
            boPerioder = listOf(
                    StandardItem(
                            land = "DK",
                            usikkerDatoIndikator = "1",
                            annenInformasjon = "Deilig i Danmark",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            fom = "2003-01-01",
                                            tom = "2004-01-01"
                                    )
                            )
                    )
            ),
            arbeidsledigPerioder = listOf(
                    StandardItem(
                            land = "IT",
                            usikkerDatoIndikator = "1",
                            annenInformasjon = "Arbeidsledig i Itelia for en kort periode.",
                            navnPaaInstitusjon = "NAV stønad for arbeidsledigetstrygd",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            fom = "2002-01-01",
                                            tom = "2004-01-01"
                                    )
                            )

                    )
            ),
            forsvartjenestePerioder = listOf(
                    StandardItem(
                            land = "SE",
                            usikkerDatoIndikator = "1",
                            annenInformasjon = "Forsvar og mlitærtjeneste fullført i Svergige",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            fom = "2001-01-01",
                                            tom = "2004-01-01"
                                    )
                            )

                    )
            ),
            sykePerioder = listOf(
                    StandardItem(
                            land = "ES",
                            usikkerDatoIndikator = "1",
                            annenInformasjon = "Sykdom og forkjølelse i Spania",
                            navnPaaInstitusjon = "Støtte for sykeophold NAV",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            fom = "2005-01-01",
                                            tom = "2007-01-01"
                                    )
                            )

                    )

            ),
            frivilligPerioder = listOf(
                    StandardItem(
                            land = "GR",
                            usikkerDatoIndikator = "1",
                            annenInformasjon = "Frivilig hjelpemedarbeider i Helles",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            fom = "2006-01-01",
                                            tom = "2007-01-01"
                                    )
                            )

                    )
            ),
            opplaeringPerioder = listOf(
                    StandardItem(
                            land = "SE",
                            usikkerDatoIndikator = "1",
                            annenInformasjon = "Opplæring høyere utdanning i Sverige",
                            navnPaaInstitusjon = "Det Akademiske instutt for høgere lære, Stockholm",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            fom = "2000-01-01",
                                            tom = "2007-01-01"
                                    )
                            )

                    )
            ),
            barnepassPerioder = listOf(
                    BarnepassItem(
                            usikkerDatoIndikator = "1",
                            annenInformasjon = "Pass av barn under opphold i Danmark",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            tom = "2008-01-01",
                                            fom = "2004-01-01"
                                    )
                            ),
                            informasjonBarn = InformasjonBarn(
                                    fornavn = "Ole",
                                    etternavn = "Olsen",
                                    foedseldato = "2002-01-01",
                                    land = "DK"
                            )
                    ),
                    BarnepassItem(
                            usikkerDatoIndikator = "1",
                            annenInformasjon = "Pass av barn under opphold i Danmark",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            tom = "2008-01-01",
                                            fom = "2004-01-01"
                                    )
                            ),
                            informasjonBarn = InformasjonBarn(
                                    fornavn = "Teddy",
                                    etternavn = "Olsen",
                                    foedseldato = "2003-01-01",
                                    land = "DK"
                            )
                    )
            )
    )
    return personTrygdeTid
}

