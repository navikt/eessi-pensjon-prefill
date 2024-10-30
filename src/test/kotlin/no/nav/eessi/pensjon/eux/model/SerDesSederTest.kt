package no.nav.eessi.pensjon.eux.model

import no.nav.eessi.pensjon.eux.model.sed.*
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.toJson
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

@DisplayName("EuxModel Test")
internal class SerDesSederTest {

    @Nested
    @DisplayName("Horisontale seder")
    inner class Seder {

        @Test
        fun `H020 from json datafile`() {
            val h020sed = SED.fromJson(getTestJsonFile("horisontal/H020-A-NAV.json"))

            assertNotNull(h020sed.nav?.bruker?.person?.pinland)
            assertEquals("01223123123", h020sed.nav?.bruker?.person?.pinland?.kompetenteuland)
        }

        @Test
        fun `H021 from json datafile`() {
            val h021sed = SED.fromJson(getTestJsonFile("horisontal/H021-A-NAV.json"))

            assertNotNull(h021sed.nav?.bruker?.person?.pinland)
            assertEquals("213421412414214", h021sed.nav?.bruker?.person?.pinland?.kompetenteuland)
        }

        @Test
        fun `H070 from json datafile`() {
            val h070sed = SED.fromJson(getTestJsonFile("horisontal/H070-NAV.json"))

            val statsborgerskap = h070sed.nav?.bruker?.person?.statsborgerskap
            assertEquals(1, statsborgerskap?.size)

        }

        @Test
        fun `H120 from json datafile`() {
            val h120sed = SED.fromJson(getTestJsonFile("horisontal/H120-NAV.json"))

            val statsborgerskap = h120sed.nav?.bruker?.person?.statsborgerskap
            assertEquals(3, statsborgerskap?.size)
        }
    }

    @Nested
    @DisplayName("KravSeder ALDER; UFØRE og GJENLEVENDE")
    inner class KravSeder {
        @Test
        fun `P2000 from json datafile`() { SED.fromJson(getTestJsonFile("P2000-NAV.json")) }

        @Test
        fun `P2100 from json datafile`() { SED.fromJson(getTestJsonFile("P2100-PinDK-NAV.json")) }

        @Test
        fun `P2000 new v4_1 from json datafile`() { SED.fromJson(getTestJsonFile("P2000-NAV-4.1-new.json")) }

        @Test
        fun `P2200 from json datafile`() {
            val p2200sed = mapJsonToAny<SED>(getTestJsonFile("P2000-NAV.json"), true)

            assertNotNull(p2200sed)
            mapAnyToJson(p2200sed, true)
            assertEquals(SED::class.java, p2200sed::class.java)
        }

        @Test
        fun `P2200 from mockData`() {
            val p2200 =  SedMock().genererP2000Mock()
            val p2200json = mapAnyToJson(p2200, true)

            assertNotNull(p2200)
            assertNotNull(p2200json)
            mapJsonToAny<SED>(p2200json)
        }
    }

