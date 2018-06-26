package no.nav.eessi.eessifagmodul.models

import com.fasterxml.jackson.annotation.JsonProperty



/**
 *  Pensjon objekt er her!
 *
 */
data class Pensjon(
        @JsonProperty("reduksjon")
        var reduksjon: List<ReduksjonItem>? = null,
        @JsonProperty("vedtak")
        var vedtak: List<VedtakItem?>? = null,
        var sak: Sak? = null,
        var gjenlevende: Gjenlevende? = null,
        @field:JsonProperty("tilleggsinformasjon")
        var tilleggsinformasjon: Tilleggsinformasjon? = null
)

data class Sak(
        val artikkel54: String? = null,
        @JsonProperty("reduksjon")
        val reduksjon: List<ReduksjonItem>? = null,
        @JsonProperty("kravtype")
        val kravtype: List<KravtypeItem>? = null
)

data class KravtypeItem(
        @JsonProperty("datoFrist")
        val datoFrist: String? = null
)

data class VedtakItem(
        val grunnlag: Grunnlag? = null,
        val begrunnelseAnnen: String? = null,
        val artikkel: String? = null,
        val virkningsdato: String? = null,
        val ukjent: Ukjent? = null,
        val type: String? = null,
        val resultat: String? = null,
        @JsonProperty("beregning")
        val beregning: List<BeregningItem>? = null,
        @JsonProperty("avslagbegrunnelse")
        val avslagbegrunnelse: List<AvslagbegrunnelseItem>? = null,
        val kjoeringsdato: String? = null,
        val basertPaa: String? = null,
        val basertPaaAnnen: String? = null,
        val delvisstans: Delvisstans? = null
)

data class Tilleggsinformasjon(
        val annen: Annen? = null,
        val anneninformation: String? = null,
        val saksnummer: String? = null,
        val person: Person? = null,
        val dato: String? = null,
        @JsonProperty("andreinstitusjoner")
        val andreinstitusjoner: List<AndreinstitusjonerItem>? = null,
        val saksnummerAnnen: String? = null,
        val artikkel48: String? = null,
        val opphoer: Opphoer? = null
)

data class AndreinstitusjonerItem(
        val institusjonsadresse: String? = null,
        val postnummer: String? = null,
        val bygningsnr: String? = null,
        val land: String? = null,
        val region: String? = null,
        val poststed: String? = null
)

data class Annen(
        val institusjonsadresse: Institusjonsadresse? = null
)

data class Delvisstans(
        val utbetaling: Utbetaling? = null,
        val indikator: String? = null
)

data class Ukjent(
        val beloepBrutto: BeloepBrutto? = null
)

data class ReduksjonItem (
        val type: String? = null,
        @JsonProperty("virkningsdato")
        val virkningsdato: List<VirkningsdatoItem>? = null,
        val arsak: Arsak? = null,
        val artikkeltype: String? = null
)

data class VirkningsdatoItem(
       val startdato: String? = null,
       val sluttdato: String? = null
)

data class Arsak(
        val inntektAnnen: String? = null,
        val annenytelseellerinntekt: String? = null
)

data class Opphoer(
       val dato: String? = null,
       val annulleringdato: String? = null
)

data class Utbetaling(
        val begrunnelse: String? = null,
        val valuta: String? = null,
        val beloepBrutto: String? = null
)

data class Grunnlag(
        val medlemskap: String? = null,
        val opptjening: Opptjening? = null,
        val framtidigtrygdetid: String? = null
)

data class Opptjening(
        val forsikredeAnnen: String? = null
)

data class AvslagbegrunnelseItem(
       val begrunnelse: String? = null,
       val annenbegrunnelse: String? = null
)

data class BeregningItem(
       val beloepNetto: BeloepNetto? = null,
        val valuta: String? = null,
        val beloepBrutto: BeloepBrutto? = null,
        val utbetalingshyppighetAnnen: String? = null,
        val periode: Periode? = null,
        val utbetalingshyppighet: String? = null
)

data class BeloepNetto(
        val beloep: String? = null
)

data class BeloepBrutto(
       val ytelseskomponentTilleggspensjon: String? = null,
       val beloep: String? = null,
       val ytelseskomponentGrunnpensjon: String? = null,
       val ytelseskomponentAnnen: String? = null
)

data class Periode(
        val fom: String? = null,
        val tom: String? = null
)

/***
 *
 * end of data class. Pensjon/P6000
 *
 */

class PensjonMock {

