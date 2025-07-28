package no.nav.eessi.pensjon.prefill.sed


import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.BucType.P_BUC_05
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.P8000
import no.nav.eessi.pensjon.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.pensjonsinformasjon.models.EPSaktype
import no.nav.eessi.pensjon.pensjonsinformasjon.models.KravArsak
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.prefill.BasePrefillNav
import no.nav.eessi.pensjon.prefill.EtterlatteService
import no.nav.eessi.pensjon.prefill.KrrService
import no.nav.eessi.pensjon.prefill.LagPdlPerson
import no.nav.eessi.pensjon.prefill.LagPdlPerson.Companion.medAdresse
import no.nav.eessi.pensjon.prefill.PersonPDLMock.medUtlandAdresse
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLAdresse
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.api.ReferanseTilPerson
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import no.nav.pensjon.v1.kravhistorikk.V1KravHistorikk
import no.nav.pensjon.v1.kravhistorikkliste.V1KravHistorikkListe
import no.nav.pensjon.v1.sak.V1Sak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert


class PrefillP8000P_BUC_05Test {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(68)
    private val pesysSaksnummer = "14398627"
    private val personService: PersonService = mockk()

    lateinit var prefillNav: PrefillPDLNav
    lateinit var prefillData: PrefillDataModel
    lateinit var etterlatteService: EtterlatteService
    lateinit var pensjonCollection: PensjonCollection
    lateinit var personDataCollection: PersonDataCollection
    lateinit var krrService: KrrService

    var kodeverkClient: KodeverkClient = mockk(relaxed = true)

    lateinit var prefillAdresse: PrefillPDLAdresse
    lateinit var prefillSEDService: PrefillSEDService

