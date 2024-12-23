package no.nav.eessi.pensjon.prefill.person

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.*
import no.nav.eessi.pensjon.eux.model.sed.SivilstandRina.gift
import no.nav.eessi.pensjon.eux.model.sed.SivilstandRina.registrert_partnerskap
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.kodeverk.PostnummerService
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.model.*
import no.nav.eessi.pensjon.prefill.LagPdlPerson
import no.nav.eessi.pensjon.prefill.LagPdlPerson.Companion.createPersonMedEktefellePartner
import no.nav.eessi.pensjon.prefill.LagPdlPerson.Companion.lagPerson
import no.nav.eessi.pensjon.prefill.LagPdlPerson.Companion.medAdresse
import no.nav.eessi.pensjon.prefill.LagPdlPerson.Companion.medBarn
import no.nav.eessi.pensjon.prefill.LagPdlPerson.Companion.medForeldre
import no.nav.eessi.pensjon.prefill.LagPdlPerson.Companion.medKontaktadresseUtland
import no.nav.eessi.pensjon.prefill.models.KrrPerson
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.shared.api.BankOgArbeid
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.person.Fodselsnummer
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.toJson
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.time.LocalDate
import java.time.LocalDateTime

class PrefillPDLNavTest {

    private val personService: PersonService = mockk()
    private val kodeverkClient: KodeverkClient = mockk(relaxed = true)

    lateinit var prefillPDLNav: PrefillPDLNav

    private val somePenSaksnr = "somePenSaksnr"
    private val someInstitutionId = "enInstId"
    private val someIntitutionNavn = "instNavn"

    @BeforeEach
    fun beforeStart() {
        every { kodeverkClient.finnLandkode(eq("NOR")) } returns "NO"
        every { kodeverkClient.finnLandkode(eq("SWE")) } returns "SE"

        prefillPDLNav = PrefillPDLNav(
            PrefillPDLAdresse(PostnummerService(), kodeverkClient, personService), someInstitutionId, someIntitutionNavn
        )
    }

    @Test
    fun `minimal prefill med forsikret som har en NPID i stedet for fnr`() {
        val forsikretSinNpid = "01220049651"
        val prefillData = PrefillDataModelMother.initialPrefillDataModel(
            SedType.P2100, pinId = forsikretSinNpid, penSaksnummer = somePenSaksnr, avdod = null
        )

        val forsikret = PdlPerson(
            identer = listOf(IdentInformasjon("01220049651", IdentGruppe.NPID)),
            navn = Navn("OLE", null, "OLSEN", null, null, null, LagPdlPerson.mockMeta()),
            adressebeskyttelse = emptyList(),
            bostedsadresse = null,
            oppholdsadresse = null,
            statsborgerskap = listOf(
                Statsborgerskap(
                    "NOR", LocalDate.of(2000, 10, 1), LocalDate.of(2300, 10, 1), LagPdlPerson.mockMeta()
                )
            ),
            foedsel = Foedsel(LocalDate.of(80, 12, 1), "NOR", null, null, null, LagPdlPerson.mockMeta()),
            geografiskTilknytning = null,
            kjoenn = Kjoenn(KjoennType.MANN, null, LagPdlPerson.mockMeta()),
            doedsfall = Doedsfall(metadata = LagPdlPerson.mockMeta()),
            forelderBarnRelasjon = emptyList(),
            sivilstand = emptyList(),
            kontaktadresse = null,
            utenlandskIdentifikasjonsnummer = emptyList()
        )

        val foreldreFdato = forsikret.foedsel?.foedselsdato?.toString()

        val personDataCollection = PersonDataCollection(
            forsikretPerson = forsikret,
            ektefellePerson = null,
            gjenlevendeEllerAvdod = forsikret
        )

        val actual = prefillPDLNav.prefill(
            prefillData.penSaksnummer,
            prefillData.bruker,
            prefillData.avdod,
            personDataCollection,
            prefillData.getBankOgArbeidFromRequest(),
            null,
            null
        )
        val expected = Nav(
            eessisak = listOf(
                EessisakItem(
                    institusjonsid = someInstitutionId,
                    institusjonsnavn = someIntitutionNavn,
                    saksnummer = somePenSaksnr,
                    land = "NO"
                )
            ),
            bruker = Bruker(
                person = lagNavPerson(
                    forsikretSinNpid,
                    "OLE",
                    "OLSEN",
                    foreldreFdato!!,
                    someInstitutionId,
                    someIntitutionNavn,
                    krrPerson = KrrPerson(false,"ola@nav.no", "11223344")
                ),
                adresse = lagTomAdresse(),
            ),
        )

        assertEquals(expected, actual)
        JSONAssert.assertEquals(expected.toJsonSkipEmpty(), actual.toJsonSkipEmpty(), true)

    }

