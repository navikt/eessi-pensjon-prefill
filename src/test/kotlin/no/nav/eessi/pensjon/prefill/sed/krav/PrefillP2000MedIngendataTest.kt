package no.nav.eessi.pensjon.prefill.sed.krav

import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.prefill.BasePrefillNav
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException


class PrefillP2000MedIngendataTest {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(68)
    private val ekteFnr = FodselsnummerGenerator.generateFnrForTest(70)

    private val pesysSaksnummer = "21644722"

    private lateinit var prefillData: PrefillDataModel
    private lateinit var prefillSEDService: PrefillSEDService
    private lateinit var pensjonCollection: PensjonCollection
    private lateinit var personDataCollection: PersonDataCollection

    @BeforeEach
    fun setup() {
        personDataCollection = PersonPDLMock.createEnkelFamilie(personFnr, ekteFnr)

//        val dataFromPEN = lesPensjonsdataFraFil("/pensjonsinformasjon/krav/P2000-TOMT-SVAR-PESYS.xml")

        prefillData = PrefillDataModelMother.initialPrefillDataModel(SedType.P2000, personFnr, penSaksnummer = pesysSaksnummer).apply {
//            partSedAsJson["PersonInfo"] = readJsonResponse("/json/nav/other/person_informasjon_selvb.json")
//            partSedAsJson["P4000"] = readJsonResponse("/json/nav/other/p4000_trygdetid_part.json")
        }
//        val innhentingService = InnhentingService(mockk(), pensjonsinformasjonService = dataFromPEN)
//        pensjonCollection = innhentingService.hentPensjoninformasjonCollection(prefillData)

        prefillSEDService = BasePrefillNav.createPrefillSEDService()

    }

    @Test
    fun `Gitt pensjonsinformasjon som mangler vedtak når preutfyller så stopp preutfylling med melding om vedtak mangler`() {
        try {
            prefillSEDService.prefill(prefillData, personDataCollection, pensjonCollection, null,)
        } catch (ex: ResponseStatusException) {
            val errormsg = """Det finnes ingen iverksatte vedtak for førstegangsbehandling kun utland, eller sluttbehandling. Vennligst gå til EESSI-Pensjon fra vedtakskontekst.""".trimIndent()
            assertEquals("400 BAD_REQUEST \"$errormsg\"", ex.message)
        }
    }

}