    @BeforeEach
    fun setup() {
        etterlatteService = mockk(relaxed = true)
        every { kodeverkClient.finnLandkode("NOR") } returns "NO"
        every { kodeverkClient.finnLandkode("SWE") } returns "SE"

        prefillAdresse = PrefillPDLAdresse(kodeverkClient, personService)
        prefillNav = BasePrefillNav.createPrefillNav(prefillAdresse)


        prefillSEDService = BasePrefillNav.createPrefillSEDService(prefillNav)
        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P8000, personFnr, penSaksnummer = pesysSaksnummer)

    }

    @Test
    fun `Forventer korrekt utfylt P8000 med adresse`() {
        val fnr = FodselsnummerGenerator.generateFnrForTest(68)

        val personforsikret = LagPdlPerson.lagPerson(fnr, "Christopher", "Robin")
            .medUtlandAdresse("LUNGJTEGATA 12", "postboks", "SWE", "bygning", "region", bySted = "UTLANDBY")
        personDataCollection = PersonDataCollection(personforsikret,personforsikret)

        val pensjonCollection = PensjonCollection(sedType = SedType.P8000)

        val p8000 = prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null)

        assertEquals("Christopher", p8000.nav?.bruker?.person?.fornavn)
        assertEquals("LUNGJTEGATA 12", p8000.nav?.bruker?.adresse?.gate)
        assertEquals("UTLANDBY", p8000.nav?.bruker?.adresse?.by)
        assertEquals("bygning", p8000.nav?.bruker?.adresse?.bygning)
        assertEquals("postboks", p8000.nav?.bruker?.adresse?.postnummer)
        assertEquals("region", p8000.nav?.bruker?.adresse?.region)
        assertEquals("SE", p8000.nav?.bruker?.adresse?.land)
        assertEquals(pesysSaksnummer, p8000.nav?.eessisak?.firstOrNull()?.saksnummer)
        assertEquals("Robin", p8000.nav?.bruker?.person?.etternavn)
        assertEquals(fnr, p8000.nav?.bruker?.person?.pin?.firstOrNull()?.identifikator)

    }

    @Test
    fun `Forventerkorrekt utfylt P8000 hvor det finnes en sak i Pesys som er gjenlevendepensjon eller barnepensjon - henvendelse gjelder avdøde`() {
        val fnr = FodselsnummerGenerator.generateFnrForTest(40)
        val avdodFnr = FodselsnummerGenerator.generateFnrForTest(93)

        val forsikretPerson = LagPdlPerson.lagPerson(fnr, "Christopher", "Robin")
            .medAdresse("Gate")

        val avdod = LagPdlPerson.lagPerson(avdodFnr, "Winnie", "Pooh", erDod = true)
            .medAdresse("Gate")

        personDataCollection = PersonDataCollection(avdod, forsikretPerson)
        pensjonCollection = PensjonCollection(sedType = SedType.P8000)

        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P8000, fnr, penSaksnummer = pesysSaksnummer, avdod = PersonInfo(norskIdent = avdodFnr, aktorId = "21323"),  refTilPerson = ReferanseTilPerson.AVDOD)

        val p8000 =  prefillSEDService.prefill(prefillData, personDataCollection,pensjonCollection, null) as P8000

        //daua person
        assertEquals("Winnie", p8000.nav?.bruker?.person?.fornavn)
        assertEquals("Pooh", p8000.nav?.bruker?.person?.etternavn)

        //levende person
        assertEquals("Christopher", p8000.nav?.annenperson?.person?.fornavn)
        assertEquals("Robin", p8000.nav?.annenperson?.person?.etternavn)
        assertEquals("01", p8000.nav?.annenperson?.person?.rolle)
        assertEquals("01",  p8000.p8000Pensjon?.anmodning?.referanseTilPerson)

    }

    @Test
    fun `Forventer korrekt utfylt P8000 hvor det finnes en sak i Pesys som er gjenlevendepensjon eller barnepensjon - henvendelse gjelder gjenlevende-søker`() {
        val fnr = FodselsnummerGenerator.generateFnrForTest(40)
        val avdodFnr = FodselsnummerGenerator.generateFnrForTest(93)

        val forsikretPerson = LagPdlPerson.lagPerson(fnr, "Christopher", "Robin")
            .medAdresse("Gate")

        val avdod = LagPdlPerson.lagPerson(avdodFnr, "Winnie", "Pooh", erDod = true)
            .medAdresse("Gate")

        personDataCollection = PersonDataCollection(avdod, forsikretPerson)

        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P8000, fnr, penSaksnummer = pesysSaksnummer, avdod = PersonInfo(norskIdent = avdodFnr, aktorId = "21323"), refTilPerson = ReferanseTilPerson.SOKER )
        pensjonCollection = PensjonCollection(sedType = SedType.P8000)

        val p8000 =  prefillSEDService.prefill(prefillData, personDataCollection,pensjonCollection, null) as P8000

        //daua person
        assertEquals("Winnie", p8000.nav?.bruker?.person?.fornavn)
        assertEquals("Pooh", p8000.nav?.bruker?.person?.etternavn)

        //levende person
        assertEquals("Christopher", p8000.nav?.annenperson?.person?.fornavn)
        assertEquals("Robin", p8000.nav?.annenperson?.person?.etternavn)
        assertEquals("01", p8000.nav?.annenperson?.person?.rolle)

        assertEquals("02",  p8000.p8000Pensjon?.anmodning?.referanseTilPerson)
    }


    @Test
    fun `Forventer korrekt utfylt P8000 hvor det finnes en sak i Pesys som har alderpensjon  med revurdering og henvendelsen gjelder gjenlevende`() {
        val fnr = FodselsnummerGenerator.generateFnrForTest(40)
        val avdodFnr = FodselsnummerGenerator.generateFnrForTest(93)

        val forsikretPerson = LagPdlPerson.lagPerson(fnr, "Christopher", "Robin")
            .medUtlandAdresse("LUNGJTEGATA 12", "1231", "SWE", "bygning", "region", bySted = "UTLANDBY")
        val fdato = forsikretPerson.foedselsdato?.foedselsdato

        val avdod = LagPdlPerson.lagPerson(avdodFnr, "Winnie", "Pooh", erDod = true)
            .medUtlandAdresse("LUNGJTEGATA 12", "1231", "SWE", "bygning", "region", bySted = "UTLANDBY")

//        every { krrService.hentPersonerFraKrr(eq(fnr)) } returns DigitalKontaktinfo("melleby11@melby.no",true, true, false, "11111111", fnr)

        personDataCollection = PersonDataCollection(avdod, forsikretPerson)

        val sak = V1Sak()
        val v1Kravhistorikk = V1KravHistorikk()
        v1Kravhistorikk.kravArsak = KravArsak.GJNL_SKAL_VURD.name

        sak.sakType = EPSaktype.ALDER.toString()
        sak.sakId = 100
        sak.kravHistorikkListe = V1KravHistorikkListe()
        sak.kravHistorikkListe.kravHistorikkListe.add(v1Kravhistorikk)

        val pensjonCollection = PensjonCollection(sak = sak, sedType = SedType.P8000)

        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P8000, fnr, penSaksnummer = "100", avdod = PersonInfo(norskIdent = avdodFnr, aktorId = "21323"),  refTilPerson = ReferanseTilPerson.SOKER, bucType = P_BUC_05)

        val p8000 =  prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null)

        println("resultat: ${p8000.toJsonSkipEmpty()}")
        val expected = """
            {
              "sed" : "P8000",
              "sedGVer" : "4",
              "sedVer" : "2",
              "nav" : {
                "eessisak" : [ {
                  "institusjonsid" : "NO:noinst002",
                  "institusjonsnavn" : "NOINST002, NO INST002, NO",
                  "saksnummer" : "100",
                  "land" : "NO"
                } ],
                "bruker" : {
                  "person" : {
                    "pin" : [ {
                      "institusjonsnavn" : "NOINST002, NO INST002, NO",
                      "institusjonsid" : "NO:noinst002",
                      "identifikator" : "$fnr",
                      "land" : "NO"
                    } ],
                    "etternavn" : "Robin",
                    "fornavn" : "Christopher",
                    "kjoenn" : "M",
                    "foedselsdato" : "$fdato",
                    "kontakt" : {
                      "telefon" : [ {
                        "type" : "mobil",
                        "nummer" : "11223344"
                      } ],
                      "email" : [ {
                        "adresse" : "ola@nav.no"
                      } ]
                    }
                  },
                  "adresse" : {
                    "gate" : "LUNGJTEGATA 12",
                    "by" : "UTLANDBY",
                    "region" : "region",
                    "postnummer" : "1231",
                    "bygning" : "bygning",
                    "land" : "SE"
                  }
                }
              },
            "pensjon" : {
                "anmodning" : {
                  "referanseTilPerson" : "02"
                }
              }              
             }

        """.trimIndent()


       JSONAssert.assertEquals(p8000.toJsonSkipEmpty(), expected, true)
    }

    @Test
    fun `Forventerkorrekt utfylt P8000 hvor det finnes en sak i Pesys som har uføretrygd og henvendelsen gjelder gjenlevende`() {
        val fnr = FodselsnummerGenerator.generateFnrForTest(40)
        val avdodFnr = FodselsnummerGenerator.generateFnrForTest(93)

        val forsikretPerson = LagPdlPerson.lagPerson(fnr, "Christopher", "Robin")
            .medUtlandAdresse("LUNGJTÖEGATA 12", "1231", "SWE", "bygning", "region", bySted = "UTLANDBY")

        val fdato = forsikretPerson.foedselsdato?.foedselsdato

        val avdod = LagPdlPerson.lagPerson(avdodFnr, "Winnie", "Pooh", erDod = true)
            .medUtlandAdresse("LUNGJTÖEGATA 12", "1231", "SWE", "bygning", "region", bySted = "UTLANDBY")

        personDataCollection = PersonDataCollection(avdod, forsikretPerson)

        val sak = V1Sak()
        val v1Kravhistorikk = V1KravHistorikk()
        sak.sakType = EPSaktype.UFOREP.toString()
        sak.sakId = 100
        sak.kravHistorikkListe = V1KravHistorikkListe()
        sak.kravHistorikkListe.kravHistorikkListe.add(v1Kravhistorikk)

        val pensjonCollection = PensjonCollection(sak = sak, sedType = SedType.P8000)

        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P8000, fnr, penSaksnummer = "100", avdod = PersonInfo(norskIdent = avdodFnr, aktorId = "21323"),  refTilPerson = ReferanseTilPerson.SOKER, bucType = P_BUC_05)

        val p8000 =  prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null)
        println(p8000.toJsonSkipEmpty())
        val expected = """
            {
              "sed" : "P8000",
              "sedGVer" : "4",
              "sedVer" : "2",
              "nav" : {
                "eessisak" : [ {
                  "institusjonsid" : "NO:noinst002",
                  "institusjonsnavn" : "NOINST002, NO INST002, NO",
                  "saksnummer" : "100",
                  "land" : "NO"
                } ],
                "bruker" : {
                  "person" : {
                    "pin" : [ {
                      "institusjonsnavn" : "NOINST002, NO INST002, NO",
                      "institusjonsid" : "NO:noinst002",
                      "identifikator" : "$fnr",
                      "land" : "NO"
                    } ],
                    "etternavn" : "Robin",
                    "fornavn" : "Christopher",
                    "kjoenn" : "M",
                    "foedselsdato" : "$fdato",                    
                    "kontakt" : {
                      "telefon" : [ {
                        "type" : "mobil",
                        "nummer" : "11223344"
                      } ],
                      "email" : [ {
                        "adresse" : "ola@nav.no"
                      } ]
                    }
                  },
                  "adresse" : {
                    "gate" : "LUNGJTÖEGATA 12",
                    "by" : "UTLANDBY",
                    "bygning" : "bygning",
                    "postnummer" : "1231",
                    "region" : "region",
                    "land" : "SE"
                  }
                }
              },
            "pensjon" : {
                "anmodning" : {
                  "referanseTilPerson" : "02"
                }
              }              
            }

        """.trimIndent()

        JSONAssert.assertEquals(expected, p8000.toJsonSkipEmpty(), true)
    }

}