    @Test
    fun `minimal preutfylling av forsikret som har Norsk statsborgerskap med null som gyldigFraOgMed dato`() {
        val forsikretSinNpid = "01220049651"
        val prefillData = PrefillDataModelMother.initialPrefillDataModel(
            SedType.P2100, pinId = forsikretSinNpid, penSaksnummer = somePenSaksnr, avdod = null
        )

        val forsikret = PdlPerson(
            identer = listOf(IdentInformasjon("01220049651", IdentGruppe.NPID)),
            navn = Navn("OLE", null, "OLSEN", null, null, null, LagPdlPerson.mockMeta()),
            adressebeskyttelse = emptyList(),
            bostedsadresse = null,
            oppholdsadresse = null,
            statsborgerskap = listOf(
                Statsborgerskap(
                    "NOR", null, null, LagPdlPerson.mockMeta()
                )
            ),
            foedsel = Foedsel(LocalDate.of(80, 12, 1), "NOR", null, null, null, LagPdlPerson.mockMeta()),
            geografiskTilknytning = null,
            kjoenn = Kjoenn(KjoennType.MANN, null, LagPdlPerson.mockMeta()),
            doedsfall = Doedsfall(metadata = LagPdlPerson.mockMeta()),
            forelderBarnRelasjon = emptyList(),
            sivilstand = emptyList(),
            kontaktadresse = null,
            utenlandskIdentifikasjonsnummer = emptyList()
        )

        val foreldreFdato = forsikret.foedsel?.foedselsdato?.toString()

        val personDataCollection = PersonDataCollection(
            forsikretPerson = forsikret,
            ektefellePerson = null,
            gjenlevendeEllerAvdod = forsikret
        )

        val actual = prefillPDLNav.prefill(
            prefillData.penSaksnummer,
            prefillData.bruker,
            prefillData.avdod,
            personDataCollection,
            prefillData.getBankOgArbeidFromRequest(),
            null,
            null
        )
        val expected = Nav(
            eessisak = listOf(
                EessisakItem(
                    institusjonsid = someInstitutionId,
                    institusjonsnavn = someIntitutionNavn,
                    saksnummer = somePenSaksnr,
                    land = "NO"
                )
            ),
            bruker = Bruker(
                person = lagNavPerson(
                    forsikretSinNpid,
                    "OLE",
                    "OLSEN",
                    foreldreFdato!!,
                    someInstitutionId,
                    someIntitutionNavn,
                    krrPerson = KrrPerson(false,"ola@nav.no", "11223344")
                ),
                adresse = lagTomAdresse(),
            ),
        )

        println("**** $actual")
        assertEquals(expected, actual)
        JSONAssert.assertEquals(expected.toJsonSkipEmpty(), actual.toJsonSkipEmpty(), true)

    }

    @Test
    fun `minimal prefill med barn`() {
        val foreldersPin = FodselsnummerGenerator.generateFnrForTest(40)
        val prefillData = PrefillDataModelMother.initialPrefillDataModel(
            SedType.P2100, pinId = foreldersPin, penSaksnummer = somePenSaksnr, avdod = null
        )
        val barnetsPin = FodselsnummerGenerator.generateFnrForTest(13)

        val forelder = lagPerson(foreldersPin, "Christopher", "Robin", sivilstand = emptyList()).medBarn(barnetsPin)
        val barn = lagPerson(barnetsPin, "Ole", "Brum", sivilstand = emptyList()).medForeldre(forelder)

        val foreldreFdato = forelder.foedsel?.foedselsdato?.toString()
        val barnFdato = barn.foedsel?.foedselsdato?.toString()

        val personDataCollection = PersonDataCollection(
            forsikretPerson = forelder,
            ektefellePerson = null,
            gjenlevendeEllerAvdod = forelder,
            barnPersonList = listOf(barn)
        )

        val actual = prefillPDLNav.prefill(
            prefillData.penSaksnummer,
            prefillData.bruker,
            prefillData.avdod,
            personDataCollection,
            prefillData.getBankOgArbeidFromRequest(),
            null,
            null
        )
        val expected = Nav(
            eessisak = listOf(
                EessisakItem(
                    institusjonsid = someInstitutionId,
                    institusjonsnavn = someIntitutionNavn,
                    saksnummer = somePenSaksnr,
                    land = "NO"
                )
            ), bruker = Bruker(
                person = lagNavPerson(
                    foreldersPin,
                    "Christopher",
                    "Robin",
                    foreldreFdato!!,
                    someInstitutionId,
                    someIntitutionNavn,
                    krrPerson = KrrPerson(false,"ola@nav.no", "11223344")
                ), adresse = lagTomAdresse()
            ), barn = listOf(
                BarnItem(
                    person = lagNavPerson(
                        barnetsPin, "Ole", "Brum", barnFdato!!, someInstitutionId, someIntitutionNavn, krrPerson = null
                    ), far = Foreldre(
                        Person(
                            fornavn = "Christopher", pin = listOf(
                                PinItem(
                                    identifikator = foreldersPin,
                                    land = "NO",
                                    institusjonsid = "enInstId",
                                    institusjonsnavn = "instNavn"
                                )
                            )
                        )
                    ), relasjontilbruker = "01"
                )
            )
        )

        assertEquals(expected, actual)
        JSONAssert.assertEquals(expected.toJsonSkipEmpty(), actual.toJsonSkipEmpty(), true)

    }

