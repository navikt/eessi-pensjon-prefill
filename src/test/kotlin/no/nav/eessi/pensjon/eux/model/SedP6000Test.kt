package no.nav.eessi.pensjon.eux.model

import no.nav.eessi.pensjon.eux.model.sed.*
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.toJson
import no.nav.eessi.pensjon.utils.validateJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class SedP6000Test {

    @Test
    fun createP6000sed() {
        val sed6000 = populerP6000()
        assertNotNull(sed6000)

        val json = sed6000.toJson()
        assertNotNull(json)

        val pensjondata = mapJsonToAny<P6000>((json))
        assertNotNull(pensjondata)
        assertEquals(sed6000.toJson(), pensjondata.toJson())

        val path = Paths.get("src/test/resources/json/nav/P6000-NAV.json")
        val p6000file = String(Files.readAllBytes(path))
        assertNotNull(p6000file)
        validateJson(p6000file)

        //map vedtak-NAV back to vedtak object.
        val pensjondataFile = mapJsonToAny<SED>(p6000file)

        assertNotNull(pensjondataFile)
        mapAnyToJson(pensjondataFile, true)
    }

    @Test
    fun `create part json and validate to object`() {
        val sed6000 = populerP6000()
        assertNotNull(sed6000)

        val p6000Pensjon = sed6000.pensjon

        val p6000PensjonJson = mapAnyToJson(p6000Pensjon!!)
        val p6000PensjonDeserialisert = mapJsonToAny<P6000Pensjon>(p6000PensjonJson)

        assertNotNull(p6000PensjonDeserialisert)
        assertEquals(p6000Pensjon.toJson(), p6000PensjonDeserialisert.toJson())
    }

    private fun populerP6000(): P6000 {
        return P6000(
            type = SedType.P6000,
            pensjon = P6000Pensjon(
                gjenlevende = Bruker(
                    adresse = Adresse(
                        postnummer = "sdfsdf",
                        by = "sfsdf",
                        land = "BG",
                        gate = "sdfsdfs",
                        bygning = "sdfsdfs",
                        region = "dfsdf"
                    ),
                    far = Foreldre(
                        person = Person(
                            fornavn = "sdfsdf",
                        )
                    ),
                    mor = Foreldre(
                        person = Person(
                            fornavn = "asdfsdafsdf"
                        )
                    ),
                    person = Person(
                        fornavn = "gafgfdga",
                        kjoenn = "m",
                        statsborgerskap = listOf(
                            StatsborgerskapItem("AT"),
                            StatsborgerskapItem("BE"),
                            StatsborgerskapItem("GR")
                        ),
                        foedested = Foedested(
                            land = "BE",
                            region = "sdfsdfsdf",
                            by = "Testaeveisgiverby2"
                        ),
                        etternavn = "asdffsdaf",
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
                        foedselsdato = "1964-12-01"
                    )
                ),
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
                                    ytelseskomponentTilleggspensjon = "234",
                                    ytelseskomponentAnnen = "1000"
                                ),
                                periode = Periode(
                                    tom = "2030-10-01",
                                    fom = "2023-10-01"
                                ),
                                valuta = "HUF",
                                utbetalingshyppighet = "maaned_12_per_aar"
                            ),
                            BeregningItem(
                                beloepBrutto = BeloepBrutto(
                                    beloep = "234",
                                    ytelseskomponentTilleggspensjon = "22",
                                    ytelseskomponentGrunnpensjon = "342"
                                ),
                                periode = Periode(
                                    fom = "2020-10-01",
                                    tom = "2025-10-01"
                                ),
                                utbetalingshyppighet = "annet",
                                valuta = "ISK"
                            )
                        ),
                        basertPaa = BasertPaa.basert_på_arbeid,
                        virkningsdato = "2020-10-01",
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
                        )
                    ),
                    VedtakItem(
                        beregning = listOf(
                            BeregningItem(
                                valuta = "ERN",
                                beloepBrutto = BeloepBrutto(
                                    ytelseskomponentTilleggspensjon = "12",
                                    ytelseskomponentGrunnpensjon = "122",
                                    beloep = "234"
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
                        basertPaaAnnen = "wertwertwert",
                        type = "03",
                        resultat = "03",
                        ukjent = Ukjent(
                            beloepBrutto = BeloepBrutto(
                                ytelseskomponentAnnen = "dsfsdf\\ns\\ndf\\nsdf"
                            )
                        ),
                        virkningsdato = "2030-10-01",
                        basertPaa = BasertPaa.basert_på_botid
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
                        aarsak = Arsak(
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
                        aarsak = Arsak(
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
                            bygningsnavn = "sdafsadf",
                            poststed = "safsd",
                            land = "HR"
                        )
                    ),
                    dato = "2019-10-01",
                    opphoer = Opphoer(
                        dato = "2022-10-01",
                        annulleringdato = "2024-10-01"
                    ),
                    artikkel48 = "0"
                )
            )
        )
    }

}
