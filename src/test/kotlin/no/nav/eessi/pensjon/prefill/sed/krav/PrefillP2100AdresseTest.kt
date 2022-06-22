package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.Nav
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.personoppslag.FodselsnummerGenerator
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PensjonsinformasjonService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PersonId
import no.nav.eessi.pensjon.prefill.models.PrefillDataModel
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLAdresse
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper.lesPensjonsdataFraFil
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper.readJsonResponse
import no.nav.eessi.pensjon.utils.toJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PrefillP2100AdresseTest {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(65)
    private val avdodPersonFnr = FodselsnummerGenerator.generateFnrForTest(75)
    private val pesysSaksnummer = "22875355"

    private lateinit var prefillData: PrefillDataModel
    private lateinit var prefillPDLAdresse: PrefillPDLAdresse
    private lateinit var dataFromPEN: PensjonsinformasjonService
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var persondataCollection: PersonDataCollection
    private lateinit var pensjonCollection: PensjonCollection

    @BeforeEach
    fun setup() {
        persondataCollection = PersonPDLMock.createAvdodFamilie(personFnr, avdodPersonFnr)

        val avdod = persondataCollection.gjenlevendeEllerAvdod
        val forsikret = persondataCollection.forsikretPerson

        val avdodAdresse = PrefillPDLAdresse(mockk(relaxed = true), mockk(relaxed = true)).createPersonAdresse(avdod!!)
        val gjenlevAdresse = PrefillPDLAdresse(mockk(relaxed = true), mockk(relaxed = true)).createPersonAdresse(forsikret!!)

        val prefillNav = PrefillPDLNav(
                prefillAdresse = mockk(){
                    every { hentLandkode(any()) } returns "NO"
                    every { createPersonAdresse(eq(avdod)) } returns avdodAdresse
                    every { createPersonAdresse(eq(forsikret)) } returns gjenlevAdresse
                },
                institutionid = "NO:noinst002",
                institutionnavn = "NOINST002, NO INST002, NO")

        dataFromPEN = lesPensjonsdataFraFil("/pensjonsinformasjon/krav/P2100-GL-UTL-INNV.xml")

        prefillData = PrefillDataModelMother.initialPrefillDataModel(
                sedType = SedType.P2100,
                pinId = personFnr,
                penSaksnummer = pesysSaksnummer,
                avdod = PersonId(avdodPersonFnr, "112233445566")).apply {
            partSedAsJson["PersonInfo"] = readJsonResponse("/json/nav/other/person_informasjon_selvb.json")
            partSedAsJson["P4000"] = readJsonResponse("/json/nav/other/p4000_trygdetid_part.json")

        }
        val innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = dataFromPEN)
        pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)

        prefillSEDService = PrefillSEDService(EessiInformasjon(), prefillNav)
        prefillPDLAdresse = PrefillPDLAdresse(mockk(relaxed = true), mockk(relaxed = true))

    }

    @Test
    fun `Hvis en P2100 preutfylles saa skal adressen til avdode fylles ut i adressefelt kapittel 2`() {

        val p2100 = prefillSEDService.prefill(prefillData, persondataCollection, pensjonCollection)

        val p2100gjenlev = SED(
            type = SedType.P2100,
            pensjon = p2100.pensjon,
            nav = Nav(bruker = p2100.nav?.bruker)
        )


        println("***".repeat(20))
        println(p2100gjenlev)
        println("***".repeat(20))

        val expectedAvdodAdresse = """
            {
              "gate" : "Avdødadresse 2222",
              "bygning" : null,
              "by" : "",
              "postnummer" : "1111",
              "postkode" : null,
              "region" : null,
              "land" : "NO",
              "kontaktpersonadresse" : null,
              "datoforadresseendring" : null,
              "postadresse" : null,
              "startdato" : null,
              "type" : null,
              "annen" : null
            }
        """.trimIndent()

        val expectedGjenlevAdresse = """
            {
              "gate" : "FORUSBEEN 2294",
              "bygning" : null,
              "by" : "",
              "postnummer" : "0010",
              "postkode" : null,
              "region" : null,
              "land" : "NO",
              "kontaktpersonadresse" : null,
              "datoforadresseendring" : null,
              "postadresse" : null,
              "startdato" : null,
              "type" : null,
              "annen" : null
            }
        """.trimIndent()

        assertEquals(expectedAvdodAdresse, p2100gjenlev.nav?.bruker?.adresse?.toJson())
        assertEquals(expectedGjenlevAdresse, p2100gjenlev.pensjon?.gjenlevende?.adresse?.toJson())

    }

}