    @Test
    fun `prefill med barn og relasjon Far`() {
        val somePersonNr = FodselsnummerGenerator.generateFnrForTest(57)
        val someBarnPersonNr = FodselsnummerGenerator.generateFnrForTest(17)

        val prefillData = PrefillDataModelMother.initialPrefillDataModel(
            SedType.P2100,
            pinId = somePersonNr,
            avdod = PersonInfo(someBarnPersonNr, aktorId = "123232312312"),
            penSaksnummer = somePenSaksnr
        )

        val far = lagPerson(somePersonNr, "Ole", "Brum", sivilstand = emptyList()).medBarn(someBarnPersonNr)
        val barn = lagPerson(someBarnPersonNr, "Nasse", "Nøff", sivilstand = emptyList()).medForeldre(far)

        //fdato
        val farfdato = far.foedsel?.foedselsdato?.toString()
        val barnfdato = barn.foedsel?.foedselsdato?.toString()

        val personDataCollection = PersonDataCollection(
            forsikretPerson = far,
            ektefellePerson = null,
            gjenlevendeEllerAvdod = far,
            barnPersonList = listOf(barn)
        )

        val actual = prefillPDLNav.prefill(
            prefillData.penSaksnummer,
            prefillData.bruker,
            prefillData.avdod,
            personDataCollection,
            prefillData.getBankOgArbeidFromRequest(),
            null,
            null
        )
        val expected = Nav(
            eessisak = listOf(
                EessisakItem(
                    institusjonsid = someInstitutionId,
                    institusjonsnavn = someIntitutionNavn,
                    saksnummer = somePenSaksnr,
                    land = "NO"
                )
            ), bruker = Bruker(
                person = lagNavPerson(
                    somePersonNr,
                    "Ole",
                    "Brum",
                    farfdato!!,
                    someInstitutionId,
                    someIntitutionNavn,
                    krrPerson = KrrPerson(false,"ola@nav.no", "11223344")
                ), adresse = lagTomAdresse()
            ), barn = listOf(
                BarnItem(
                    mor = null, far = Foreldre(
                        Person(
                            fornavn = "Ole", pin = listOf(
                                PinItem(
                                    identifikator = somePersonNr,
                                    land = "NO",
                                    institusjonsid = "enInstId",
                                    institusjonsnavn = "instNavn"
                                )
                            )
                        )
                    ), person = lagNavPerson(
                        someBarnPersonNr,
                        "Nasse",
                        "Nøff",
                        barnfdato!!,
                        someInstitutionId,
                        someIntitutionNavn,
                        krrPerson = null
                    ), relasjontilbruker = "01"
                )
            )
        )

        println("**** $expected")
        println("@@@@@@ $actual")
        assertEquals(expected, actual)
        JSONAssert.assertEquals(expected.toJsonSkipEmpty(), actual.toJsonSkipEmpty(), true)
    }

    @Test
    fun `prefill med barn og relasjon Far npid`() {
        val somePersonNr = "01220049651"
        val someBarnPersonNr = FodselsnummerGenerator.generateFnrForTest(17)

        val prefillData = PrefillDataModelMother.initialPrefillDataModel(
            SedType.P2100,
            pinId = somePersonNr,
            avdod = PersonInfo(someBarnPersonNr, "123232312312"),
            penSaksnummer = somePenSaksnr
        )

        val far = lagPerson(somePersonNr, "Ole", "Brum", sivilstand = emptyList()).medBarn(someBarnPersonNr)
        val barn = lagPerson(someBarnPersonNr, "Nasse", "Nøff", sivilstand = emptyList()).medForeldre(far)

        //fdato
        val farfdato = far.foedsel?.foedselsdato?.toString()
        val barnfdato = barn.foedsel?.foedselsdato?.toString()

        val personDataCollection = PersonDataCollection(
            forsikretPerson = far,
            ektefellePerson = null,
            gjenlevendeEllerAvdod = far,
            barnPersonList = listOf(barn)
        )

        val actual = prefillPDLNav.prefill(
            prefillData.penSaksnummer,
            prefillData.bruker,
            prefillData.avdod,
            personDataCollection,
            prefillData.getBankOgArbeidFromRequest(),
            null,
            null
        )
        val expected = Nav(
            eessisak = listOf(
                EessisakItem(
                    institusjonsid = someInstitutionId,
                    institusjonsnavn = someIntitutionNavn,
                    saksnummer = somePenSaksnr,
                    land = "NO"
                )
            ), bruker = Bruker(
                person = lagNavPerson(
                    somePersonNr,
                    "Ole",
                    "Brum",
                    farfdato!!,
                    someInstitutionId,
                    someIntitutionNavn,
                    krrPerson = KrrPerson(false,"ola@nav.no", "11223344")
                ), adresse = lagTomAdresse()
            ), barn = listOf(
                BarnItem(
                    mor = null, far = Foreldre(
                        Person(
                            fornavn = "Ole", pin = listOf(
                                PinItem(
                                    identifikator = somePersonNr,
                                    land = "NO",
                                    institusjonsid = "enInstId",
                                    institusjonsnavn = "instNavn"
                                )
                            )
                        )
                    ), person = lagNavPerson(
                        someBarnPersonNr,
                        "Nasse",
                        "Nøff",
                        barnfdato!!,
                        someInstitutionId,
                        someIntitutionNavn,
                        krrPerson = null
                    ), relasjontilbruker = "06"
                )
            )
        )

        assertEquals(expected, actual)
        JSONAssert.assertEquals(expected.toJsonSkipEmpty(), actual.toJsonSkipEmpty(), true)
    }

