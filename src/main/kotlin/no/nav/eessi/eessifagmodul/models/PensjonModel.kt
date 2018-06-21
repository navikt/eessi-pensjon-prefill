package no.nav.eessi.eessifagmodul.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName
import java.time.LocalDate

@JsonRootName("pensjon")
data class Pensjon(
        var sak: Sak? = null,
        var vedtak: Vedtak? = null,
        var kjoeringsdato: String? = null
)

data class Sak(
        var type: String? = null,
        var artikkel44: String? = null,
        @JsonProperty("kravtype") var kravtyper: List<Kravtype>? = null
)

data class Kravtype(
        var datoFirst: String? = null
)

data class Vedtak(
        var basertPaa: String? = null,
        @JsonProperty("beregning") var beregninger: List<Beregning>? = null,
        var grunnlag: Grunnlag? = null,
        var avslag: Avslag? = null,
        var opphor: Opphor? = null,
        var begrunnelseAnnen: String? = null,
        @JsonProperty("reduksjon") var reduksjoner: List<Reduksjon>? = null,
        var dato: String? = null,
        var artikkel48: String? = null

)

data class Reduksjon(
        var type: String? = null,
        var arsak: Arsak? = null,
        var artikkeltype: String? = null
)

data class Arsak(
        var inntekt: String? = null,
        var inntektAnnen: String? = null
)

data class Opphor(
        var verdi: String? = null,
        var utbetaling: Utbetaling? = null,
        var begrunnelse: String? = null,
        var dato: String? = null,
        var annulleringdato: String? = null
)

data class Utbetaling(
        var beloepBrutto: String? = null,
        var valuta: String? = null

)

data class Grunnlag(
        var meldlemskap: String? = null,
        var opptjening: Opptjening? = null,
        var framtidigtrygdetid: String? = null
)

data class Opptjening(
        var forsikredeAnnen: String? = null
)

data class Avslag(
        var begrunnelse: String? = null,
        var begrunnelseAnnen: String? = null
)

data class Beregning(
        var artikkel: String? = null,
        var virkningsdato: String? = null,
        var periode: Periode? = null,
        var beloepNetto: BeloepNetto? = null,
        var beloepBrutto: BeloepBrutto? = null,
        var valuta: String? = null,
        var utbetalingshyppighet: String? = null,
        var utbetalingshyppighetAnnen: String? = null
)


data class BeloepNetto(
        var beloep: String? = null
)

data class BeloepBrutto(
        var beloep: String? = null,
        var ytelseskomponentGrunnpensjon: String? = null,
        var ytelseskomponentTilleggspensjon: String? = null,
        var ytelseskomponentAnnen: String? = null
)

data class Periode(
        var fom: String? = null,
        var tom: String? = null
)
