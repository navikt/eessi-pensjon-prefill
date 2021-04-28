package no.nav.eessi.pensjon.fagmodul.prefill
import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedMock
import no.nav.eessi.pensjon.eux.model.sed.Bruker
import no.nav.eessi.pensjon.eux.model.sed.InstitusjonX005
import no.nav.eessi.pensjon.eux.model.sed.Kontekst
import no.nav.eessi.pensjon.eux.model.sed.Leggtilinstitusjon
import no.nav.eessi.pensjon.eux.model.sed.Nav
import no.nav.eessi.pensjon.eux.model.sed.Navsak
import no.nav.eessi.pensjon.eux.model.sed.Person
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.eux.model.sed.SedType
import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.models.PersonDataCollection
import no.nav.eessi.pensjon.fagmodul.models.PersonId
import no.nav.eessi.pensjon.fagmodul.models.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

class PrefillServiceTest {

/*    @Mock*/
    var mockPrefillSEDService: PrefillSEDService = mockk()

    private lateinit var prefillService: PrefillService
    private lateinit var personcollection: PersonDataCollection


    @BeforeEach
    fun `startup initilize testing`() {
        prefillService = PrefillService(mockPrefillSEDService)
        personcollection = PersonDataCollection(null, null)
    }

    @Test
    fun `call prefillEnX005ForHverInstitusjon sjekk paa antall`() {
        val data = generatePrefillModel()

        val mockInstitusjonList = listOf(
                InstitusjonItem(country = "FI", institution = "Finland", name="Finland test"),
                InstitusjonItem(country = "DE", institution = "Tyskland", name="Tyskland test")
        )
        val x005sed = generateMockX005(data)
        every { mockPrefillSEDService.prefill(any(), any()) } returns x005sed
        val x005Liste = prefillService.prefillEnX005ForHverInstitusjon(mockInstitusjonList, data, personcollection)
        assertEquals(x005Liste.size, 2)
    }

    @Test
    fun `call prefillEnX005ForHverInstitusjon mock adding institusjon `() {
        val data = generatePrefillModel()

        val de = InstitusjonItem(country = "DE", institution = "Tyskland", name="Tyskland test")

        val SedType = SedType.X005

        val datax005 = data.copy(avdod = null, sedType = SedType, institution = listOf(de))
        val x005sed = generateMockX005(datax005)

        every{mockPrefillSEDService.prefill(any(), any())} returns x005sed

        val mockInstitusjonList = listOf(de)
        val x005Liste = prefillService.prefillEnX005ForHverInstitusjon(mockInstitusjonList, data, personcollection)

        assertEquals(x005Liste.size, 1)
        val result = x005Liste[0]

        val valid = """
            {
              "sed" : "X005",
              "sedGVer" : "4",
              "sedVer" : "1",
              "nav" : {
                "sak" : {
                  "kontekst" : {
                    "bruker" : {
                      "person" : {
                        "etternavn" : "Konsoll",
                        "fornavn" : "Gul",
                        "foedselsdato" : "1967-12-01"
                      }
                    }
                  },
                  "leggtilinstitusjon" : {
                    "institusjon" : {
                      "id" : "DE:Tyskland",
                      "navn" : "Tyskland test"
                    }
                  }
                }
              }
            }
        """.trimIndent()

        JSONAssert.assertEquals(result.toJsonSkipEmpty(), valid, true)

    }


    fun generateMockP2000(prefillModel: PrefillDataModel): SED {
        val mocksed = SED(type = prefillModel.sedType)
        val mockp2000 = SedMock().genererP2000Mock()
        mocksed.nav = mockp2000.nav
        mocksed.pensjon = mockp2000.pensjon
        return mocksed
    }

    fun generateMockX005(prefillModel: PrefillDataModel): SED {
        val mockP2000 = generateMockP2000(prefillModel)
        val person = mockP2000.nav?.bruker?.person

        val singleSelectedInstitustion = prefillModel.institution.first()
        val institusjonX005 = InstitusjonX005(
            id = singleSelectedInstitustion.checkAndConvertInstituion(),
            navn = singleSelectedInstitustion.name ?: singleSelectedInstitustion.checkAndConvertInstituion()
        )

        //val x005Datamodel = PrefillDataModel.fromJson(prefillModel.clone())
        val x005 = SED(SedType.X005)
        x005.nav = Nav(
                sak = Navsak(
                        kontekst = Kontekst(
                                bruker = Bruker(
                                        person = Person(
                                                fornavn = person?.fornavn,
                                                etternavn = person?.etternavn,
                                                foedselsdato = person?.foedselsdato
                                        )
                                )
                        ),
                        leggtilinstitusjon = Leggtilinstitusjon(
                                institusjon = institusjonX005
                        )
                )
        )
        return x005
    }

    fun generatePrefillModel(): PrefillDataModel {
        return PrefillDataModel(
                penSaksnummer = "123456789999",
                bruker = PersonId("12345678901", "dummy"),
                avdod = null,
                euxCaseID = "1000",
                sedType = SedType.P2000,
                buc  = "P_BUC_01",
                institution = listOf(
                InstitusjonItem(
                        country = "NO",
                        institution = "DUMMY"))
                )
    }
}