    @Test
    fun `prefill med familie relasjon person og ektefelle`() {

        val somePersonNr = FodselsnummerGenerator.generateFnrForTest(60)
        val somerEktefellePersonNr = FodselsnummerGenerator.generateFnrForTest(50)

        val personFdato = Fodselsnummer.fra(somePersonNr)?.getBirthDate().toString()
        val ektefellFdato = Fodselsnummer.fra(somerEktefellePersonNr)?.getBirthDate().toString()

        //ektefelle
        val pair = createPersonMedEktefellePartner(somePersonNr, somerEktefellePersonNr, Sivilstandstype.GIFT)

        val person = pair.first
        val ektefelle = pair.second

        val prefillData = PrefillDataModelMother.initialPrefillDataModel(
            SedType.P2000, pinId = somePersonNr, penSaksnummer = somePenSaksnr
        )
        val personDataCollection = PersonDataCollection(
            forsikretPerson = person,
            ektefellePerson = ektefelle,
            gjenlevendeEllerAvdod = person,
            barnPersonList = emptyList()
        )

        val actual = prefillPDLNav.prefill(
            prefillData.penSaksnummer,
            prefillData.bruker,
            prefillData.avdod,
            personDataCollection,
            prefillData.getBankOgArbeidFromRequest(),
            null,
            null
        )

        val expected = Nav(
            eessisak = listOf(
                EessisakItem(
                    institusjonsid = someInstitutionId,
                    institusjonsnavn = someIntitutionNavn,
                    saksnummer = somePenSaksnr,
                    land = "NO"
                )
            ), bruker = Bruker(
                person = lagNavPerson(
                    somePersonNr,
                    "Ola",
                    "Testbruker",
                    personFdato,
                    someInstitutionId,
                    someIntitutionNavn,
                    sivilstand = listOf(SivilstandItem("2000-10-01", gift)),
                    krrPerson = KrrPerson(false,"ola@nav.no", "11223344")
                ), adresse = lagTomAdresse()
            ), ektefelle = Ektefelle(
                person = lagNavPerson(
                    somerEktefellePersonNr,
                    "Jonna",
                    "Dolla",
                    ektefellFdato,
                    someInstitutionId,
                    someIntitutionNavn,
                    "K",
                    sivilstand = listOf(SivilstandItem("2000-10-01", gift)),
                    krrPerson = KrrPerson(false,"ola@nav.no", "11223344")
                ), type = "ektefelle"
            )
        )

        assertEquals(expected, actual)
        JSONAssert.assertEquals(expected.toJsonSkipEmpty(), actual.toJsonSkipEmpty(), true)

    }

