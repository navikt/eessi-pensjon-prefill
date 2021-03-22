package no.nav.eessi.pensjon.fagmodul.sedmodel

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.eessi.pensjon.fagmodul.models.SEDType

class P5000(
    @JsonProperty("sed")
    override var type: SEDType = SEDType.P5000,
    override val sedGVer: String? = "4",
    override var sedVer: String? = "1",
    override var nav: Nav? = null,
    @JsonProperty("pensjon")
    val p5000Pensjon: P5000Pensjon
) : SED(type, sedGVer, sedVer, nav)

/**
 * Benyttes både ved prefill og ved visning av trygdetidsperioder til frontend
 *
 * Flere felter her er ubrukt i javakoden men benyttes av frontend
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class MedlemskapItem(
    val relevans: String? = null,
    val ordning: String? = null,
    val land: String? = null,
    val sum: TotalSum? = null,
    val yrke: String? = null,
    val gyldigperiode: String? = null,
    val type: String? = null,
    val beregning: String? = null,
    val informasjonskalkulering: String? = null,
    val periode: Periode? = null,
    val enkeltkrav: KravtypeItem? = null
)

data class Dager(
    val nr: String? = null,
    val type: String? = null
)

data class TotalSum(
    val kvartal: String? = null,
    val aar: String? = null,
    val uker: String? = null,
    val dager: Dager? = null,
    val maaneder: String? = null
)