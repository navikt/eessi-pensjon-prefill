package no.nav.eessi.pensjon.prefill

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.BucType
import no.nav.eessi.pensjon.eux.model.BucType.*
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.SedType.*
import no.nav.eessi.pensjon.pensjonsinformasjon.clients.PensjonsinformasjonClient
import no.nav.eessi.pensjon.personoppslag.pdl.model.AktoerId
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentGruppe.AKTORID
import no.nav.eessi.pensjon.shared.api.*
import no.nav.pensjon.v1.brukerssakerliste.V1BrukersSakerListe
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.sak.V1Sak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException


private const val AKTOERID = "467846784671"
private const val FNR = "46784678467"

private const val SAKTYPE_ALDER = "ALDER"
private const val SAKTYPE_UFORE = "UFOREP"

class InnhentingServiceTest {

    var personDataService: PersonDataService = mockk()
    var pensjonsinformasjonService: PensjonsinformasjonService = mockk()

    private lateinit var innhentingService: InnhentingService

    @BeforeEach
    fun before() {
        innhentingService = InnhentingService(personDataService, pensjonsinformasjonService = pensjonsinformasjonService)
    }

    @Test
    fun `call getAvdodAktoerId  expect valid aktoerId when avdodfnr exist and sed is P2100`() {
        val apiRequest = ApiRequest(
            subjectArea = "Pensjon",
            sakId = "EESSI-PEN-123",
            sed = P2100,
            buc = P_BUC_02,
            aktoerId = "0105094340092",
            avdodfnr = "12345566"

        )
        every { personDataService.hentIdent(eq(AKTORID), any()) } returns AktoerId(AKTOERID)

        val result = innhentingService.getAvdodAktoerIdPDL(apiRequest)
        assertEquals(AKTOERID, result)
    }

    @Test
    fun `call getAvdodAktoerId  expect valid aktoerId when avdod exist and sed is P5000`() {
        val apiRequest = ApiRequest(
            subjectArea = "Pensjon",
            sakId = "EESSI-PEN-123",
            sed = P5000,
            buc = P_BUC_02,
            aktoerId = "0105094340092",
            avdodfnr = "12345566",
            vedtakId = "23123123",
            subject = ApiSubject(gjenlevende = SubjectFnr("23123123"), avdod = SubjectFnr(FNR))
        )

        every { personDataService.hentIdent(eq(AKTORID), any()) } returns AktoerId(AKTOERID)

        val result = innhentingService.getAvdodAktoerIdPDL(apiRequest)
        assertEquals(AKTOERID, result)
    }

    @Test
    fun `call getAvdodAktoerId  expect error when avdodfnr is missing and sed is P2100`() {
        val apiRequest = ApiRequest(
            subjectArea = "Pensjon",
            sakId = "EESSI-PEN-123",
            sed = P2100,
            buc = P_BUC_02,
            aktoerId = "0105094340092"
        )
        assertThrows<ResponseStatusException> {
            innhentingService.getAvdodAktoerIdPDL(apiRequest)
        }
    }

    @Test
    fun `call getAvdodAktoerId expect error when avdodfnr is invalid and sed is P15000`() {
        val apiRequest = ApiRequest(
            subjectArea = "Pensjon",
            sakId = "EESSI-PEN-123",
            sed = P15000,
            buc = BucType.P_BUC_10,
            aktoerId = "0105094340092",
            avdodfnr = "12345566"
        )
        assertThrows<ResponseStatusException> {
            innhentingService.getAvdodAktoerIdPDL(apiRequest)
        }
    }

    @Test
    fun `call getAvdodAktoerId  expect null value when sed is P2000`() {
        val apireq = ApiRequest(
            subjectArea = "Pensjon",
            sakId = "EESSI-PEN-123",
            sed = P2000,
            buc = P_BUC_01,
            aktoerId = "0105094340092",
            avdodfnr = "12345566"
        )
        val result = innhentingService.getAvdodAktoerIdPDL(request = apireq)
        assertEquals(null, result)
    }