    @Test
    fun `prefill komplett familierelasjon og sivilstand`() {
        //generer fnr
        val farfnr = FodselsnummerGenerator.generateFnrForTest(42)
        val morfnr = FodselsnummerGenerator.generateFnrForTest(41)
        val barn1 = FodselsnummerGenerator.generateFnrForTest(11)
        val barn2 = FodselsnummerGenerator.generateFnrForTest(13)

        val personFdato = Fodselsnummer.fra(farfnr)?.getBirthDate().toString()
        val ektefellFdato = Fodselsnummer.fra(morfnr)?.getBirthDate().toString()
        val barnetfdato = Fodselsnummer.fra(barn1)?.getBirthDate().toString()
        val barntofdato = Fodselsnummer.fra(barn2)?.getBirthDate().toString()

        //far og mor i pair
        val pair = createPersonMedEktefellePartner(farfnr, morfnr, Sivilstandstype.GIFT)

        //far og mor med barn
        val far = pair.first.medAdresse("STORGATA").medBarn(barn1).medBarn(barn2)
        val mor = pair.second.medAdresse("STORGATA").medBarn(barn1).medBarn(barn2)

        //barn
        val barnet = lagPerson(barn1, fornavn = "OLE", etternavn = "BRUM").medForeldre(far).medForeldre(mor)
        val barnto = lagPerson(barn2, fornavn = "NASSE", etternavn = "NØFF").medForeldre(far).medForeldre(mor)

        val personDataCollection = PersonDataCollection(
            forsikretPerson = far,
            ektefellePerson = mor,
            gjenlevendeEllerAvdod = far,
            barnPersonList = listOf(barnet, barnto)
        )
        val prefillData =
            PrefillDataModelMother.initialPrefillDataModel(SedType.P2200, pinId = farfnr, penSaksnummer = somePenSaksnr)

        val actual = prefillPDLNav.prefill(
            prefillData.penSaksnummer,
            prefillData.bruker,
            prefillData.avdod,
            personDataCollection,
            prefillData.getBankOgArbeidFromRequest(),
            null,
            null
        )

        val expected = Nav(
            eessisak = listOf(
                EessisakItem(
                    institusjonsid = someInstitutionId,
                    institusjonsnavn = someIntitutionNavn,
                    saksnummer = somePenSaksnr,
                    land = "NO"
                )
            ), bruker = Bruker(
                person = lagNavPerson(
                    farfnr,
                    "Ola",
                    "Testbruker",
                    personFdato,
                    someInstitutionId,
                    someIntitutionNavn,
                    sivilstand = listOf(SivilstandItem("2000-10-01", gift)),
                    krrPerson = KrrPerson(false,"ola@nav.no", "11223344")
                ), adresse = Adresse("STORGATA 12", postnummer = "0101", by = "OSLO", land = "NO")
            ), ektefelle = Ektefelle(
                person = lagNavPerson(
                    morfnr,
                    "Jonna",
                    "Dolla",
                    ektefellFdato,
                    someInstitutionId,
                    someIntitutionNavn,
                    "K",
                    sivilstand = listOf(SivilstandItem("2000-10-01", gift)),
                    krrPerson = KrrPerson(false,"ola@nav.no", "11223344")
                ), type = "ektefelle"
            ), barn = listOf(
                BarnItem(
                    mor = Foreldre(
                        Person(
                            fornavn = "Jonna", pin = listOf(
                                PinItem(
                                    identifikator = morfnr,
                                    land = "NO",
                                    institusjonsid = "enInstId",
                                    institusjonsnavn = "instNavn"
                                )
                            )
                        )
                    ), far = Foreldre(
                        Person(
                            fornavn = "Ola", pin = listOf(
                                PinItem(
                                    identifikator = farfnr,
                                    land = "NO",
                                    institusjonsid = "enInstId",
                                    institusjonsnavn = "instNavn"
                                )
                            )
                        )
                    ), person = lagNavPerson(
                        barn1,
                        "OLE",
                        "BRUM",
                        barnetfdato,
                        someInstitutionId,
                        someIntitutionNavn,
                        krrPerson = null
                    ), relasjontilbruker = "01"
                ), BarnItem(
                    mor = Foreldre(
                        Person(
                            fornavn = "Jonna", pin = listOf(
                                PinItem(
                                    identifikator = morfnr,
                                    land = "NO",
                                    institusjonsid = "enInstId",
                                    institusjonsnavn = "instNavn"
                                )
                            )
                        )
                    ), far = Foreldre(
                        Person(
                            fornavn = "Ola", pin = listOf(
                                PinItem(
                                    identifikator = farfnr,
                                    land = "NO",
                                    institusjonsid = "enInstId",
                                    institusjonsnavn = "instNavn"
                                )
                            )
                        )
                    ), person = lagNavPerson(
                        barn2,
                        "NASSE",
                        "NØFF",
                        barntofdato,
                        someInstitutionId,
                        someIntitutionNavn,
                        krrPerson = null
                    ), relasjontilbruker = "01"
                )
            )
        )

        assertEquals(expected, actual)
        JSONAssert.assertEquals(expected.toJsonSkipEmpty(), actual.toJsonSkipEmpty(), true)

    }


    @Test
    fun `prefill med samboerpar relasjon person og partner`() {
        val somePersonNr = FodselsnummerGenerator.generateFnrForTest(60)
        val somerEktefellePersonNr = FodselsnummerGenerator.generateFnrForTest(50)

        val personfnr = Fodselsnummer.fra(somePersonNr)
        val ektefnr = Fodselsnummer.fra(somerEktefellePersonNr)
        val personFdato = personfnr?.getBirthDate().toString()
        val ektefellFdato = ektefnr?.getBirthDate().toString()

        val pair =
            createPersonMedEktefellePartner(somePersonNr, somerEktefellePersonNr, Sivilstandstype.REGISTRERT_PARTNER)

        val person = pair.first
        val partner = pair.second


        val prefillData = PrefillDataModelMother.initialPrefillDataModel(
            SedType.P2000, pinId = somePersonNr, penSaksnummer = somePenSaksnr
        )
        val personDataCollection = PersonDataCollection(
            forsikretPerson = person,
            ektefellePerson = partner,
            gjenlevendeEllerAvdod = person,
            barnPersonList = emptyList()
        )

        val actual = prefillPDLNav.prefill(
            prefillData.penSaksnummer,
            prefillData.bruker,
            prefillData.avdod,
            personDataCollection,
            prefillData.getBankOgArbeidFromRequest(),
            null,
            null
        )

        val expected = Nav(
            eessisak = listOf(
                EessisakItem(
                    institusjonsid = someInstitutionId,
                    institusjonsnavn = someIntitutionNavn,
                    saksnummer = somePenSaksnr,
                    land = "NO"
                )
            ), bruker = Bruker(
                person = lagNavPerson(
                    somePersonNr,
                    "Ola",
                    "Testbruker",
                    personFdato,
                    someInstitutionId,
                    someIntitutionNavn,
                    sivilstand = listOf(SivilstandItem("2000-10-01", registrert_partnerskap)),
                    krrPerson = KrrPerson(false,"ola@nav.no", "11223344")
                ), adresse = lagTomAdresse()
            ), ektefelle = Ektefelle(
                person = lagNavPerson(
                    somerEktefellePersonNr,
                    "Jonna",
                    "Dolla",
                    ektefellFdato,
                    someInstitutionId,
                    someIntitutionNavn,
                    "K",
                    sivilstand = listOf(SivilstandItem("2000-10-01", registrert_partnerskap)),
                    krrPerson = KrrPerson(false,"ola@nav.no", "11223344")
                ), type = "part_i_et_registrert_partnerskap"
            )
        )

        assertEquals(expected, actual)
        JSONAssert.assertEquals(expected.toJsonSkipEmpty(), actual.toJsonSkipEmpty(), true)

    }

