package no.nav.eessi.pensjon.eux.model

import no.nav.eessi.pensjon.eux.model.sed.Adresse
import no.nav.eessi.pensjon.eux.model.sed.AndreinstitusjonerItem
import no.nav.eessi.pensjon.eux.model.sed.Arsak
import no.nav.eessi.pensjon.eux.model.sed.AvslagbegrunnelseItem
import no.nav.eessi.pensjon.eux.model.sed.BeloepBrutto
import no.nav.eessi.pensjon.eux.model.sed.BeregningItem
import no.nav.eessi.pensjon.eux.model.sed.Bruker
import no.nav.eessi.pensjon.eux.model.sed.Foedested
import no.nav.eessi.pensjon.eux.model.sed.Foreldre
import no.nav.eessi.pensjon.eux.model.sed.Grunnlag
import no.nav.eessi.pensjon.eux.model.sed.KravtypeItem
import no.nav.eessi.pensjon.eux.model.sed.Nav
import no.nav.eessi.pensjon.eux.model.sed.Opphoer
import no.nav.eessi.pensjon.eux.model.sed.Opptjening
import no.nav.eessi.pensjon.eux.model.sed.P6000
import no.nav.eessi.pensjon.eux.model.sed.P6000Pensjon
import no.nav.eessi.pensjon.eux.model.sed.Periode
import no.nav.eessi.pensjon.eux.model.sed.Person
import no.nav.eessi.pensjon.eux.model.sed.PinItem
import no.nav.eessi.pensjon.eux.model.sed.ReduksjonItem
import no.nav.eessi.pensjon.eux.model.sed.Sak
import no.nav.eessi.pensjon.eux.model.sed.StatsborgerskapItem
import no.nav.eessi.pensjon.eux.model.sed.Tilleggsinformasjon
import no.nav.eessi.pensjon.eux.model.sed.Ukjent
import no.nav.eessi.pensjon.eux.model.sed.VedtakItem
import no.nav.eessi.pensjon.eux.model.sed.VirkningsdatoItem
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.toJson
import no.nav.eessi.pensjon.utils.validateJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class SedTest {

    @Test
    fun createP6000sed() {
        val sed6000 = populerP6000Pensjon()
        assertNotNull(sed6000)

        val json = sed6000.toJson()
        //map json back to vedtak obj
        val pensjondata = mapJsonToAny<P6000>(json)
        assertNotNull(pensjondata)

        //map load vedtak-NAV refrence
        val path = Paths.get("src/test/resources/json/nav/P6000-NAV.json")
        val p6000file = String(Files.readAllBytes(path))
        assertNotNull(p6000file)
        validateJson(p6000file)

        //map vedtak-NAV back to vedtak object.
        val p6000FraJson =mapJsonToAny<P6000>((p6000file))

        assertNotNull(p6000FraJson)
    }

    @Test
    fun `create part json to object`() {
        val sed6000 = populerP6000Pensjon()
        assertNotNull(sed6000)

        val p6000Pensjon = sed6000.pensjon
        val brukerback = mapJsonToAny<P6000Pensjon>(mapAnyToJson(p6000Pensjon!!))
        assertNotNull(brukerback)
        assertEquals(p6000Pensjon.toJson(), brukerback.toJson())


        val navmock = NavMock().genererNavMock()
        val penmock = PensjonMock().genererMockData()

        val sed = P6000(
            type = SedType.P6000,
            nav = Nav(bruker = navmock.bruker),
            pensjon = P6000Pensjon(gjenlevende = penmock.gjenlevende)
        )

        val testPersjson = mapAnyToJson(sed, true)
        assertNotNull(testPersjson)

    }

    fun populerP6000Pensjon(): P6000 {
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
                                    ytelseskomponentTilleggspensjon = "234"
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
                        basertPaa = "02",
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
                        ),
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
