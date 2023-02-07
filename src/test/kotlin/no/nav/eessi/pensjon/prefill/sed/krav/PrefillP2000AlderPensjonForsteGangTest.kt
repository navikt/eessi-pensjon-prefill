package no.nav.eessi.pensjon.prefill.sed.krav

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.personoppslag.FodselsnummerGenerator
import no.nav.eessi.pensjon.prefill.PensjonsinformasjonService
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother.initialPrefillDataModel
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.prefill.person.PrefillPDLAdresse
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper.lesPensjonsdataFraFil
import no.nav.eessi.pensjon.prefill.sed.PrefillTestHelper.readJsonResponse
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException

class PrefillP2000AlderPensjonForsteGangTest {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(67)
    private val ekteFnr = FodselsnummerGenerator.generateFnrForTest(69)

    private lateinit var prefillData: PrefillDataModel
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var dataFromPEN: PensjonsinformasjonService
    private lateinit var persondataCollection: PersonDataCollection

    @BeforeEach
    fun setup() {
        val prefillNav = PrefillPDLNav(
                prefillAdresse = mockk<PrefillPDLAdresse>{
                    every { hentLandkode(any()) } returns "NO"
                    every { createPersonAdresse(any()) } returns mockk()
                },
                institutionid = "NO:noinst002",
                institutionnavn = "NOINST002, NO INST002, NO")

        persondataCollection = PersonPDLMock.createEnkelFamilie(personFnr, ekteFnr)


        dataFromPEN = lesPensjonsdataFraFil("/pensjonsinformasjon/krav/AP_FORSTEG_BH.xml")

        prefillData = initialPrefillDataModel(SedType.P2000, personFnr, penSaksnummer = "22580170").apply {
            partSedAsJson["PersonInfo"] = readJsonResponse("/json/nav/other/person_informasjon_selvb.json")
            partSedAsJson["P4000"] = readJsonResponse("/json/nav/other/p4000_trygdetid_part.json")
        }
        prefillSEDService = PrefillSEDService(EessiInformasjon(), prefillNav)

    }

    @Test
    fun `Sjekk av kravsøknad alderpensjon P2000`() {
        val pendata: Pensjonsinformasjon = dataFromPEN.hentPensjonInformasjon(prefillData.bruker.norskIdent, prefillData.bruker.aktorId)

        assertNotNull(PensjonsinformasjonService.finnSak(prefillData.penSaksnummer, pendata))
        assertNotNull(pendata.brukersSakerListe)
        assertEquals("ALDER", PensjonsinformasjonService.finnSak(prefillData.penSaksnummer, pendata)?.sakType)
    }

    @Test
    fun `Gitt at kravtype er FORSTEG_BH skal det kastes en exception`() {

        assertThrows<ResponseStatusException> {
            prefillSEDService.prefill(prefillData, persondataCollection, PensjonCollection(sedType = SedType.P2000))
        }
    }

}