    class InnhentingSaktyperTest {
        val personDataService: PersonDataService = mockk()
        val pensjonsinformasjonClient: PensjonsinformasjonClient = mockk(relaxed = true)

        val pensjonsinformasjonService = PensjonsinformasjonService(pensjonsinformasjonClient)
        val innhentingsservice = InnhentingService(personDataService, pensjonsinformasjonService = pensjonsinformasjonService)

        @Test
        fun `Gitt en P2100 med saktype ALDER saa skal hentPensjoninformasjonCollection sitt resultat paa saktype gi ut ALDER`() {
            val prefillData = prefillDataModel(sedType = P2100)
            val peninfo = pensjonsinformasjon(SAKTYPE_ALDER)

            every { pensjonsinformasjonClient.hentAltPaaFNR(FNR) } returns peninfo

            val resultat = innhentingsservice.hentPensjoninformasjonCollection(prefillData)
            assertEquals(SAKTYPE_ALDER, resultat.sak?.sakType)
            assertEquals(SAKTYPE_ALDER, resultat.sak?.sakType)
        }

        @Test
        fun `Gitt en P15000 med saktype UFORE saa skal hentPensjoninformasjonCollection sitt resultat paa saktype returnere UFOREP`() {
            val prefillData = prefillDataModel(sedType = P15000)
            val peninfo = pensjonsinformasjon(SAKTYPE_UFORE)

            every { pensjonsinformasjonClient.hentAltPaaFNR(FNR) } returns peninfo

            val resultat = innhentingsservice.hentPensjoninformasjonCollection(prefillData)
            assertEquals(SAKTYPE_UFORE, resultat.sak?.sakType)

        }

        @Test
        fun `Gitt en P8000 med saktype ALDER saa skal hentPensjoninformasjonCollection sitt resultat paa saktype returnere ALDER`() {
            val prefillData = prefillDataModel(sedType = P8000)
            val peninfo = pensjonsinformasjon(SAKTYPE_ALDER)

            every { pensjonsinformasjonClient.hentAltPaaFNR(FNR) } returns peninfo

            val resultat = innhentingsservice.hentPensjoninformasjonCollection(prefillData)
            assertEquals(null, resultat.sak?.sakType)

        }

        @Test
        fun `Gitt en P8000 med på en P_BUC_05 med saktype ALDER saa skal hentPensjoninformasjonCollection sitt resultat paa saktype returnere ALDER`() {
            val prefillData = prefillDataModel(sedType = P8000, bucType = P_BUC_05)
            val peninfo = pensjonsinformasjon(SAKTYPE_ALDER)

            every { pensjonsinformasjonClient.hentAltPaaFNR(FNR) } returns peninfo

            val resultat = innhentingsservice.hentPensjoninformasjonCollection(prefillData)
            assertEquals(SAKTYPE_ALDER, resultat.sak?.sakType)

        }

        private fun pensjonsinformasjon(saktype: String) : Pensjonsinformasjon {
            val pensjonInformasjon = Pensjonsinformasjon()
            val mocksak = V1Sak()
            mocksak.sakId = 1010
            mocksak.status = "INNV"
            mocksak.sakType = saktype
            pensjonInformasjon.brukersSakerListe = V1BrukersSakerListe()
            pensjonInformasjon.brukersSakerListe.brukersSakerListe.add(mocksak)
            return pensjonInformasjon
        }

        private fun prefillDataModel(
            fnr: String = FNR,
            aktorId: String = AKTOERID,
            sedType: SedType,
            bucType: BucType = BucType.P_BUC_10
        ) = PrefillDataModel(
            penSaksnummer = "1010",
            bruker = PersonInfo(fnr, aktorId),
            avdod = null,
            sedType = sedType,
            buc = bucType,
            vedtakId = null,
            kravDato = null,
            kravId = null,
            kravType = null,
            euxCaseID = "1234569",
            institution = emptyList(),
            refTilPerson = null,
            melding = null,
        )
    }

}