    fun genererMockData(): Pensjon {
        return Pensjon(
                gjenlevende = Gjenlevende(
                        adresse = Adresse(
                                postnummer = "sdfsdf",
                                by = "sfsdf",
                                land = "BG",
                                gate = "sdfsdfs",
                                bygning = "sdfsdfs",
                                region = "dfsdf"
                        ),
                        far = Far(
                                person = Person(
                                        fornavn = "sdfsdf",
                                        etternavnvedfoedsel = "sdfsd")),
                        mor = Mor(
                                person = Person(
                                        etternavnvedfoedsel = "asdfsdf",
                                        fornavn = "asdfsdafsdf"
                                )
                        ),
                        person = Person(
                                fornavn = "gafgfdga",
                                forrnavnvedfoedsel = "ffsdfgsfgadfg",
                                kjoenn = "m",
                                statsborgerskap = listOf(
                                        StatsborgerskapItem("AT"),
                                        StatsborgerskapItem("BE"),
                                        StatsborgerskapItem("GR")
                                ),
                                foedested = Foedested(
                                        land = "BE",
                                        region = "sdfsdfsdf",
                                        by = "Testaeveisgiverby2"),
                                tidligerefornavn = "sadfsdf",
                                etternavn = "asdffsdaf",
                                etternavnvedfoedsel = "sdfsd",
                                pin = listOf(
                                        PinItem(
                                                sektor = "dagpenger",
                                                land = "BE",
                                                identifikator = "sdafsdf"
                                        ),
                                        PinItem(
                                                sektor = "alle",
                                                land = "HR",
                                                identifikator = "sdfgsdgh"
                                        )
                                ),
                                tidligereetternavn = "asdfasdf",
                                foedselsdato = "1964-12-01"
                        )
                )
                //end Gjendlevende
                ,
                vedtak = listOf(
                        VedtakItem(
                                grunnlag = Grunnlag(
                                        framtidigtrygdetid = "0",
                                        medlemskap = "02",
                                        opptjening = Opptjening(
                                                forsikredeAnnen = "01"
                                        )
                                ),
                                beregning = listOf(
                                        BeregningItem(
                                                beloepBrutto = BeloepBrutto(
                                                        ytelseskomponentGrunnpensjon = "2344",
                                                        beloep = "523",
                                                        ytelseskomponentTilleggspensjon = "234"
                                                ),
                                                periode = Periode(
                                                        tom = "2030-10-01",
                                                        fom = "2023-10-01"
                                                ),
                                                valuta = "HUF",
                                                beloepNetto = BeloepNetto(
                                                        beloep = "344"
                                                ),
                                                utbetalingshyppighetAnnen = "13123",
                                                utbetalingshyppighet = "maaned_12_per_aar"
                                        ),
                                        BeregningItem(
                                                utbetalingshyppighetAnnen = "werwer",
                                                beloepBrutto = BeloepBrutto(
                                                        beloep = "234",
                                                        ytelseskomponentTilleggspensjon = "22",
                                                        ytelseskomponentGrunnpensjon = "342"
                                                ),
                                                periode = Periode(
                                                        fom = "2020-10-01",
                                                        tom = "2025-10-01"
                                                ),
                                                beloepNetto = BeloepNetto(
                                                        beloep = "344"
                                                ),
                                                utbetalingshyppighet = "annet",
                                                valuta = "ISK"
                                        )
                                ),
                                basertPaa = "02",
                                delvisstans = Delvisstans(
                                        utbetaling = Utbetaling(
                                                begrunnelse = "sfdgsdf\\nfdg\\ns",
                                                beloepBrutto = "24234",
                                                valuta = "SEK"
                                        ),
                                indikator = "1"
                                ),
                                virkningsdato = "2020-10-01",
                                artikkel = "02",
                                kjoeringsdato = "2020-12-01",
                                type = "02",
                                basertPaaAnnen = "sadfsdf",
                                ukjent = Ukjent(
                                        beloepBrutto = BeloepBrutto(
                                                ytelseskomponentAnnen = "sdfsfd\\nsdf\\nsfd"
                                )
                                ),
                                resultat = "01",
                                avslagbegrunnelse = listOf(
                                        AvslagbegrunnelseItem(
                                                begrunnelse = "03",
                                                annenbegrunnelse = "fsafasfd\\nasd\\nfsda"
                                        ),
                                        AvslagbegrunnelseItem(
                                                begrunnelse = "02",
                                                annenbegrunnelse = "tet\\nertert\\nretret"
                                        )
                                ),
                                begrunnelseAnnen = "afsdaf\\nsdafsfasd\\nsadfsd"
                        ),
                        VedtakItem(
                                beregning = listOf(
                                        BeregningItem(
                                                utbetalingshyppighetAnnen = "gagfdgg",
                                                valuta = "ERN",
                                                beloepBrutto = BeloepBrutto(
                                                        ytelseskomponentTilleggspensjon = "12",
                                                        ytelseskomponentGrunnpensjon = "122",
                                                        beloep = "234"
                                                ),
                                                beloepNetto = BeloepNetto(
                                                        beloep = "23"
                                                ),
                                                periode = Periode(
                                                        tom = "2043-10-01",
                                                        fom = "2032-10-01"
                                                ),
                                                utbetalingshyppighet = "kvartalsvis"
                                        )
                                ),
                                avslagbegrunnelse = listOf(
                                        AvslagbegrunnelseItem(
                                                annenbegrunnelse = "324234\\n234\\n234\\n4",
                                                begrunnelse = "04"
                                        ),
                                        AvslagbegrunnelseItem(
                                                annenbegrunnelse = "sdfafs\\nsdfsdf\\nfsadfsdf",
                                                begrunnelse = "04"
                                        )
                                ),
                                grunnlag = Grunnlag(
                                        framtidigtrygdetid = "0",
                                        medlemskap = "03",
                                        opptjening = Opptjening(
                                                forsikredeAnnen = "03"
                                        )
                                ),
                                artikkel = "03",
                                basertPaaAnnen = "wertwertwert",
                                delvisstans = Delvisstans(
                                        utbetaling = Utbetaling(
                                                begrunnelse = "sdfsdf\nsdfsdf\nsdf\nfsd",
                                                beloepBrutto = "234",
                                                valuta = "NZD"
                                        ),
                                        indikator = "0"
                                ),
                                type = "03",
                                begrunnelseAnnen = "sdfsdf\\nsd\\nfsd",
                                resultat = "03",
                                kjoeringsdato = "2022-10-01",
                                ukjent = Ukjent(
                                        beloepBrutto = BeloepBrutto(
                                                ytelseskomponentAnnen = "dsfsdf\\ns\\ndf\\nsdf"
                                        )
                                ),
                                virkningsdato = "2030-10-01",
                                basertPaa = "01"
                        )
                ),
                sak = Sak(
                        artikkel54 = "0",
                        kravtype = listOf(
                                KravtypeItem(
                                        datoFrist = "fasfsda"
                                )
                        ),
                        reduksjon = listOf(
                                ReduksjonItem(
                                        artikkeltype = "02"
                                ),
                                ReduksjonItem(
                                        artikkeltype = "03"
                                )
                        )
                ),
                reduksjon = listOf(
                        ReduksjonItem(
                                type = "02",
                                virkningsdato = listOf(
                                        VirkningsdatoItem(
                                                sluttdato = "2021-09-01",
                                                startdato = "2020-12-01"
                                        ),
                                        VirkningsdatoItem(
                                                sluttdato = "2022-10-01",
                                                startdato = "2034-10-01"
                                        )
                                ),
                                arsak = Arsak(
                                        annenytelseellerinntekt = "06",
                                        inntektAnnen = "adfasfsd"
                                )
                        ),
                        ReduksjonItem(
                                virkningsdato = listOf(
                                        VirkningsdatoItem(
                                                sluttdato = "2034-10-01",
                                                startdato = "2033-10-01"
                                        )
                                ),
                                arsak = Arsak(
                                        annenytelseellerinntekt = "02",
                                        inntektAnnen = "ewrwer"
                                ),
                                type = "02"
                        )
                ),
                tilleggsinformasjon = Tilleggsinformasjon(
                        andreinstitusjoner = listOf(
                                AndreinstitusjonerItem(
                                        institusjonsadresse = "asdfsdf",
                                        region = "sadfasdf",
                                        postnummer = "asdfsdf",
                                        bygningsnr = "sdafsadf",
                                        poststed = "safsd",
                                        land = "HR"
                                )
                        ),
                        person = Person(
                                pinannen = PinItem(
                                        identifikator = "retertret",
                                        sektor = "alle"
                                )

                        ),
                        dato = "2019-10-01",
                        anneninformation = "werwer\\nwer\\nwer",
                        annen = Annen(
                                institusjonsadresse = Institusjonsadresse(
                                        land = "BE"
                                )
                        ),
                        opphoer = Opphoer(
                                dato = "2022-10-01",
                                annulleringdato = "2024-10-01"
                        ),
                        saksnummerAnnen = "werwer",
                        saksnummer = "werwer",
                        artikkel48 = "0"
                )
        )
    }

}
