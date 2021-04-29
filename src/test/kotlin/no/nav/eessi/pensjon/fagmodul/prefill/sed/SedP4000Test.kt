package no.nav.eessi.pensjon.fagmodul.prefill.sed

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.NavMock
import no.nav.eessi.pensjon.eux.model.sed.Adresse
import no.nav.eessi.pensjon.eux.model.sed.AnsattSelvstendigItem
import no.nav.eessi.pensjon.eux.model.sed.BarnepassItem
import no.nav.eessi.pensjon.eux.model.sed.InformasjonBarn
import no.nav.eessi.pensjon.eux.model.sed.P4000
import no.nav.eessi.pensjon.eux.model.sed.Periode
import no.nav.eessi.pensjon.eux.model.sed.PersonArbeidogOppholdUtland
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.eux.model.sed.SedType
import no.nav.eessi.pensjon.eux.model.sed.StandardItem
import no.nav.eessi.pensjon.eux.model.sed.TrygdeTidPeriode
import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.models.PersonDataCollection
import no.nav.eessi.pensjon.fagmodul.prefill.ApiRequest
import no.nav.eessi.pensjon.fagmodul.prefill.PersonPDLMock
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillSed
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.toJson
import no.nav.eessi.pensjon.utils.typeRefs
import no.nav.eessi.pensjon.utils.validateJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.nio.file.Files
import java.nio.file.Paths


class SedP4000Test {

    var prefillSed: PrefillSed = mockk()

    private lateinit var pre4000: PrefillP4000

    @BeforeEach
    fun setup() {
        pre4000 = PrefillP4000(prefillSed)
    }

    @Test
    fun `create mock structure P4000`() {
        val result = createPersonTrygdeTidMock()
        assertNotNull(result)

        val nav = NavMock().genererNavMock()
        val p4000 = P4000(
                type = SedType.P4000,
                nav = nav,
                trygdetid = result
        )

        val p4000Json = p4000.toJson()
        val mapSED = mapJsonToAny(p4000Json, typeRefs<P4000>())

        assertNotNull(mapSED)
        assertEquals(result, mapSED.trygdetid)
    }

    @Test
    fun `create and validate P4000 from json to nav-sed back to json`() {
        //map load P4000-NAV refrence
        val path = Paths.get("src/test/resources/json/nav/P4000-NAV.json")
        val p4000file = String(Files.readAllBytes(path))
        assertNotNull(p4000file)
        validateJson(p4000file)
        val p4000 = mapJsonToAny(p4000file, typeRefs<P4000>())
        assertNotNull(p4000)
        assertNotNull(p4000.trygdetid)
        assertNotNull(p4000.trygdetid?.ansattSelvstendigPerioder)
    }

    @Test
    fun `create dummy or mock apiRequest with p4000 json as payload`() {

        val trygdetid = createPersonTrygdeTidMock()
        val payload = mapAnyToJson(trygdetid)

        val req = ApiRequest(
                sed = "P4000",
                sakId = "12231231",
                euxCaseId = "99191999911",
                aktoerId = "00000",
                buc = "P_BUC_01",
                subjectArea = "Pensjon",
                payload = payload
        )
        val json = mapAnyToJson(req)
        assertNotNull(json)
        val apireq = mapJsonToAny(json, typeRefs<ApiRequest>())
        val payjson = apireq.payload ?: ""
        assertNotNull(payjson)
        assertEquals(payload, payjson)

        val check = mapJsonToAny(payjson, typeRefs<PersonArbeidogOppholdUtland>())
        assertNotNull(check)
        assertEquals("DK", check.boPerioder!![0].land)
    }