    @Test
    fun `prefill person singel med mellomnavn`() {
        val somePersonNr = FodselsnummerGenerator.generateFnrForTest(60)

        val personfnr = Fodselsnummer.fra(somePersonNr)
        val personFdato = personfnr?.getBirthDate().toString()

        val single = lagPerson(somePersonNr, "Ola", "Testbruker").copy(
            navn = Navn(
                "Fornavn Ole", "Mellomnavn Mellomn", "Test Bruker", null, null, null, LagPdlPerson.mockMeta()
            )
        )

        val prefillData = PrefillDataModelMother.initialPrefillDataModel(
            SedType.P2000, pinId = somePersonNr, penSaksnummer = somePenSaksnr
        )
        val personDataCollection = PersonDataCollection(
            forsikretPerson = single,
            ektefellePerson = null,
            gjenlevendeEllerAvdod = single,
            barnPersonList = emptyList()
        )

        val actual = prefillPDLNav.prefill(
            prefillData.penSaksnummer,
            prefillData.bruker,
            prefillData.avdod,
            personDataCollection,
            prefillData.getBankOgArbeidFromRequest(),
            null,
            null
        )

        val expected = Nav(
            eessisak = listOf(
                EessisakItem(
                    institusjonsid = someInstitutionId,
                    institusjonsnavn = someIntitutionNavn,
                    saksnummer = somePenSaksnr,
                    land = "NO"
                )
            ), bruker = Bruker(
                person = lagNavPerson(
                    somePersonNr,
                    "Fornavn Ole Mellomnavn Mellomn",
                    "Test Bruker",
                    personFdato,
                    someInstitutionId,
                    someIntitutionNavn,
                    krrPerson = KrrPerson(false,"ola@nav.no", "11223344")
                ), adresse = lagTomAdresse()
            )
        )

        assertEquals(expected, actual)
        JSONAssert.assertEquals(expected.toJsonSkipEmpty(), actual.toJsonSkipEmpty(), true)

    }

    @Test
    fun `prefill person med utlandsadresse fra oppholdsadresse`() {
        val somePersonNr = FodselsnummerGenerator.generateFnrForTest(60)

        val gateadresse = "Storavegsentra 12, Noenhusbygg, 2012 SE, Østaby"
        val single = lagPerson(somePersonNr).copy(
            bostedsadresse = null, oppholdsadresse = Oppholdsadresse(
                LocalDateTime.of(2000, 10, 2, 9, 32, 1), null, null, UtenlandskAdresse(
                    gateadresse, "örasund", null, "SWE", null, null, null
                ), LagPdlPerson.mockMeta()
            )
        )

        val prefillData = PrefillDataModelMother.initialPrefillDataModel(
            SedType.P2000, pinId = somePersonNr, penSaksnummer = somePenSaksnr
        )
        val personDataCollection = PersonDataCollection(
            forsikretPerson = single,
            ektefellePerson = null,
            gjenlevendeEllerAvdod = single,
            barnPersonList = emptyList()
        )

        val actual = prefillPDLNav.prefill(
            prefillData.penSaksnummer,
            prefillData.bruker,
            prefillData.avdod,
            personDataCollection,
            prefillData.getBankOgArbeidFromRequest(),
            null,
            null
        )

        assertEquals(gateadresse, actual.bruker?.adresse?.gate)
    }

    @Test
    fun `prefill person med kontaktadresse og utlandsadresse i frittformat`() {
        val somePersonNr = FodselsnummerGenerator.generateFnrForTest(60)
        val personfnr = Fodselsnummer.fra(somePersonNr)
        val personFdato = personfnr?.getBirthDate().toString()

        val single = lagPerson(somePersonNr).copy(
            bostedsadresse = null, oppholdsadresse = Oppholdsadresse(
                LocalDateTime.of(2000, 10, 2, 9, 32, 1), null, null, UtenlandskAdresse(
                    "Adresselinje 1, Adresselinje 2, Adresselinje 3", null, null, "SWE", null, null, null
                ), LagPdlPerson.mockMeta()
            )
        ).medKontaktadresseUtland()

        val prefillData = PrefillDataModelMother.initialPrefillDataModel(
            SedType.P2000, pinId = somePersonNr, penSaksnummer = somePenSaksnr
        )
        val personDataCollection = PersonDataCollection(
            forsikretPerson = single,
            ektefellePerson = null,
            gjenlevendeEllerAvdod = single,
            barnPersonList = emptyList()
        )

        val actual = prefillPDLNav.prefill(
            prefillData.penSaksnummer,
            prefillData.bruker,
            prefillData.avdod,
            personDataCollection,
            prefillData.getBankOgArbeidFromRequest(),
            null,
            null
        )

        val expected = Nav(
            eessisak = listOf(
                EessisakItem(
                    institusjonsid = someInstitutionId,
                    institusjonsnavn = someIntitutionNavn,
                    saksnummer = somePenSaksnr,
                    land = "NO"
                )
            ), bruker = Bruker(
                person = lagNavPerson(
                    somePersonNr,
                    "OLE",
                    "OLSEN",
                    personFdato,
                    someInstitutionId,
                    someIntitutionNavn,
                    krrPerson = KrrPerson(false,"ola@nav.no", "11223344")
                ), adresse = Adresse(
                    "Adresselinje 1", "Adresselinje 2", "Adresselinje 3", null, null, land = "SE"
                )
            )
        )

        JSONAssert.assertEquals(expected.toJsonSkipEmpty(), actual.toJsonSkipEmpty(), true)
    }

