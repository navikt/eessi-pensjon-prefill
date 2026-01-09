package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.prefill.BasePrefillNav
import no.nav.eessi.pensjon.prefill.InnhentingService
import no.nav.eessi.pensjon.prefill.PensjonsinformasjonService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
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
import org.springframework.web.server.ResponseStatusException

class PrefillP2000KravKunUtlandTest {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(67)
    private val ekteFnr = FodselsnummerGenerator.generateFnrForTest(70)

    lateinit var prefillData: PrefillDataModel
    lateinit var prefillSEDService: PrefillSEDService
    lateinit var dataFromPEN: PensjonsinformasjonService

    private lateinit var persondataCollection: PersonDataCollection
    private lateinit var pensjonCollection: PensjonCollection

    @BeforeEach
    fun setup() {
        persondataCollection = PersonPDLMock.createEnkelFamilie(personFnr, ekteFnr)

        dataFromPEN = lesPensjonsdataFraFil("/pensjonsinformasjon/krav/P2000-AP-KUNUTL-IKKEVIRKNINGTID.xml")

        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P2000, personFnr, penSaksnummer = "21920707").apply {
            partSedAsJson["PersonInfo"] = readJsonResponse("/json/nav/other/person_informasjon_selvb.json")
            partSedAsJson["P4000"] = readJsonResponse("/json/nav/other/p4000_trygdetid_part.json")
        }

        val innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = dataFromPEN)

        pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)
        prefillSEDService = BasePrefillNav.createPrefillSEDService()

    }

    @Test
    fun `Sjekk av kravsøknad alderpensjon P2000`() {
        val pendata: Pensjonsinformasjon = dataFromPEN.hentPensjonInformasjon(prefillData.bruker.norskIdent!!, prefillData.bruker.aktorId)

        assertNotNull(PensjonsinformasjonService.finnSak(prefillData.penSaksnummer, pendata))

        assertNotNull(pendata.brukersSakerListe)
        assertEquals("ALDER", PensjonsinformasjonService.finnSak(prefillData.penSaksnummer, pendata)?.sakType)
    }

    @Test
    fun `Utfylling alderpensjon uten kravhistorikk Kunutland uten virkningstidspunkt`() {

        val expected = "Søknad gjelder Førstegangsbehandling kun utland. Se egen rutine på navet"
        try {
            prefillSEDService.prefill(prefillData, persondataCollection, pensjonCollection, null)
        } catch (ex: ResponseStatusException) {
            assertEquals(expected, ex.reason)
        }

    }

}
