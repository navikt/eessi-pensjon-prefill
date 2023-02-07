package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.personoppslag.FodselsnummerGenerator
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PensjonsinformasjonService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.prefill.person.PrefillPDLAdresse
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper.lesPensjonsdataFraFil
import no.nav.eessi.pensjon.shared.api.PersonId
import no.nav.eessi.pensjon.utils.toJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PrefillP2100AdresseTest {

    private val personService: PersonService = mockk()
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
        prefillPDLAdresse = PrefillPDLAdresse(mockk(relaxed = true), mockk(relaxed = true){
            every { finnLandkode(eq("NOR")) } returns "NO"
        }, personService)
        prefillPDLAdresse.initMetrics()

        val prefillNav = PrefillPDLNav(prefillPDLAdresse,
                institutionid = "NO:noinst002",
                institutionnavn = "NOINST002, NO INST002, NO")

        persondataCollection = PersonPDLMock.createAvdodFamilieMedDødsboadresse(personFnr, avdodPersonFnr)
        prefillData = PrefillDataModelMother.initialPrefillDataModel(
                sedType = SedType.P2100,
                pinId = personFnr,
                penSaksnummer = pesysSaksnummer,
                avdod = PersonId(avdodPersonFnr, "112233445566")
        )

        dataFromPEN = lesPensjonsdataFraFil("/pensjonsinformasjon/krav/P2100-GL-UTL-INNV.xml")
        val innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = dataFromPEN)
        pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)

        prefillSEDService = PrefillSEDService(EessiInformasjon(), prefillNav)

    }

    @Test
    fun `Hvis en P2100 preutfylles saa skal adressen til avdode fylles ut i adressefelt kapittel 2`() {

        val p2100 = prefillSEDService.prefill(prefillData, persondataCollection, pensjonCollection)

        val expectedAvdodAdresse = """
            {
              "gate" : "Dødsbo v/Michelle Etternavn, Avdødadresse",
              "bygning" : "adresse 2",
              "by" : "2222",
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

        assertEquals(expectedAvdodAdresse, p2100.nav?.bruker?.adresse?.toJson())
        assertEquals(expectedGjenlevAdresse, p2100.pensjon?.gjenlevende?.adresse?.toJson())

    }

}