    @Test
    fun `prefill person utland med oppholdsadresse samt kontaktadresse i frittformat`() {
        val somePersonNr = FodselsnummerGenerator.generateFnrForTest(60)
        val personfnr = Fodselsnummer.fra(somePersonNr)
        val personFdato = personfnr?.getBirthDate().toString()

        val single = lagPerson(somePersonNr).copy(
            bostedsadresse = null, oppholdsadresse = Oppholdsadresse(
                LocalDateTime.of(2000, 10, 2, 9, 32, 1), null, null, UtenlandskAdresse(
                    "Adresselinje 1, Adresselinje 2, Adresselinje 3",
                    "utenland by",
                    null,
                    "SWE",
                    null,
                    "postkoden",
                    null
                ), LagPdlPerson.mockMeta()
            )
        ).medKontaktadresseUtland()

        val prefillData = PrefillDataModelMother.initialPrefillDataModel(
            SedType.P2000, pinId = somePersonNr, penSaksnummer = somePenSaksnr
        )
        val personDataCollection = PersonDataCollection(
            forsikretPerson = single,
            ektefellePerson = null,
            gjenlevendeEllerAvdod = single,
            barnPersonList = emptyList()
        )

        val actual = prefillPDLNav.prefill(
            prefillData.penSaksnummer,
            prefillData.bruker,
            prefillData.avdod,
            personDataCollection,
            prefillData.getBankOgArbeidFromRequest(),
            null,
            null
        )

        println("*".repeat(20))
        println(actual.toJsonSkipEmpty())
        println("*".repeat(20))

        val expected = Nav(
            eessisak = listOf(
                EessisakItem(
                    institusjonsid = someInstitutionId,
                    institusjonsnavn = someIntitutionNavn,
                    saksnummer = somePenSaksnr,
                    land = "NO"
                )
            ), bruker = Bruker(
                person = lagNavPerson(
                    somePersonNr,
                    "OLE",
                    "OLSEN",
                    personFdato,
                    someInstitutionId,
                    someIntitutionNavn,
                    krrPerson = KrrPerson(false,"ola@nav.no", "11223344")
                ), adresse = Adresse(
                    "Adresselinje 1", "Adresselinje 2", "Adresselinje 3", null, null, land = "SE"
                )
            )
        )
        println(expected.toJsonSkipEmpty())

        JSONAssert.assertEquals(expected.toJsonSkipEmpty(), actual.toJsonSkipEmpty(), true)
    }


    @Test
    fun `minimal prefill med brukerinfo på request`() {
        val somePersonNr = FodselsnummerGenerator.generateFnrForTest(60)
        val personfnr = Fodselsnummer.fra(somePersonNr)
        val personFdato = personfnr?.getBirthDate().toString()

        val prefillData = PrefillDataModelMother.initialPrefillDataModel(
            SedType.P2000, pinId = somePersonNr, penSaksnummer = somePenSaksnr
        ).apply {
            partSedAsJson["PersonInfo"] = mapAnyToJson(
                BankOgArbeid(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "Nordnb",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )
            )
        }

        val person = lagPerson(somePersonNr, "Ole", "Brum")
        val personDataCollection = PersonDataCollection(
            forsikretPerson = person,
            ektefellePerson = null,
            gjenlevendeEllerAvdod = person,
            barnPersonList = emptyList()
        )

        val actual = prefillPDLNav.prefill(
            prefillData.penSaksnummer,
            prefillData.bruker,
            prefillData.avdod,
            personDataCollection,
            prefillData.getBankOgArbeidFromRequest(),
            null,
            null
        )

        val expected = Nav(
            eessisak = listOf(
                EessisakItem(
                    institusjonsid = someInstitutionId,
                    institusjonsnavn = someIntitutionNavn,
                    saksnummer = somePenSaksnr,
                    land = "NO"
                )
            ), bruker = Bruker(
                person = lagNavPerson(
                    somePersonNr,
                    "Ole",
                    "Brum",
                    personFdato,
                    someInstitutionId,
                    someIntitutionNavn,
                    krrPerson = KrrPerson(false,"ola@nav.no", "11223344")
                ), arbeidsforhold = listOf(
                    ArbeidsforholdItem(
                        planlagtstartdato = "",
                        arbeidstimerperuke = "",
                        planlagtpensjoneringsdato = "",
                        yrke = "",
                        type = "",
                        sluttdato = ""
                    )
                ), bank = Bank(
                    navn = "Nordnb", konto = Konto(
                        sepa = Sepa(), innehaver = Innehaver(
                            rolle = "01", navn = "Nordnb"
                        )
                    ), adresse = Adresse()
                ), adresse = lagTomAdresse()
            )
        )

        assertEquals(expected, actual)
        JSONAssert.assertEquals(expected.toJsonSkipEmpty(), actual.toJsonSkipEmpty(), true)

    }