    @Test
    fun `create insurance periods P4000 from file`() {

        val path = Paths.get("src/test/resources/json/nav/other/p4000_trygdetid_part.json")
        val jsonfile = String(Files.readAllBytes(path))
        assertNotNull(jsonfile)
        validateJson(jsonfile)

        val obj = mapJsonToAny(jsonfile, typeRefs<PersonArbeidogOppholdUtland>(), true)
        assertNotNull(obj)

        val backtojson = mapAnyToJson(obj, true)
        assertNotNull(backtojson)
        validateJson(backtojson)
        val payload = mapAnyToJson(obj)
        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        val req = ApiRequest(
                institutions = items,
                sed = "P4000",
                sakId = "12231231",
                euxCaseId = "99191999911",
                aktoerId = "00000",
                buc = "P_BUC_01",
                subjectArea = "Pensjon",
                payload = payload
        )
        assertNotNull(req)
        JSONAssert.assertEquals(jsonfile, backtojson, false)
    }

    @Test
    fun `testing prefill fails and still continues on prefillP4000`() {
        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        val req = ApiRequest(
                institutions = items,
                sed = "P4000",
                sakId = "12231231",
                euxCaseId = "99191999911",
                aktoerId = "00000",
                buc = "P_BUC_01",
                subjectArea = "Pensjon",
                payload = "{}"
        )
        val data = ApiRequest.buildPrefillDataModelOnExisting(req, "12345", null)

        val personData = PersonDataCollection(forsikretPerson = PersonPDLMock.createWith(), gjenlevendeEllerAvdod = PersonPDLMock.createWith())

        val sed = pre4000.prefill(data, personData)
        assertNull(sed.trygdetid)
    }

    @Test
    fun `validate and prefill P4000_2 from file`() {

        val path = Paths.get("src/test/resources/json/nav/other/P4000-from-frontend.json")
        val jsonfile = String(Files.readAllBytes(path))
        assertNotNull(jsonfile)
        validateJson(jsonfile)

        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        val req = ApiRequest(
                institutions = items,
                sed = "P4000",
                sakId = "12231231",
                euxCaseId = "99191999911",
                aktoerId = "1000060964183",
                buc = "P_BUC_01",
                subjectArea = "Pensjon",
                payload = jsonfile
        )
        val reqjson = mapAnyToJson(req, true)
        assertNotNull(reqjson)
        validateJson(reqjson)

        val data = ApiRequest.buildPrefillDataModelOnExisting(req, "12345", null)

        assertNotNull(data)
        assertNotNull(data.getPartSEDasJson("P4000"))
        assertEquals("12345", data.bruker.norskIdent)

        val personData = PersonDataCollection(forsikretPerson = PersonPDLMock.createWith(), gjenlevendeEllerAvdod = PersonPDLMock.createWith())

        every { prefillSed.prefill(any(), any()) } returns SED(type = SedType.P4000)

        val sed = pre4000.prefill(data, personData)
        assertNotNull(sed)
    }
}

