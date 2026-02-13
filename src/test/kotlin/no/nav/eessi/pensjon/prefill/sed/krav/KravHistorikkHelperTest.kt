package no.nav.eessi.pensjon.prefill.sed.krav

import no.nav.eessi.pensjon.pensjonsinformasjon.KravHistorikkHelper
import no.nav.eessi.pensjon.pensjonsinformasjon.models.KravArsak
import no.nav.eessi.pensjon.pensjonsinformasjon.models.PenKravtype
import no.nav.eessi.pensjon.pensjonsinformasjon.models.PenKravtype.*
import no.nav.pensjon.v1.kravhistorikk.V1KravHistorikk
import no.nav.pensjon.v1.kravhistorikkliste.V1KravHistorikkListe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class KravHistorikkHelperTest {

    @Test
    fun `Gitt en liste med krav som har gjenlevende årsak når kravhistorikk blir hentet så returneres kravhistorikk med enten tilstøtende_dødsfall eller gjenlevende_tilegg`() {
        val gjenlevendHistorikk =  V1KravHistorikk()
        gjenlevendHistorikk.kravArsak = KravArsak.GJNL_SKAL_VURD.name

        val kravListe = mutableListOf(gjenlevendHistorikk)
        val kravHistorikkListe = V1KravHistorikkListe()
        kravHistorikkListe.kravHistorikkListe.addAll(kravListe)
        val actual = KravHistorikkHelper.hentKravhistorikkForGjenlevende(kravHistorikkListe)

        assert(actual?.kravArsak == KravArsak.GJNL_SKAL_VURD.name)

        val tilsDodHistorikk =  V1KravHistorikk()
        gjenlevendHistorikk.kravArsak = KravArsak.TILST_DOD.name
        val kravListeDod = mutableListOf(tilsDodHistorikk, gjenlevendHistorikk)
        val kravHistorikkListeDod = V1KravHistorikkListe()
        kravHistorikkListeDod.kravHistorikkListe.addAll(kravListeDod)
        val actuaDod = KravHistorikkHelper.hentKravhistorikkForGjenlevende(kravHistorikkListeDod)

        assert(actuaDod?.kravArsak == KravArsak.TILST_DOD.name)
    }

    @ParameterizedTest
    @MethodSource("kravTypeProvider")
    fun `hentKravHistorikkForsteGangsBehandlingUtlandEllerForsteGang skal hente ut ihht liste av kravtyper`(input: Pair<List<PenKravtype>, PenKravtype>) {
        val kravListe = input.first.map {
            V1KravHistorikk().apply {
                kravType = it.name
                mottattDato = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar("2022-01-15T00:00:00")

            }
        }
        val kravHistorikkListe = V1KravHistorikkListe().apply { kravHistorikkListe.addAll(kravListe) }
        val actual = KravHistorikkHelper.hentKravHistorikkForsteGangsBehandlingUtlandEllerForsteGang(kravHistorikkListe)
        assert(actual.kravType == input.second.name)
    }

    companion object {
        @JvmStatic
        fun kravTypeProvider(): Stream<Pair<List<PenKravtype>, PenKravtype>> = Stream.of(
            Pair(listOf(PenKravtype.F_BH_MED_UTL, PenKravtype.REVURD, PenKravtype.F_BH_BO_UTL, PenKravtype.SLUTT_BH_UTL), PenKravtype.F_BH_MED_UTL),
            Pair(listOf(F_BH_MED_UTL, F_BH_KUN_UTL, REVURD, F_BH_BO_UTL, SLUTT_BH_UTL), PenKravtype.F_BH_MED_UTL),
            Pair(listOf(F_BH_KUN_UTL, REVURD, F_BH_BO_UTL, SLUTT_BH_UTL), PenKravtype.F_BH_KUN_UTL),
            Pair(listOf(REVURD, F_BH_BO_UTL, SLUTT_BH_UTL), PenKravtype.REVURD),
            Pair(listOf(F_BH_BO_UTL, SLUTT_BH_UTL), PenKravtype.F_BH_BO_UTL),

            Pair(listOf(SLUTT_BH_UTL, F_BH_KUN_UTL, REVURD, F_BH_BO_UTL, F_BH_MED_UTL), PenKravtype.F_BH_MED_UTL),
        )
    }
}