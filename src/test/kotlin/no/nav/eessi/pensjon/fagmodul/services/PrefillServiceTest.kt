package no.nav.eessi.pensjon.fagmodul.services

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.pensjon.fagmodul.models.*
import no.nav.eessi.pensjon.fagmodul.prefill.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.PrefillSED
import no.nav.eessi.pensjon.fagmodul.services.eux.BucSedResponse
import no.nav.eessi.pensjon.fagmodul.services.eux.BucUtils
import no.nav.eessi.pensjon.fagmodul.services.eux.EuxService
import no.nav.eessi.pensjon.fagmodul.services.eux.SedDokumentIkkeOpprettetException
import no.nav.eessi.pensjon.fagmodul.services.eux.bucmodel.Buc
import no.nav.eessi.pensjon.fagmodul.services.eux.bucmodel.ShortDocumentItem
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.typeRefs
import no.nav.eessi.pensjon.utils.validateJson
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(MockitoJUnitRunner::class)
class PrefillServiceTest {

    @Mock
    lateinit var mockPrefillSED: PrefillSED

    private lateinit var prefillService: PrefillService

    @Before
    fun `startup initilize testing`() {
        prefillService = PrefillService(mockPrefillSED)

    }

    @Test
    fun `call prefillEnX005ForHverInstitusjon| mock adding institusjon `() {
        val euxCaseId = "12131234"

        val data = generatePrefillModel()
        data.euxCaseID = euxCaseId
        data.sed = generateMockP2000(data)

        val mockInstitusjonList = listOf(
                InstitusjonItem(country = "FI", institution = "Finland", name="Finland test"),
                InstitusjonItem(country = "DE", institution = "Tyskland", name="Tyskland test")
        )

        whenever(mockPrefillSED.prefill(any())).thenReturn(data)

        val x005Liste = prefillService.prefillEnX005ForHverInstitusjon(mockInstitusjonList, data)

        assertEquals(x005Liste.size, 2)
    }

    @Test(expected = SedValidatorException::class)
    fun `call prefillAndPreview| Exception ved validating SED`() {
        val dataModel = generatePrefillModel()

        val resultData = generatePrefillModel()
        resultData.sed = generateMockP2000ForValidatorError(dataModel)

        whenever(mockPrefillSED.prefill(any())).thenReturn(resultData)
        prefillService.prefillSed(dataModel)
    }

    @Test
    fun `call prefillAndPreview| Exception ved validating SED etternavn`() {
        val dataModel = generatePrefillModel()

        val resultData = generatePrefillModel()
        resultData.sed = generateMockP2000ForValidatorError(dataModel)

        whenever(mockPrefillSED.prefill(any())).thenReturn(resultData)

        try {
            prefillService.prefillSed(resultData)
            fail("skal ikke komme hit!")
        } catch (sedv: SedValidatorException) {
            assertEquals("Etternavn mangler", sedv.message)
        }
    }

    @Test
    fun `call prefillAndPreview| Exception ved validating SED fornavn`() {
        val dataModel = generatePrefillModel()

        val resultData = generatePrefillModel()
        resultData.sed = generateMockP2000ForValidatorError(dataModel)
        resultData.sed.nav?.bruker = Bruker(person = Person(etternavn = "BAMSELUR"))

        whenever(mockPrefillSED.prefill(any())).thenReturn(resultData)

        try {
            prefillService.prefillSed(resultData)
            fail("skal ikke komme hit!")
        } catch (sedv: SedValidatorException) {
            assertEquals("Fornavn mangler", sedv.message)
        }
    }

    @Test
    fun `call prefillAndPreview| Exception ved validating SED fdato`() {
        val dataModel = generatePrefillModel()

        val resultData = generatePrefillModel()
        resultData.sed = generateMockP2000ForValidatorError(dataModel)
        resultData.sed.nav?.bruker = Bruker(person = Person(etternavn = "BAMSELUR", fornavn = "DUMMY"))

        whenever(mockPrefillSED.prefill(any())).thenReturn(resultData)

        try {
            prefillService.prefillSed(resultData)
            fail("skal ikke komme hit!")
        } catch (sedv: SedValidatorException) {
            assertEquals("Fødseldsdato mangler", sedv.message)
        }
    }

    @Test
    fun `call prefillAndPreview| Exception ved validating SED kjonn`() {
        val dataModel = generatePrefillModel()

        val resultData = generatePrefillModel()
        resultData.sed = generateMockP2000ForValidatorError(dataModel)
        resultData.sed.nav?.bruker = Bruker(person = Person(etternavn = "BAMSELUR", fornavn = "DUMMY", kjoenn = "M"))

        whenever(mockPrefillSED.prefill(any())).thenReturn(resultData)

        try {
            prefillService.prefillSed(resultData)
            fail("skal ikke komme hit!")
        } catch (sedv: SedValidatorException) {
            assertEquals("Fødseldsdato mangler", sedv.message)
        }
    }

    @Test
    fun `call prefillAndPreview| Exception ved validating SED kravDato`() {
        val dataModel = generatePrefillModel()

        val resultData = generatePrefillModel()
        resultData.sed = generateMockP2000ForValidatorError(dataModel)
        resultData.sed.nav?.bruker = Bruker(person = Person(etternavn = "BAMSELUR", fornavn = "DUMMY", kjoenn = "M", foedselsdato = "1955-05-05"))

        whenever(mockPrefillSED.prefill(any())).thenReturn(resultData)

        try {
            prefillService.prefillSed(resultData)
            fail("skal ikke komme hit!")
        } catch (sedv: SedValidatorException) {
            assertEquals("Kravdato mangler", sedv.message)
        }
    }


    fun generateMockP2000(prefillModel: PrefillDataModel): SED {
        val mocksed = prefillModel.sed
        val mockp2000 = SedMock().genererP2000Mock()
        mocksed.nav = mockp2000.nav
        mocksed.nav?.krav = Krav("1960-06-12")
        mocksed.pensjon = mockp2000.pensjon
        return mocksed
    }

    fun generateMockX005(prefillModel: PrefillDataModel): SED {
        val mockP2000 = generateMockP2000(prefillModel)
        val person = mockP2000.nav?.bruker?.person

        val x005Datamodel = PrefillDataModel.fromJson(prefillModel.clone())
        val x005 = SED.create("X005")
        x005Datamodel.sed = x005
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
                                institusjon = InstitusjonX005(
                                        id = "",
                                        navn = ""
                                ),
                                grunn = null
                        )
                )
        )
        x005Datamodel.sed = x005
        return x005Datamodel.sed
    }

    fun generateMockP2000ForValidatorError(prefillModel: PrefillDataModel): SED {
        val mocksed = prefillModel.sed
        mocksed.nav = Nav()
        mocksed.pensjon = Pensjon()
        return mocksed
    }

    fun generatePrefillModel(): PrefillDataModel {
        return PrefillDataModel().apply {
            euxCaseID = "1000"
            sed = SED.create("P2000")
            buc  = "P_BUC_01"
            institution = listOf(
                    InstitusjonItem(
                            country = "NO",
                            institution = "DUMMY"
                    )
            )
            penSaksnummer = "123456789999"
            personNr = "12345678901"
        }

    }

    fun generatePrefillModel(bucType: String, caseID: String, navSed: SED): PrefillDataModel {
        return PrefillDataModel().apply {
            euxCaseID = caseID
            sed = navSed
            buc  = bucType
            institution = listOf(
                    InstitusjonItem(
                            country = "NO",
                            institution = "DUMMY"
                    )
            )
            penSaksnummer = "123456789999"
            personNr = "12345678901"
        }

    }


}