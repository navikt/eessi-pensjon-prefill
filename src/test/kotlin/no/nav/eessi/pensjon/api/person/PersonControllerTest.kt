package no.nav.eessi.pensjon.api.person

import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.pensjon.services.aktoerregister.AktoerregisterService
import no.nav.eessi.pensjon.services.personv3.PersonV3Service
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import org.junit.Test
import org.junit.runner.RunWith
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.ComponentScan
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get


@RunWith(SpringRunner::class)
@WebMvcTest(PersonController::class)
@ComponentScan(basePackages = ["no.nav.eessi.pensjon.api.person"])
@ActiveProfiles("unsecured-webmvctest")
class PersonControllerTest {

    @Autowired
    lateinit var mvc: MockMvc

    @MockBean
    lateinit var mockAktoerregisterService: AktoerregisterService

    @MockBean
    lateinit var mockPersonV3Service: PersonV3Service

    @Test
    fun `getPerson should return Person as json`() {
        val anAktorId = "012345"
        val anFnr = "01010123456"

        whenever(mockAktoerregisterService.hentGjeldendeNorskIdentForAktorId(anAktorId)).thenReturn(anFnr)

        whenever(mockPersonV3Service.hentPerson(anFnr)).thenReturn(
            HentPersonResponse().withPerson(Person()))

        val response = mvc.perform(
            get("/person/$anAktorId")
                .accept(MediaType.APPLICATION_JSON))
            .andReturn().response

        JSONAssert.assertEquals(personAsJson(), response.contentAsString, false)
    }

    private fun personAsJson() = """{
                  "person": {
                    "diskresjonskode": null,
                    "bostedsadresse": null,
                    "sivilstand": null,
                    "statsborgerskap": null,
                    "harFraRolleI": [],
                    "aktoer": null,
                    "kjoenn": null,
                    "personnavn": null,
                    "personstatus": null,
                    "postadresse": null,
                    "doedsdato": null,
                    "foedselsdato": null
                  }
                }"""
}
