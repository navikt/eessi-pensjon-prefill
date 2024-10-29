package no.nav.eessi.pensjon.prefill.sed

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.person.PrefillSed
import no.nav.eessi.pensjon.shared.api.ApiRequest
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import no.nav.eessi.pensjon.utils.mapJsonToAny
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class PrefillP4000Test {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(68)
    private lateinit var p4000: PrefillP4000
    private lateinit var prefillNav: PrefillPDLNav
    @BeforeEach
    fun setup() {
        prefillNav = PrefillPDLNav(
            prefillAdresse = mockk {
                every { hentLandkode(any()) } returns "NO"
                every { createPersonAdresse(any()) } returns mockk(relaxed = true)
            },
            institutionid = "NO:noinst002",
            institutionnavn = "NOINST002, NO INST002, NO")

        p4000 = PrefillP4000(PrefillSed(prefillNav))
    }

    @Test
    fun `Ser at P4000 prefiller med gjenlevende`() {

        val data = ApiRequest.buildPrefillDataModelOnExisting(
            mapJsonToAny<ApiRequest>(apiRequest()).copy(
                payload = javaClass.getResource("/json/nav/P4000-NAV.json").readText()
            ), PersonInfo("12345", personFnr)
        )
        val personData = PersonDataCollection(
            forsikretPerson = PersonPDLMock.createWith(),
            gjenlevendeEllerAvdod = PersonPDLMock.createWith()
        )

        val sed = p4000.prefill(data, personData)
        assertEquals("3123", sed.p4000Pensjon?.gjenlevende?.person?.pin?.get(0)?.identifikator)
    }

    fun apiRequest(): String {
        return """
            {
               "sakId":"254",
               "vedtakId":null,
               "kravId":null,
               "kravDato":null,
               "kravType":null,
               "aktoerId":"2182545702388",
               "fnr":null,
               "payload":null,
               "buc":"P_BUC_02",
               "sed":"P4000",
               "documentid":null,
               "euxCaseId":"1446892",
               "institutions":[],
               "subjectArea":null,
               "avdodfnr":"12478326775",
               "subject":{
                  "gjenlevende":{
                     "fnr":"11068320811"
                  },
                  "avdod":{
                     "fnr":"12478326775"
                  }
               },
               "referanseTilPerson":null,
               "gjenny":true
            }
        """.trimIndent()
    }
}