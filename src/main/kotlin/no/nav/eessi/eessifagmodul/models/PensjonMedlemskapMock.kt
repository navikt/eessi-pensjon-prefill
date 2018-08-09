package no.nav.eessi.eessifagmodul.models

//P5000 - bekreftforsikred
fun createMedlemskapMock(): Pensjon {

    val pensjon = Pensjon(

        sak = Sak(
                enkeltkrav = KravtypeItem(
                        krav = "10"
                )

        ),
        medlemskap = listOf(
                MedlemskapItem(
                        land = "DK",
                        ordning = "01",
                        type = "10",
                        relevans = "100",
                        gyldigperiode = "1",
                        beregning = "100",
                        periode = Periode(
                                fom = "2000-01-01",
                                tom = "2010-01-01"
                        ),
                        sum = TotalSum(
                                aar = "4",
                                dager = Dager(nr = "2"),
                                maaneder = "2"
                        )
                )
        ),
        medlemskapAnnen = listOf(
                MedlemskapItem(
                        land = "DE",
                        type = "21",
                        ordning = "01",
                        relevans = "100",
                        beregning = "100",
                        sum = TotalSum(
                                aar = "4",
                                maaneder = "2",
                                dager = Dager(nr = "5")
                        )

                )
        ),
        medlemskapTotal = listOf(
                MedlemskapItem(
                        type = "10",
                        relevans = "100",
                        sum = TotalSum(
                                aar = "11",
                                maaneder = "1",
                                dager = Dager(nr = "6")
                        )
                )
        ),
        trygdetid = listOf(
                MedlemskapItem(
                        type = "11",
                        sum = TotalSum(
                                aar = "10",
                                maaneder = "2",
                                dager = Dager(nr = "5")
                    )
                )
        )
    )
    return pensjon
}