    @Test
    fun `create birthplace as unknown`() {
        val person = lagPerson().copy(foedsel = null)
        val result = prefillPDLNav.createFodested(person)

        assertNull(result)
    }

    @Test
    fun `create correct birthplace known`() {
        val person = lagPerson()
        val nyPerson = person.copy(
            foedsel = Foedsel(
                person.foedsel?.foedselsdato, "NOR", "OSLO", null, null, LagPdlPerson.mockMeta()
            )
        )

        val result = prefillPDLNav.createFodested(nyPerson)

        assertNotNull(result)
        assertEquals("NO", result?.land)
    }

    @Test
    fun `isPersonAvdod gir true`() {
        val dodPerson =
            lagPerson().copy(doedsfall = Doedsfall(LocalDate.of(2010, 10, 1), null, LagPdlPerson.mockMeta()))
        assertEquals(true, PrefillPDLNav.isPersonAvdod(dodPerson))
    }

    @Test
    fun `isPersonAvdod gir false`() {
        val person = lagPerson()
        assertEquals(false, PrefillPDLNav.isPersonAvdod(person))
    }

    @Test
    fun `Gitt en person med kosovo statsborgerskap Når preutfyller Statsborgerstak Så preutfyll tomt statsborgerskap`() {
        val personfnr = FodselsnummerGenerator.generateFnrForTest(40)
        val person = lagPerson(personfnr).copy(
            statsborgerskap = listOf(
                Statsborgerskap(
                    "XXK", LocalDate.of(2000, 10, 1), LocalDate.of(2300, 10, 1), LagPdlPerson.mockMeta()
                )
            )
        )

        val bruker = prefillPDLNav.createBruker(person, bank = null, ansettelsesforhold = null, null)

        println("statsborgerskap: ${bruker?.person?.statsborgerskap?.toJson()}")

        assertEquals(bruker!!.person!!.statsborgerskap!!.size, 0)
    }

    @Test
    fun `Gitt en person med noe annet enn kosovo statsborgerskap Når preutfyller Statsborgerstak Så preutfyll statsborgerskap`() {
        val personfnr = FodselsnummerGenerator.generateFnrForTest(40)
        val person = lagPerson(personfnr).copy(
            statsborgerskap = listOf(
                Statsborgerskap(
                    "NOR", LocalDate.of(2000, 10, 1), LocalDate.of(2300, 10, 1), LagPdlPerson.mockMeta()
                )
            )
        )

        val bruker = prefillPDLNav.createBruker(person, null, null, null)

        assertEquals(bruker!!.person!!.statsborgerskap!!.size, 1)
        assertEquals(bruker.person!!.statsborgerskap!![0].land, "NO")
    }

//    @Test
//    fun `Gitt en person uten fdato skal benytte fnr for fdato så SED blir preutfylt`() {
//
//        val personfnr = "01028143352"
//        val person = lagPerson(personfnr).copy(foedsel = null)
//
//        val bruker = prefillPDLNav.createBruker(person, null, null)
//
//        assertEquals("1981-02-01", bruker?.person?.foedselsdato)
//
//        val dnr = "41028143352"
//        val personDnr = lagPerson(dnr).copy(foedsel = null)
//        val brukerDnr = prefillPDLNav.createBruker(personDnr, null, null)
//
//        assertEquals("1981-02-01", brukerDnr?.person?.foedselsdato)
//    }

    companion object {
        private fun lagTomAdresse(): Adresse {
            return Adresse(
                gate = "", bygning = "", by = "", postnummer = "", land = ""
            )
        }

        fun lagNavPerson(
            foreldersPin: String,
            fornavn: String,
            etternavn: String,
            fdato: String,
            someInstitutionId: String? = null,
            someIntitutionNavn: String? = null,
            kjoenn: String? = "M",
            foedsted: String? = "NO",
            krrPerson: KrrPerson?,
            sivilstand: List<SivilstandItem>? = emptyList()
        ) = Person(
            pin = listOf(
                PinItem(
                    institusjonsnavn = someIntitutionNavn,
                    institusjonsid = someInstitutionId,
                    identifikator = foreldersPin,
                    land = "NO"
                )
            ),
            statsborgerskap = listOf(StatsborgerskapItem(land = "NO")),
            sivilstand = sivilstand,
            etternavn = etternavn,
            fornavn = fornavn,
            kjoenn = kjoenn,
            foedselsdato = fdato,
            foedested = if (foedsted == null) null else Foedested("Unknown", foedsted, region = ""),
            kontakt = if (krrPerson == null) null else createKontakt(krrPerson)
        )

        fun createKontakt(krrPerson: KrrPerson?): Kontakt? {
            krrPerson ?: return null

            val telefonList = krrPerson.mobiltelefonnummer?.let { listOf(Telefon("mobil", it)) }
            val emailList = krrPerson.epostadresse?.let { listOf(Email(it)) }

            return if (telefonList == null && emailList == null) null else Kontakt(telefonList, emailList)
        }
    }
}