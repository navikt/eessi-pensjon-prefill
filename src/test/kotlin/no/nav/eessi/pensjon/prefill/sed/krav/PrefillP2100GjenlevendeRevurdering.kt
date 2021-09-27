package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.sed.Nav
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.eux.model.sed.SedType
import no.nav.eessi.pensjon.personoppslag.FodselsnummerGenerator
import no.nav.eessi.pensjon.personoppslag.pdl.model.Sivilstandstype
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PensjonsinformasjonService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PersonId
import no.nav.eessi.pensjon.prefill.models.PrefillDataModel
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper.lesPensjonsdataFraFil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PrefillP2100GjenlevendeRevurdering {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(45)
    private val avdodPersonFnr = FodselsnummerGenerator.generateFnrForTest(45)
    private val pesysSaksnummer = "22915550"
    private val pesysKravid = "41098605"

    private lateinit var prefillData: PrefillDataModel
    private lateinit var dataFromPEN: PensjonsinformasjonService
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var prefillNav: PrefillPDLNav
    private lateinit var pensjonCollection: PensjonCollection

    @BeforeEach
    fun setup() {
        prefillNav = PrefillPDLNav(
                prefillAdresse = mockk(relaxed = true),
                institutionid = "NO:NAVAT02",
                institutionnavn = "NOINST002, NO INST002, NO")
    }

    @Test
    fun `forventet korrekt utfylt P2100 uforepensjon med kap4 og 9`() {
        prefillData = PrefillDataModelMother.initialPrefillDataModel(
                sedType = SedType.P2100,
                pinId = personFnr,
                penSaksnummer = pesysSaksnummer,
                avdod = PersonId(avdodPersonFnr, "112233445566"),
                kravId = pesysKravid)
        dataFromPEN = lesPensjonsdataFraFil("P2100-GJENLEV-REVURDERING-M-KRAVID-INNV.xml")

        val innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = dataFromPEN)
        pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)

        val person = PersonPDLMock.createWith(fornavn = "BAMSE ULUR", fnr = personFnr)
        val avdod = PersonPDLMock.createWith(fornavn = "BAMSE LUR", fnr = avdodPersonFnr, erDod = true)
        val persondataCollection = PersonDataCollection(
            forsikretPerson = person,
            sivilstandstype = Sivilstandstype.ENKE_ELLER_ENKEMANN,
            gjenlevendeEllerAvdod = avdod
        )

        prefillSEDService = PrefillSEDService(EessiInformasjon(), prefillNav)
        val p2100 = prefillSEDService.prefill(prefillData, persondataCollection, pensjonCollection)

        val p2100gjenlev = SED(
                type = SedType.P2100,
                pensjon = p2100.pensjon,
                nav = Nav(krav = p2100.nav?.krav)
        )

        assertNotNull(p2100gjenlev.nav?.krav)
        assertEquals("2020-02-12", p2100gjenlev.nav?.krav?.dato)
    }


}

