package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.prefill.BasePrefillNav
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PensjonsinformasjonService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLAdresse
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.EtterlatteService
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper.lesPensjonsdataFraFil
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper.readJsonResponse
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PrefillP2000UtenKravhistorieTest {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(47)
    private val barn1Fnr = FodselsnummerGenerator.generateFnrForTest(12)
    private val barn2Fnr = FodselsnummerGenerator.generateFnrForTest(14)

    private lateinit var prefillData: PrefillDataModel
    private lateinit var etterlatteService: EtterlatteService
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var pensjonCollection: PensjonCollection
    private lateinit var dataFromPEN: PensjonsinformasjonService
    private lateinit var personDataCollection: PersonDataCollection

    @BeforeEach
    fun setup() {
        etterlatteService = mockk()
       personDataCollection = PersonPDLMock.createEnkeWithBarn(personFnr, barn1Fnr, barn2Fnr)

        val prefillNav = BasePrefillNav.createPrefillNav()

        dataFromPEN = lesPensjonsdataFraFil("/pensjonsinformasjon/krav/PensjonsinformasjonSaksliste-AP-14069110.xml")
        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P2000, personFnr, penSaksnummer = "14069110").apply {
            partSedAsJson["PersonInfo"] = readJsonResponse("/json/nav/other/person_informasjon_selvb.json")
            partSedAsJson["P4000"] = readJsonResponse("/json/nav/other/p4000_trygdetid_part.json")
        }
        val innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = dataFromPEN)
        pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)

        prefillSEDService = PrefillSEDService(EessiInformasjon(), prefillNav)
    }

    @Test
    fun `Sjekk av kravsoknad alderpensjon P2000`() {
        val pendata: Pensjonsinformasjon = dataFromPEN.hentPensjonInformasjon(prefillData.bruker.norskIdent, prefillData.bruker.aktorId)

        assertNotNull(PensjonsinformasjonService.finnSak(prefillData.penSaksnummer, pendata))

        assertNotNull(pendata.brukersSakerListe)
        assertEquals("ALDER", PensjonsinformasjonService.finnSak(prefillData.penSaksnummer, pendata)?.sakType)

    }

    @Test
    fun `Preutfylling P2000 uten kravdato skal feile`() {
        val ex = assertThrows<Exception> {
            prefillSEDService.prefill(prefillData, personDataCollection,pensjonCollection, null)
        }
        assertEquals("400 BAD_REQUEST \"Det finnes ingen iverksatte vedtak for førstegangsbehandling kun utland. Vennligst gå til EESSI-Pensjon fra vedtakskontekst.\"", ex.message)
    }


}
