package no.nav.eessi.pensjon.fagmodul.pesys.mockup

import no.nav.eessi.pensjon.fagmodul.models.SEDType
import no.nav.eessi.pensjon.fagmodul.sedmodel.Alderspensjon
import no.nav.eessi.pensjon.fagmodul.sedmodel.Bruker
import no.nav.eessi.pensjon.fagmodul.sedmodel.Krav
import no.nav.eessi.pensjon.fagmodul.sedmodel.Landspesifikk
import no.nav.eessi.pensjon.fagmodul.sedmodel.Nav
import no.nav.eessi.pensjon.fagmodul.sedmodel.Norge
import no.nav.eessi.pensjon.fagmodul.sedmodel.Pensjon
import no.nav.eessi.pensjon.fagmodul.sedmodel.Person
import no.nav.eessi.pensjon.fagmodul.sedmodel.PinItem
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.fagmodul.sedmodel.StatsborgerskapItem
import org.slf4j.LoggerFactory

class MockSED001 {

    private val logger = LoggerFactory.getLogger(MockSED001::class.java)

    fun mockP2000(): SED {
        val p2000 = SED(SEDType.P2000)
        p2000.nav = Nav(
                bruker = Bruker(
                        person = Person(
                                pin = listOf(PinItem(
                                        sektor = "Pensjon",
                                        identifikator = "410155012341",
                                        land = "SE"
                                )),
                                statsborgerskap = listOf(
                                        StatsborgerskapItem(
                                                land = "SE"
                                        )
                                )
                        )
                ),
                krav = Krav(dato = "2019-03-11")
        )
        return p2000
    }

    fun mockP3000NO(pensjonsgrad: String? = null): SED {
        val p3000no = SED(
                type = SEDType.P3000_NO,
                pensjon = Pensjon(
                        landspesifikk = Landspesifikk(
                                norge = Norge(
                                        alderspensjon = Alderspensjon(
                                                pensjonsgrad = pensjonsgrad
                                        )
                                )
                        )

                )
        )
        return p3000no
    }

    fun mockP4000(): SED {
        return try {
            val p4000 = SED(type = SEDType.P4000)

            logger.info("p4000 mocket og legges til")

            p4000
        } catch (ex: Exception) {
            logger.error(ex.message)
            return  SED(type = SEDType.P4000)
        }

    }
}
