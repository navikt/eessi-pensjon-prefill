package no.nav.eessi.pensjon.prefill.sed


import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.P8000
import no.nav.eessi.pensjon.pensjonsinformasjon.EPSaktype
import no.nav.eessi.pensjon.pensjonsinformasjon.KravArsak
import no.nav.eessi.pensjon.personoppslag.FodselsnummerGenerator
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.prefill.LagPDLPerson
import no.nav.eessi.pensjon.prefill.LagPDLPerson.Companion.medAdresse
import no.nav.eessi.pensjon.prefill.PersonPDLMock.medUtlandAdresse
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PersonId
import no.nav.eessi.pensjon.prefill.models.PrefillDataModel
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.models.ReferanseTilPerson
import no.nav.eessi.pensjon.prefill.person.PrefillPDLAdresse
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.services.geo.PostnummerService
import no.nav.eessi.pensjon.services.kodeverk.KodeverkClient
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

    lateinit var prefillData: PrefillDataModel
    lateinit var prefillNav: PrefillPDLNav
    lateinit var personDataCollection: PersonDataCollection
    lateinit var pensjonCollection: PensjonCollection

    var kodeverkClient: KodeverkClient = mockk()

    lateinit var prefillAdresse: PrefillPDLAdresse
    lateinit var prefillSEDService: PrefillSEDService

    @BeforeEach
    fun setup() {
        every { kodeverkClient.finnLandkode("NOR") } returns "NO"
        every { kodeverkClient.finnLandkode("SWE") } returns "SE"

        prefillAdresse = PrefillPDLAdresse(PostnummerService(), kodeverkClient, personService)
        prefillNav = PrefillPDLNav( prefillAdresse,
                institutionid = "NO:noinst002",
                institutionnavn = "NOINST002, NO INST002, NO")


        prefillSEDService = PrefillSEDService(EessiInformasjon(), prefillNav)
        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P8000, personFnr, penSaksnummer = pesysSaksnummer)

    }

    @Test
    fun `Forventer korrekt utfylt P8000 med adresse`() {
        val fnr = FodselsnummerGenerator.generateFnrForTest(68)

        val personforsikret = LagPDLPerson.lagPerson(fnr, "Christopher", "Robin")
            .medUtlandAdresse("LUNGJTEGATA 12", "1231" , "SWE")
        personDataCollection = PersonDataCollection(personforsikret,personforsikret)

        val pensjonCollection = PensjonCollection(sedType = SedType.P8000)

        val p8000 = prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection)

        assertEquals("Christopher", p8000.nav?.bruker?.person?.fornavn)
        assertEquals("LUNGJTEGATA 12", p8000.nav?.bruker?.adresse?.gate)
        assertEquals("SE", p8000.nav?.bruker?.adresse?.land)
        assertEquals(pesysSaksnummer, p8000.nav?.eessisak?.firstOrNull()?.saksnummer)
        assertEquals("Robin", p8000.nav?.bruker?.person?.etternavn)
        assertEquals(fnr, p8000.nav?.bruker?.person?.pin?.firstOrNull()?.identifikator)

    }

    @Test
    fun `Forventerkorrekt utfylt P8000 hvor det finnes en sak i Pesys som er gjenlevendepensjon eller barnepensjon - henvendelse gjelder avdøde`() {
        val fnr = FodselsnummerGenerator.generateFnrForTest(40)
        val avdodFnr = FodselsnummerGenerator.generateFnrForTest(93)

        val forsikretPerson = LagPDLPerson.lagPerson(fnr, "Christopher", "Robin")
            .medAdresse("Gate")

        val avdod = LagPDLPerson.lagPerson(avdodFnr, "Winnie", "Pooh", erDod = true)
            .medAdresse("Gate")

        personDataCollection = PersonDataCollection(avdod, forsikretPerson)
        pensjonCollection = PensjonCollection(sedType = SedType.P8000)

        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P8000, fnr, penSaksnummer = pesysSaksnummer, avdod = PersonId(norskIdent = avdodFnr, aktorId = "21323"),  refTilPerson = ReferanseTilPerson.AVDOD)

        val p8000 =  prefillSEDService.prefill(prefillData, personDataCollection,pensjonCollection) as P8000

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

        val forsikretPerson = LagPDLPerson.lagPerson(fnr, "Christopher", "Robin")
            .medAdresse("Gate")

        val avdod = LagPDLPerson.lagPerson(avdodFnr, "Winnie", "Pooh", erDod = true)
            .medAdresse("Gate")

        personDataCollection = PersonDataCollection(avdod, forsikretPerson)

        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P8000, fnr, penSaksnummer = pesysSaksnummer, avdod = PersonId(norskIdent = avdodFnr, aktorId = "21323"), refTilPerson = ReferanseTilPerson.SOKER )
        pensjonCollection = PensjonCollection(sedType = SedType.P8000)

        val p8000 =  prefillSEDService.prefill(prefillData, personDataCollection,pensjonCollection) as P8000

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

        val forsikretPerson = LagPDLPerson.lagPerson(fnr, "Christopher", "Robin")
            .medUtlandAdresse("LUNGJTEGATA 12", "1231" , "SWE")
        val fdato = forsikretPerson.foedsel?.foedselsdato

        val avdod = LagPDLPerson.lagPerson(avdodFnr, "Winnie", "Pooh", erDod = true)
            .medUtlandAdresse("LUNGJTEGATA 12", "1231" , "SWE")

        personDataCollection = PersonDataCollection(avdod, forsikretPerson)

        val sak = V1Sak()
        val v1Kravhistorikk = V1KravHistorikk()
        v1Kravhistorikk.kravArsak = KravArsak.GJNL_SKAL_VURD.name

        sak.sakType = EPSaktype.ALDER.toString()
        sak.sakId = 100
        sak.kravHistorikkListe = V1KravHistorikkListe()
        sak.kravHistorikkListe.kravHistorikkListe.add(v1Kravhistorikk)

        val pensjonCollection = PensjonCollection(sak = sak, sedType = SedType.P8000)

        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P8000, fnr, penSaksnummer = "100", avdod = PersonId(norskIdent = avdodFnr, aktorId = "21323"),  refTilPerson = ReferanseTilPerson.SOKER, bucType = "P_BUC_05")

        val p8000 =  prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection)

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
                      "identifikator" : "$fnr",
                      "land" : "NO"
                    } ],
                    "etternavn" : "Robin",
                    "fornavn" : "Christopher",
                    "kjoenn" : "M",
                    "foedselsdato" : "$fdato"
                  },
                  "adresse" : {
                    "gate" : "LUNGJTEGATA 12",
                    "by" : "UTLANDBY",
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

        val forsikretPerson = LagPDLPerson.lagPerson(fnr, "Christopher", "Robin")
            .medUtlandAdresse("LUNGJTÖEGATA 12", "1231" , "SWE")

        val fdato = forsikretPerson.foedsel?.foedselsdato

        val avdod = LagPDLPerson.lagPerson(avdodFnr, "Winnie", "Pooh", erDod = true)
            .medUtlandAdresse("LUNGJTÖEGATA 12", "1231" , "SWE")

        personDataCollection = PersonDataCollection(avdod, forsikretPerson)

        val sak = V1Sak()
        val v1Kravhistorikk = V1KravHistorikk()
        sak.sakType = EPSaktype.UFOREP.toString()
        sak.sakId = 100
        sak.kravHistorikkListe = V1KravHistorikkListe()
        sak.kravHistorikkListe.kravHistorikkListe.add(v1Kravhistorikk)

        val pensjonCollection = PensjonCollection(sak = sak, sedType = SedType.P8000)

        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P8000, fnr, penSaksnummer = "100", avdod = PersonId(norskIdent = avdodFnr, aktorId = "21323"),  refTilPerson = ReferanseTilPerson.SOKER, bucType = "P_BUC_05")

        val p8000 =  prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection)

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
                      "identifikator" : "$fnr",
                      "land" : "NO"
                    } ],
                    "etternavn" : "Robin",
                    "fornavn" : "Christopher",
                    "kjoenn" : "M",
                    "foedselsdato" : "$fdato"                    
                  },
                  "adresse" : {
                    "gate" : "LUNGJTÖEGATA 12",
                    "by" : "UTLANDBY",
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

}

