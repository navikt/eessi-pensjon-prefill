package no.nav.eessi.pensjon.prefill

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.PersonoppslagException
import no.nav.eessi.pensjon.personoppslag.pdl.model.*
import no.nav.eessi.pensjon.prefill.LagPDLPerson.Companion.lagPerson
import no.nav.eessi.pensjon.prefill.LagPDLPerson.Companion.medAdresse
import no.nav.eessi.pensjon.prefill.LagPDLPerson.Companion.medBarn
import no.nav.eessi.pensjon.prefill.LagPDLPerson.Companion.medForeldre
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.shared.api.PersonId
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException

private const val NPID_VOKSEN = "01220049651"

internal class PersonDataServiceTest {

    companion object {
        const val SAK_ID = "12345"
        const val EUX_RINA = "23123"

        const val FNR_VOKSEN = "11067122781"    // KRAFTIG VEGGPRYD
        const val FNR_VOKSEN_2 = "22117320034"  // LEALAUS KAKE
        const val FNR_BARN = "12011577847"      // STERK BUSK

        const val AKTOER_ID_2 = "0009876543210"
    }

    private val personService: PersonService = mockk(relaxed = false)

    private val persondataService: PersonDataService = PersonDataService(
        personService = personService
    )

    @AfterEach
    fun after() {
        clearAllMocks()
    }

    @Test
    fun `test henting av forsikretperson som feiler`() {

        every { personService.hentPerson(any()) } throws PersonoppslagException("Fant ikke person", "not_found")

        val data = PrefillDataModelMother.initialPrefillDataModel(SedType.P2000, FNR_VOKSEN, SAK_ID, euxCaseId = EUX_RINA)

        assertThrows<ResponseStatusException> {
            persondataService.hentPersonData(data)
        }

        verify ( exactly = 1 ) { personService.hentPerson(any())  }

    }

    @Test
    fun `test henting av forsikretperson for persondatacollection`() {
        val mockPerson = lagPerson(FNR_VOKSEN)

        every { personService.hentPerson(any()) } returns mockPerson

        val data = PrefillDataModelMother.initialPrefillDataModel(SedType.P2000, FNR_VOKSEN, SAK_ID, euxCaseId = EUX_RINA)

        val result = persondataService.hentPersonData(data)

        assertNull(result.ektefellePerson)
        assertNull(result.sivilstandstype)
        assertEquals(emptyList<Person>(), result.barnPersonList)
        assertEquals(mockPerson, result.gjenlevendeEllerAvdod)
        assertEquals(mockPerson, result.forsikretPerson)

        verify ( exactly = 1 ) { personService.hentPerson(any())  }

    }

    @Test
    fun `test henting av forsikretperson og avdødperson for persondatacollection`() {
        val gjenlev = lagPerson(FNR_VOKSEN)
        val avdod = lagPerson(FNR_VOKSEN_2, erDod = true)

        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns gjenlev
        every { personService.hentPerson(NorskIdent(FNR_VOKSEN_2)) } returns avdod

        val data = PrefillDataModelMother.initialPrefillDataModel(SedType.P2000, FNR_VOKSEN, SAK_ID, euxCaseId = EUX_RINA, avdod = PersonId(
            FNR_VOKSEN_2, AKTOER_ID_2
        )
        )

        val result = persondataService.hentPersonData(data)

        assertNull(result.ektefellePerson)
        assertNull(result.sivilstandstype)
        assertEquals(emptyList<Person>(), result.barnPersonList)
        assertEquals(avdod, result.gjenlevendeEllerAvdod)
        assertEquals(gjenlev, result.forsikretPerson)

        verify ( exactly = 2 ) { personService.hentPerson(any())  }
    }

