package no.nav.eessi.pensjon.prefill.sed

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.personoppslag.pdl.model.UtenlandskIdentifikasjonsnummer
import no.nav.eessi.pensjon.prefill.BasePrefillNav
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.PersonPDLMock.mockMeta
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.person.PrefillSed
import no.nav.eessi.pensjon.shared.api.ApiRequest
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.toJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class PrefillP4000Test {

    private val personFnr = FodselsnummerGenerator.generateFnrForTest(68)
    private lateinit var p4000: PrefillP4000
    private lateinit var prefillNav: PrefillPDLNav
    @BeforeEach
    fun setup() {
        prefillNav = BasePrefillNav.createPrefillNav()
        p4000 = PrefillP4000(PrefillSed(prefillNav))
    }

    @Test
    fun `Ser at P4000 prefiller med gjenlevende`() {
        val apiRequest: ApiRequest =  mapJsonToAny<ApiRequest>(apiRequest())
        val data = ApiRequest.buildPrefillDataModelOnExisting(
            apiRequest.copy(payload = javaClass.getResource("/json/nav/P4000-NAV.json")!!.readText()),
            PersonInfo("12345", apiRequest.aktoerId!!), personFnr
        )

        val personData = PersonDataCollection(
            forsikretPerson = PersonPDLMock.createWith().copy(utenlandskIdentifikasjonsnummer = listOf(
                UtenlandskIdentifikasjonsnummer(
                    "123123123",
                    "AUT",
                    false,
                    metadata = mockMeta(
                    )
                )
        )),
            gjenlevendeEllerAvdod = PersonPDLMock.createWith()
        )

        val sed = p4000.prefill(data, personData)

        assertEquals("3123", sed.p4000Pensjon?.gjenlevende?.person?.pin?.get(0)?.identifikator)
        assertEquals("123123123", sed.p4000Pensjon?.gjenlevende?.person?.pin?.firstOrNull { it.land == "AU" }?.identifikator)
        assertEquals("AU", sed.p4000Pensjon?.gjenlevende?.person?.pin?.firstOrNull { it.land == "AU" }?.land)
    }

    @Test
    fun `Ser at P4000 prefiller forsikret uten uid fra USA som ikke er et EU land`() {
        val apiRequest: ApiRequest =  mapJsonToAny<ApiRequest>(apiRequest())
        val data = ApiRequest.buildPrefillDataModelOnExisting(
            apiRequest.copy(payload = javaClass.getResource("/json/nav/P4000-NAV.json")!!.readText()),
            PersonInfo("12345", apiRequest.aktoerId!!), personFnr
        )

        val personData = PersonDataCollection(
            forsikretPerson = PersonPDLMock.createWith().copy(utenlandskIdentifikasjonsnummer = listOf(
                UtenlandskIdentifikasjonsnummer(
                    "123123123",
                    "USA",
                    false,
                    metadata = mockMeta(
                    )
                )
            )),
            gjenlevendeEllerAvdod = PersonPDLMock.createWith()
        )

        val sed = p4000.prefill(data, personData)

        assertEquals("3123", sed.p4000Pensjon?.gjenlevende?.person?.pin?.get(0)?.identifikator)
        assertEquals(null, sed.p4000Pensjon?.gjenlevende?.person?.pin?.firstOrNull { it.land == "US" }?.identifikator)
        assertEquals(null, sed.nav?.bruker?.person?.pin?.firstOrNull { it.land == "US" }?.identifikator)
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