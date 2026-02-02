package no.nav.eessi.pensjon.prefill

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.BucType
import no.nav.eessi.pensjon.eux.model.BucType.*
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.SedType.*
import no.nav.eessi.pensjon.personoppslag.pdl.model.AktoerId
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentGruppe.AKTORID
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.shared.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

private const val AKTOERID = "467846784671"
private const val FNR = "46784678467"

class InnhentingServiceTest {

    private var pesysService: PesysService = mockk()
    private var personDataService: PersonDataService = mockk()

    private lateinit var innhentingService: InnhentingService

    @BeforeEach
    fun before() {
        innhentingService = InnhentingService(personDataService, pesysService = pesysService)
    }

    fun setup() {
        every { pesysService.hentP2100data(any()) } returns mockk() {
            every { sak } returns P2xxxMeldingOmPensjonDto.Sak(
                sakType = EessiFellesDto.EessiSakType.GJENLEV,
                kravHistorikk = listOf(
                    P2xxxMeldingOmPensjonDto.KravHistorikk(
                        mottattDato = LocalDate.of(2015, 11, 25),
                        kravType = EessiFellesDto.EessiKravGjelder.F_BH_KUN_UTL,
                        virkningstidspunkt = LocalDate.of(2015, 11, 25),
                    )
                ),
                ytelsePerMaaned = emptyList(),
                forsteVirkningstidspunkt = LocalDate.of(2025, 12, 12),
                status = EessiFellesDto.EessiSakStatus.TIL_BEHANDLING,
            )
            every { vedtak } returns P2xxxMeldingOmPensjonDto.Vedtak(boddArbeidetUtland = true)
        }

    }

    @Test
    fun `call getAvdodAktoerId  expect valid aktoerId when avdodfnr exist and sed is P2100`() {
        innhentingService = InnhentingService(personDataService, pesysService = pesysService)

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
        private val pesysService: PesysService = mockk()
        val innhentingsService = InnhentingService(personDataService, pesysService = pesysService)

        @BeforeEach
        fun setup() {
            every { pesysService.hentP2100data(any()) } returns mockk() {
                every { sak } returns P2xxxMeldingOmPensjonDto.Sak(
                    sakType = EessiFellesDto.EessiSakType.ALDER,
                    kravHistorikk = listOf(
                        P2xxxMeldingOmPensjonDto.KravHistorikk(
                            mottattDato = LocalDate.of(2015, 11, 25),
                            kravType = EessiFellesDto.EessiKravGjelder.F_BH_KUN_UTL,
                            virkningstidspunkt = LocalDate.of(2015, 11, 25),
                        )
                    ),
                    ytelsePerMaaned = emptyList(),
                    forsteVirkningstidspunkt = LocalDate.of(2025, 12, 12),
                    status = EessiFellesDto.EessiSakStatus.TIL_BEHANDLING,
                )
                every { vedtak } returns P2xxxMeldingOmPensjonDto.Vedtak(boddArbeidetUtland = true)
            }
        }

        @Test
        fun `Gitt en P2100 med saktype ALDER saa skal hentPensjoninformasjonCollection sitt resultat paa saktype gi ut ALDER`() {
            val prefillData = prefillDataModel(sedType = P2100, vedtakId = "123456" )

            val resultat = innhentingsService.hentPensjoninformasjonCollection(prefillData)
            assertEquals(EessiFellesDto.EessiSakType.ALDER, resultat.p2xxxMeldingOmPensjonDto?.sak?.sakType)
        }

        @Test
        fun `Gitt en P15000 med saktype UFORE saa skal hentPensjoninformasjonCollection sitt resultat paa saktype returnere UFOREP`() {
            every { pesysService.hentP15000data(any()) } returns mockk() {
                every { sakType } returns EessiFellesDto.EessiSakType.UFOREP.name

            }
            val prefillData = prefillDataModel(sedType = P15000, vedtakId = "2321654")

            val resultat = innhentingsService.hentPensjoninformasjonCollection(prefillData)
            assertEquals(EessiFellesDto.EessiSakType.UFOREP.name, resultat.p15000Data?.sakType)

        }

        @Test
        fun `Gitt en P8000 med saktype ALDER saa skal hentPensjoninformasjonCollection sitt resultat paa saktype returnere ALDER`() {
            val prefillData = prefillDataModel(sedType = P8000, vedtakId = "2321654")

            val resultat = innhentingsService.hentPensjoninformasjonCollection(prefillData)
            assertEquals(null, resultat.p8000Data?.sakType)

        }

        @Test
        fun `Gitt en P8000 med på en P_BUC_05 med saktype ALDER saa skal hentPensjoninformasjonCollection sitt resultat paa saktype returnere ALDER`() {
            val prefillData = prefillDataModel(sedType = P8000, bucType = P_BUC_05, vedtakId = "2321654")

            val resultat = innhentingsService.hentPensjoninformasjonCollection(prefillData)
            assertEquals(EessiFellesDto.EessiSakType.ALDER, resultat.p8000Data?.sakType)

        }

        private fun prefillDataModel(
            fnr: String = FNR,
            aktorId: String = AKTOERID,
            sedType: SedType,
            bucType: BucType = BucType.P_BUC_10,
            vedtakId: String? = null
        ) = PrefillDataModel(
            penSaksnummer = "1010",
            bruker = PersonInfo(fnr, aktorId),
            avdod = null,
            sedType = sedType,
            buc = bucType,
            vedtakId = vedtakId,
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