    @Test
    fun `test henting av forsikretperson og barn for persondatacollection`() {
        val forelder = lagPerson(FNR_VOKSEN, "Christopher", "Robin").medBarn(FNR_BARN)
        val barn = lagPerson(FNR_BARN, "Ole", "Brum").medForeldre(forelder)

        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns forelder
        every { personService.hentPerson(NorskIdent(FNR_BARN)) } returns barn

        val data = PrefillDataModelMother.initialPrefillDataModel(SedType.P2000, FNR_VOKSEN, SAK_ID, euxCaseId = EUX_RINA)

        val result = persondataService.hentPersonData(data)

        assertNull(result.ektefellePerson)
        assertNull(result.sivilstandstype)
        assertEquals(barn, result.barnPersonList.firstOrNull())
        assertEquals(forelder, result.gjenlevendeEllerAvdod)
        assertEquals(forelder, result.forsikretPerson)

        verify ( exactly = 2 ) { personService.hentPerson(any())  }

    }

    @Test
    fun `test henting av forsikretperson med barn under 18 aar for persondatacollection`() {

        val barn1fnr = FodselsnummerGenerator.generateFnrForTest(12)
        val barn2fnr = FodselsnummerGenerator.generateFnrForTest(19)

        val forelder = lagPerson(FNR_VOKSEN, "Christopher", "Robin").medBarn(barn1fnr).medBarn(barn2fnr)
        val barn1 = lagPerson(barn2fnr, "Ole", "Brum").medForeldre(forelder)
        val barn2 = lagPerson(barn1fnr, "Nasse", "Nøff").medForeldre(forelder)

        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns forelder
        every { personService.hentPerson(NorskIdent(barn1fnr)) } returns barn1
        every { personService.hentPerson(NorskIdent(barn2fnr)) } returns barn2

        val data = PrefillDataModelMother.initialPrefillDataModel(SedType.P2000, FNR_VOKSEN, SAK_ID, euxCaseId = EUX_RINA)

        val result = persondataService.hentPersonData(data)

        assertNull(result.ektefellePerson)
        assertNull(result.sivilstandstype)
        assertEquals(barn1, result.barnPersonList.firstOrNull())
        assertEquals(1, result.barnPersonList.size)
        assertEquals(forelder, result.gjenlevendeEllerAvdod)
        assertEquals(forelder, result.forsikretPerson)

        verify ( exactly = 2 ) { personService.hentPerson(any())  }

    }

    @Test
    fun `Innhenting av forsikret person med barn under 18 aar returnerer persondatacollection`() {
        val barn1fnr = FodselsnummerGenerator.generateFnrForTest(12)
        val barn2fnr = FodselsnummerGenerator.generateFnrForTest(19)

        val forelder = lagPerson(NPID_VOKSEN, "Christopher", "Robin").medBarn(barn1fnr).medBarn(barn2fnr)
        val barn1 = lagPerson(barn2fnr, "Ole", "Brum").medForeldre(forelder)
        val barn2 = lagPerson(barn1fnr, "Nasse", "Nøff").medForeldre(forelder)

        every { personService.hentPerson(Npid(NPID_VOKSEN)) } returns forelder
        every { personService.hentPerson(NorskIdent(barn1fnr)) } returns barn1
        every { personService.hentPerson(NorskIdent(barn2fnr)) } returns barn2

        val data = PrefillDataModelMother.initialPrefillDataModel(SedType.P2000, NPID_VOKSEN, SAK_ID, euxCaseId = EUX_RINA)

        val result = persondataService.hentPersonData(data)

        assertNull(result.ektefellePerson)
        assertNull(result.sivilstandstype)
        assertEquals(barn1, result.barnPersonList.firstOrNull())
        assertEquals(1, result.barnPersonList.size)
        assertEquals(forelder, result.gjenlevendeEllerAvdod)
        assertEquals(forelder, result.forsikretPerson)

        verify ( exactly = 2 ) { personService.hentPerson(any())  }

    }
    @Test
    fun `test henting av forsikretperson med avdod ektefelle for persondatacollection`() {

        val barn1fnr = FodselsnummerGenerator.generateFnrForTest(12)
        val barn2fnr = FodselsnummerGenerator.generateFnrForTest(19)

        val forelder = lagPerson(FNR_VOKSEN, "Christopher", "Robin").medBarn(barn1fnr).medBarn(barn2fnr)
        val barn1 = lagPerson(barn2fnr, "Ole", "Brum").medForeldre(forelder)
        val barn2 = lagPerson(barn1fnr, "Nasse", "Nøff").medForeldre(forelder)

        every { personService.hentPerson(NorskIdent(FNR_VOKSEN)) } returns forelder
        every { personService.hentPerson(NorskIdent(barn1fnr)) } returns barn1
        every { personService.hentPerson(NorskIdent(barn2fnr)) } returns barn2

        val data = PrefillDataModelMother.initialPrefillDataModel(SedType.P2000, FNR_VOKSEN, SAK_ID, euxCaseId = EUX_RINA)

        val result = persondataService.hentPersonData(data)

        assertNull( result.ektefellePerson)
        assertNull( result.sivilstandstype)
        assertEquals(barn1, result.barnPersonList.firstOrNull())
        assertEquals(1, result.barnPersonList.size)
        assertEquals(forelder, result.gjenlevendeEllerAvdod)
        assertEquals(forelder, result.forsikretPerson)

        verify ( exactly = 2 ) { personService.hentPerson(any())  }

    }


