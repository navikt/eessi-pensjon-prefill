package no.nav.eessi.eessifagmodul.preutfyll

import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.utils.mapJsonToAny
import no.nav.eessi.eessifagmodul.utils.typeRefs

class PreutfyllingP4000 {

    fun utfyllTrygdeTid(utfyllingData: UtfyllingData): PersonTrygdeTid {

        val json = utfyllingData.hentPartSedasJson("P4000")

        return when {
            json != "" -> {
//1                val andrePerioder: List<StandardItem>? = null,
//2                val arbeidsledigPerioder: List<StandardItem>? = null,
//3                val boPerioder: List<StandardItem>? = null,
//4                val opplaeringPerioder: List<StandardItem>? = null,
//5                val sykePerioder: List<StandardItem>? = null,
//6                val barnepassPerioder: List<BarnepassItem>? = null,
//7                val ansattSelvstendigPerioder: List<AnsattSelvstendigItem>? = null,
//8                val forsvartjenestePerioder: List<StandardItem>? = null,
//9                val foedselspermisjonPerioder: List<StandardItem>? = null,
//10               val frivilligPerioder: List<StandardItem>? = null

                val allePerioder = createPersonTrygdeTidMock()
                return when (json) {
                    "1" ->  PersonTrygdeTid(andrePerioder = allePerioder.andrePerioder )
                    "2" ->  PersonTrygdeTid(arbeidsledigPerioder = allePerioder.arbeidsledigPerioder)
                    "3" ->  PersonTrygdeTid(boPerioder = allePerioder.boPerioder )
                    "4" ->  PersonTrygdeTid(opplaeringPerioder = allePerioder.opplaeringPerioder )
                    "5" ->  PersonTrygdeTid(sykePerioder = allePerioder.sykePerioder)
                    "6" ->  PersonTrygdeTid(barnepassPerioder = allePerioder.barnepassPerioder)
                    "7" ->  PersonTrygdeTid(ansattSelvstendigPerioder = allePerioder.ansattSelvstendigPerioder)
                    "8" ->  PersonTrygdeTid(forsvartjenestePerioder = allePerioder.forsvartjenestePerioder)
                    "9" ->  PersonTrygdeTid(foedselspermisjonPerioder = allePerioder.foedselspermisjonPerioder)
                   "10" ->  PersonTrygdeTid(frivilligPerioder = allePerioder.frivilligPerioder)
                    "all" -> allePerioder
                    else -> mapJsonToAny(json!!, typeRefs<PersonTrygdeTid>())
                }
            }
            else -> {
                //val trygdeTid = PersonTrygdeTid()
                val trygdeTid = PersonTrygdeTid(
                        andrePerioder = listOf(StandardItem(
                                land = "NL",
                                annenInformasjon = "safsdfsd sdfsdfs",
                                usikkerDatoIndikator = "0",
                                typePeriode = null,
                                periode = TrygdeTidPeriode(
                                        lukketPeriode = Periode(
                                                fom = "2001-01-01",
                                                tom = "2004-01-01"
                                        )
                                )
                        ))
                )
                trygdeTid
            }
        }
    }

}