fun createPersonTrygdeTidMock(): PersonArbeidogOppholdUtland {

    return PersonArbeidogOppholdUtland(
            foedselspermisjonPerioder = listOf(
                    StandardItem(
                            land = "NO",
                            usikkerDatoIndikator = "1",
                            annenInformasjon = "førdeslperm i Norge",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            fom = "2000-01-01",
                                            tom = "2001-01-01"
                                    )
                            )
                    ),
                    StandardItem(
                            land = "FR",
                            usikkerDatoIndikator = "0",
                            annenInformasjon = "fødselperm i frankrike",
                            periode = TrygdeTidPeriode(
                                    openPeriode = Periode(
                                            fom = "2002-01-01",
                                            extra = "98"
                                    )
                            )

                    )
            ),
            ansattSelvstendigPerioder = listOf(
                    AnsattSelvstendigItem(
                            typePeriode = "01",
                            jobbUnderAnsattEllerSelvstendig = "Kanin fabrikk ansatt",
                            annenInformasjon = "Noting else",
                            adresseFirma = Adresse(
                                    gate = "foo",
                                    postnummer = "23123",
                                    bygning = "Bygg",
                                    region = "Region",
                                    land = "NO",
                                    by = "Oslo"
                            ),
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            tom = "1995-01-01",
                                            fom = "1990-01-01"
                                    )
                            ),
                            navnFirma = "Store Kaniner AS",
                            forsikkringEllerRegistreringNr = "12123123123123123",
                            usikkerDatoIndikator = "1"
                    )
            ),
            andrePerioder = listOf(
                    StandardItem(
                            land = "SE",
                            usikkerDatoIndikator = "1",
                            annenInformasjon = "ikkenoe",
                            typePeriode = "Ingen spesielt",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            fom = "2000-01-01",
                                            tom = "2001-01-01"
                                    )
                            )
                    ),
                    StandardItem(
                            land = "SE",
                            usikkerDatoIndikator = "1",
                            annenInformasjon = "ikkenoemere",
                            typePeriode = "Leve og ha det gøy",
                            periode = TrygdeTidPeriode(
                                    openPeriode = Periode(
                                            fom = "2000-01-01",
                                            extra = "01"
                                    )
                            )
                    )
            ),
            boPerioder = listOf(
                    StandardItem(
                            land = "DK",
                            usikkerDatoIndikator = "1",
                            annenInformasjon = "Deilig i Danmark",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            fom = "2003-01-01",
                                            tom = "2004-01-01"
                                    )
                            )
                    )
            ),
            arbeidsledigPerioder = listOf(
                    StandardItem(
                            land = "IT",
                            usikkerDatoIndikator = "1",
                            annenInformasjon = "Arbeidsledig i Itelia for en kort periode.",
                            navnPaaInstitusjon = "NAV stønad for arbeidsledigetstrygd",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            fom = "2002-01-01",
                                            tom = "2004-01-01"
                                    )
                            )

                    )
            ),
            forsvartjenestePerioder = listOf(
                    StandardItem(
                            land = "SE",
                            usikkerDatoIndikator = "1",
                            annenInformasjon = "Forsvar og mlitærtjeneste fullført i Svergige",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            fom = "2001-01-01",
                                            tom = "2004-01-01"
                                    )
                            )

                    )
            ),
            sykePerioder = listOf(
                    StandardItem(
                            land = "ES",
                            usikkerDatoIndikator = "1",
                            annenInformasjon = "Sykdom og forkjølelse i Spania",
                            navnPaaInstitusjon = "Støtte for sykeophold NAV",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            fom = "2005-01-01",
                                            tom = "2007-01-01"
                                    )
                            )

                    )

            ),
            frivilligPerioder = listOf(
                    StandardItem(
                            land = "GR",
                            usikkerDatoIndikator = "1",
                            annenInformasjon = "Frivilig hjelpemedarbeider i Helles",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            fom = "2006-01-01",
                                            tom = "2007-01-01"
                                    )
                            )

                    )
            ),
            opplaeringPerioder = listOf(
                    StandardItem(
                            land = "SE",
                            usikkerDatoIndikator = "1",
                            annenInformasjon = "Opplæring høyere utdanning i Sverige",
                            navnPaaInstitusjon = "Det Akademiske instutt for høgere lære, Stockholm",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            fom = "2000-01-01",
                                            tom = "2007-01-01"
                                    )
                            )

                    )
            ),
            barnepassPerioder = listOf(
                    BarnepassItem(
                            usikkerDatoIndikator = "1",
                            annenInformasjon = "Pass av barn under opphold i Danmark",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            tom = "2008-01-01",
                                            fom = "2004-01-01"
                                    )
                            ),
                            informasjonBarn = InformasjonBarn(
                                    fornavn = "Ole",
                                    etternavn = "Olsen",
                                    foedseldato = "2002-01-01",
                                    land = "DK"
                            )
                    ),
                    BarnepassItem(
                            usikkerDatoIndikator = "1",
                            annenInformasjon = "Pass av barn under opphold i Danmark",
                            periode = TrygdeTidPeriode(
                                    lukketPeriode = Periode(
                                            tom = "2008-01-01",
                                            fom = "2004-01-01"
                                    )
                            ),
                            informasjonBarn = InformasjonBarn(
                                    fornavn = "Teddy",
                                    etternavn = "Olsen",
                                    foedseldato = "2003-01-01",
                                    land = "DK"
                            )
                    )
            )
    )

}