    @Test
    fun `test henting komplett familie med barn for persondatacollection`() {

        //generer fnr
        val farfnr = FodselsnummerGenerator.generateFnrForTest(42)
        val morfnr = FodselsnummerGenerator.generateFnrForTest(41)
        val barn1 = FodselsnummerGenerator.generateFnrForTest(11)
        val barn2 = FodselsnummerGenerator.generateFnrForTest(13)

        //far og mor i pair
        val pair = LagPDLPerson.createPersonMedEktefellePartner(farfnr, morfnr, Sivilstandstype.GIFT)

        //far og mor med barn
        val far = pair.first.medAdresse("STORGATA").medBarn(barn1).medBarn(barn2)
        val mor = pair.second.medAdresse("STORGATA").medBarn(barn1).medBarn(barn2)

        //barn
        val barnet = lagPerson(barn1, fornavn = "OLE", etternavn = "BRUM").medForeldre(far).medForeldre(mor)
        val barnto = lagPerson(barn2, fornavn = "NASSE", etternavn = "NØFF").medForeldre(far).medForeldre(mor)

        every { personService.hentPerson(NorskIdent(farfnr)) } returns far
        every { personService.hentPerson(NorskIdent(morfnr)) } returns mor
        every { personService.hentPerson(NorskIdent(barn1)) } returns barnet
        every { personService.hentPerson(NorskIdent(barn2)) } returns barnto

        val data = PrefillDataModelMother.initialPrefillDataModel(SedType.P2000, farfnr, SAK_ID, euxCaseId = EUX_RINA)

        val result = persondataService.hentPersonData(data)

        assertEquals(mor, result.ektefellePerson)
        assertEquals(Sivilstandstype.GIFT, result.sivilstandstype)
        assertEquals(listOf(barnet, barnto), result.barnPersonList)
        assertEquals(far, result.gjenlevendeEllerAvdod)
        assertEquals(far, result.forsikretPerson)

        verify ( exactly = 4 ) { personService.hentPerson(any())  }

    }

    @Test
    fun `Gitt at fnr for bruker ikke finnes men npid finnes så forsøker vi å hente npid for aktoerId`() {
        val aktoerIdForFnr = "321654897"
        val aktoerIdForNpid = "123456789"
        every { personService.hentIdent(eq(IdentGruppe.FOLKEREGISTERIDENT), AktoerId(aktoerIdForFnr)) } returns null
        every { personService.hentIdent(eq(IdentGruppe.NPID), AktoerId(aktoerIdForFnr)) } returns null
        every { personService.hentIdent(eq(IdentGruppe.FOLKEREGISTERIDENT), AktoerId(aktoerIdForNpid)) } returns null
        every { personService.hentIdent(eq(IdentGruppe.NPID), AktoerId(aktoerIdForNpid)) } returns Npid("077080111")

        val fnr = persondataService.hentFnrEllerNpidFraAktoerService(aktoerIdForFnr)
        val npid = persondataService.hentFnrEllerNpidFraAktoerService(aktoerIdForNpid)

        //fnr og npid finnes ikke for denne aktoerIden returnerer null
        assertEquals(null, fnr)
        //fnr finnes ikke, men npid finnes aktoerIden dermed returneres npiden
        assertEquals("077080111", npid)

    }

}