    @Nested
    @DisplayName("Seder for PENSJON")
    inner class PenSeder {
        @Test
        fun `P1000 from json datafaile`() {
            SED.fromJson(getTestJsonFile("P1000-NAV.json"))
        }

        @Test
        fun `P1100 from json datafile`() {
            SED.fromJson(getTestJsonFile("P1100-NAV.json"))
        }

        @Test
        fun `P3000_NO from json datafile`() {
            val p3000sed = SED.fromJson(getTestJsonFile("P3000_NO-NAV.json"))
            p3000sed.toJson()

            assertEquals("6511", p3000sed.pensjon?.landspesifikk?.norge?.ufore?.barnInfo!!.get(0).etternavn)
            assertEquals(
                "CZK",
                p3000sed.pensjon?.landspesifikk?.norge?.alderspensjon?.ektefelleInfo?.pensjonsmottaker!!.first().institusjonsopphold?.belop?.last()!!.valuta
            )
        }

        @Test
        fun `P5000 from datafile`() {
            val navSedP5000 = SedMock().genererP5000Mock()
            val json = mapAnyToJson(navSedP5000, true)
            val pensjondata = mapJsonToAny<SED>(json)

            assertNotNull(navSedP5000)
            assertNotNull(pensjondata)
        }

        @Test
        fun `P5000 map trygdetidsperioder`() {
            val p5000json = String(Files.readAllBytes(Paths.get("src/test/resources/json/nav/P5000-NAV.json")))
            val p5000 = mapJsonToAny<P5000>(p5000json)

            assertNotNull(p5000.pensjon?.trygdetid)
        }

        @Test
        fun `P7000 from json datafile`() {
            val p7000sed = SED.fromJson(getTestJsonFile("P7000-NAV.json"))

            assertEquals("1942-12-19", p7000sed.pensjon?.ytelser?.get(0)?.startdatoutbetaling)
        }

        @Test
        fun `P7000_2 from json datafile`() {
            val p7000sed = SED.fromJson(getTestJsonFile("P7000_2-NAV_v4_1.json"))
            p7000sed.toJson()
        }

        @Test
        fun `P8000 from json datafile`() {
            val p8000json = getTestJsonFile("P8000-NAV.json")
            val p8000sed = mapJsonToAny<P8000>(p8000json)

            assertEquals("02", p8000sed.p8000Pensjon?.anmodning?.referanseTilPerson)
        }

        @Test
        fun `P9000 from json datafile`() {
            val p9000sed = SED.fromJson(getTestJsonFile("P9000-NAV.json"))
            val bruker = p9000sed.nav?.bruker

            //hovedperson
            assertEquals("Raus 212", bruker?.person?.fornavn)
            assertEquals("levanger 21811", bruker?.person?.foedested?.by)
            assertEquals("NO2082760100435", bruker?.bank?.konto?.sepa?.iban)

            //annenperson
            assertEquals("Rausa 322", p9000sed.nav?.annenperson?.person?.fornavn)
            assertEquals("0101010202022 327112", p9000sed.nav?.annenperson?.person?.pin?.first()?.identifikator)
        }

        @Test
        fun `P10000 from json datafile`() {
            val p10000sed = SED.fromJson(getTestJsonFile("P10000-03Barn-NAV.json"))

            assertEquals("VAIKUNTHARAJAN-MASK", p10000sed.nav?.bruker?.person?.fornavn)
            assertEquals("samboer", p10000sed.nav?.bruker?.person?.sivilstand?.get(0)?.status)
            assertEquals(
                "NSSI_TNT1, NATIONAL SOCIAL SECURITY INSTITUTE, BG",
                p10000sed.nav?.eessisak?.get(0)?.institusjonsnavn
            )
        }

        @Test
        fun `P11000 from json datafile`() {
            SED.fromJson(getTestJsonFile("P11000_Fixed-NAV.json"))
        }

        @Test
        fun `P12000 from json datafile`() {
            SED.fromJson(getTestJsonFile("P12000-NAV.json"))
        }

        @Test
        fun `P13000 from json datafile`() {
            SED.fromJson(getTestJsonFile("P13000-NAV.json"))
        }

        @Test
        fun `P14000 from json datafile`() {
            val p14000sed = SED.fromJson(getTestJsonFile("P14000-NAV.json"))
            p14000sed.toJson()
        }

        @Test
        fun `P15000 from json datafile`() {
            val p15000sed = SED.fromJson(getTestJsonFile("P15000-NAV.json"))
            p15000sed.toJson()
            val bruker = p15000sed.nav?.bruker

            //hovedperson
            assertEquals(KravType.ALDER, p15000sed.nav?.krav?.type)
            assertEquals("Mandag", bruker?.person?.fornavn)
            assertEquals(null, bruker?.bank?.konto?.sepa?.iban)
            assertEquals("21811", bruker?.person?.foedested?.by)
            assertEquals("2019-02-01", p15000sed.nav?.krav?.dato)
        }

        @Test
        fun `X005 from json datafile`() {
            val x005json = getTestJsonFile("X005-NAV.json")
            val x005sed = SED.fromJsonToConcrete(x005json) as X005

            val sak = x005sed.xnav?.sak
            val bruker = x005sed.xnav?.sak?.kontekst?.bruker?.person

            assertEquals("Duck", bruker?.etternavn)
            assertEquals("Dummy", bruker?.fornavn)
            assertEquals("1958-02-01", bruker?.foedselsdato)

            assertEquals("NO:NAVT002", sak?.leggtilinstitusjon?.institusjon?.id)
            assertEquals("NAVT002", sak?.leggtilinstitusjon?.institusjon?.navn)

            x005sed.toJsonSkipEmpty()

            val xprefill005sed = SED.fromJsonToConcrete(getTestJsonFile("PrefillX005-NAV.json")) as X005
            val sak2 = xprefill005sed.xnav?.sak
            val bruker2 = sak2?.kontekst?.bruker

            assertEquals("POTET", bruker2?.person?.etternavn)
            assertEquals("KRIMINELL", bruker2?.person?.fornavn)
            assertEquals("1944-12-25", bruker2?.person?.foedselsdato)

            assertEquals("NO:NAVT007", sak2?.leggtilinstitusjon?.institusjon?.id)
            assertEquals("NAVT007", sak2?.leggtilinstitusjon?.institusjon?.navn)
        }
    }
}
