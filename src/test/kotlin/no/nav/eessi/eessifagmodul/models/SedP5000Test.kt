package no.nav.eessi.eessifagmodul.models

import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import no.nav.eessi.eessifagmodul.utils.mapJsonToAny
import no.nav.eessi.eessifagmodul.utils.typeRefs
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SedP5000Test {

    val logger: Logger by lazy { LoggerFactory.getLogger(SedP5000Test::class.java) }

    @Before
    fun setup() {
        logger.debug("Starting tests.... ...")
        //MockitoAnnotations.initMocks(this)
    }

    @Test
    fun `validate P5000 to json and back`() {
        val navSedP5000 = SedMock().genererP5000Mock()
        assertNotNull(navSedP5000)

        val json = mapAnyToJson(navSedP5000, true)
        //map json back to P6000 obj
        val pensjondata = mapJsonToAny(json, typeRefs<SED>())
        assertNotNull(pensjondata)
        assertEquals(navSedP5000, pensjondata)

        //logger.debug("\n\n $json \n\n")
    }
}

//P5000 - bekreftforsikred
fun createMedlemskapMock(): Pensjon {

    return Pensjon(
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
}
