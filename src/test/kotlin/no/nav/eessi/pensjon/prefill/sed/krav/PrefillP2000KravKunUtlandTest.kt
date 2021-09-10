package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.sed.SedType
import no.nav.eessi.pensjon.prefill.PensjonsinformasjonService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModel
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.models.eessi.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.pdl.FodselsnummerMother.generateRandomFnr
import no.nav.eessi.pensjon.prefill.models.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper.lesPensjonsdataFraFil
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper.readJsonResponse
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException


class PrefillP2000KravKunUtlandTest {

    private val personFnr = generateRandomFnr(67)
    private val ekteFnr = generateRandomFnr(70)

    lateinit var prefillData: PrefillDataModel
    lateinit var dataFromPEN: PensjonsinformasjonService
    lateinit var prefillSEDService: PrefillSEDService

    private lateinit var persondataCollection: PersonDataCollection
    private lateinit var pensjonCollection: PensjonCollection

    @BeforeEach
    fun setup() {
        persondataCollection = PersonPDLMock.createEnkelFamilie(personFnr, ekteFnr)

        val prefillNav = PrefillPDLNav(
                prefillAdresse = mockk(){
                    every { hentLandkode(any()) } returns "NO"
                    every { createPersonAdresse(any())} returns mockk(relaxed = true)
                },
                institutionid = "NO:noinst002",
                institutionnavn = "NOINST002, NO INST002, NO")

        dataFromPEN = lesPensjonsdataFraFil("P2000-AP-KUNUTL-IKKEVIRKNINGTID.xml")

        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P2000, personFnr, penSaksnummer = "21920707").apply {
            partSedAsJson["PersonInfo"] = readJsonResponse("other/person_informasjon_selvb.json")
            partSedAsJson["P4000"] = readJsonResponse("other/p4000_trygdetid_part.json")
        }

        val innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = dataFromPEN)

        pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)
        prefillSEDService = PrefillSEDService(EessiInformasjon(), prefillNav)

    }

    @Test
    fun `Sjekk av kravsøknad alderpensjon P2000`() {
        val pendata: Pensjonsinformasjon = dataFromPEN.hentPensjonInformasjon(prefillData.bruker.aktorId)

        assertNotNull(PensjonsinformasjonService.finnSak(prefillData.penSaksnummer, pendata))

        assertNotNull(pendata.brukersSakerListe)
        assertEquals("ALDER", PensjonsinformasjonService.finnSak(prefillData.penSaksnummer, pendata)?.sakType)
    }

    @Test
    fun `Utfylling alderpensjon uten kravhistorikk Kunutland uten virkningstidspunkt`() {

        val expected = "Søknad gjelder Førstegangsbehandling kun utland. Se egen rutine på navet"
        try {
            prefillSEDService.prefill(prefillData, persondataCollection, pensjonCollection)
        } catch (ex: ResponseStatusException) {
            assertEquals(expected, ex.reason)
        }

    }

}
