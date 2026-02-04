package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.Nav
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.prefill.BasePrefillNav
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.PesysService
import no.nav.eessi.pensjon.prefill.models.pensjon.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.models.YtelseskomponentType
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.prefill.models.pensjon.Ytelseskomponent
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PrefillP2100GjenlevendeRevurdering {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(45)
    private val avdodPersonFnr = FodselsnummerGenerator.generateFnrForTest(45)
    private val pesysSaksnummer = "22915550"
    private val pesysService : PesysService = mockk()
    private val pesysKravid = "41098605"

    private lateinit var prefillData: PrefillDataModel
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var pensjonCollection: PensjonCollection

    @BeforeEach
    fun setup() {
    }

    @Test
    fun `forventet korrekt utfylt P2100 uforepensjon med kap4 og 9`() {
        every { pesysService.hentP2100data(any()) } returns mockk(){
            every { sak } returns P2xxxMeldingOmPensjonDto.Sak(
                sakType = EessiSakType.GJENLEV,
                kravHistorikk = listOf(
                    P2xxxMeldingOmPensjonDto.KravHistorikk(
                        mottattDato = LocalDate.of(2020, 2, 12),
                        kravType = EessiFellesDto.EessiKravGjelder.F_BH_KUN_UTL,
                        virkningstidspunkt = LocalDate.of(2015, 11, 25),
                    )
                ),
                ytelsePerMaaned = listOf(
                    P2xxxMeldingOmPensjonDto.YtelsePerMaaned(
                        fom = LocalDate.of(2015, 11, 25),
                        belop = 123,
                        ytelseskomponentListe = listOf(
                            Ytelseskomponent(
                                YtelseskomponentType.GAP.name,
                                444
                            )
                        )
                    )
                ),
                forsteVirkningstidspunkt = LocalDate.of(2025, 12, 12),
                status = EessiFellesDto.EessiSakStatus.TIL_BEHANDLING,
            )
            every { vedtak } returns P2xxxMeldingOmPensjonDto.Vedtak(boddArbeidetUtland = true)
        }
        prefillData = PrefillDataModelMother.initialPrefillDataModel(
                sedType = SedType.P2100,
                pinId = personFnr,
                penSaksnummer = pesysSaksnummer,
                avdod = PersonInfo(avdodPersonFnr, "112233445566"),
                kravId = pesysKravid)

        val innhentingService = InnhentingService(mockk(), pesysService = pesysService)
        pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)

        val person = PersonPDLMock.createWith(fornavn = "BAMSE ULUR", fnr = personFnr)
        val avdod = PersonPDLMock.createWith(fornavn = "BAMSE LUR", fnr = avdodPersonFnr, erDod = true)
        val persondataCollection = PersonDataCollection(
            forsikretPerson = person,
            gjenlevendeEllerAvdod = avdod
        )

        prefillSEDService = BasePrefillNav.createPrefillSEDService()
        val p2100 = prefillSEDService.prefill(prefillData, persondataCollection, pensjonCollection, null,)

        val p2100gjenlev = SED(
                type = SedType.P2100,
                pensjon = p2100.pensjon,
                nav = Nav(krav = p2100.nav?.krav)
        )

        assertNotNull(p2100gjenlev.nav?.krav)
        assertEquals("2020-02-12", p2100gjenlev.nav?.krav?.dato)
    }


}

