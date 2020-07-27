package no.nav.eessi.pensjon.api.person

import com.nhaarman.mockitokotlin2.*
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonService
import no.nav.eessi.pensjon.logging.AuditLogger
import no.nav.eessi.pensjon.personoppslag.aktoerregister.*
import no.nav.eessi.pensjon.personoppslag.personv3.PersonV3IkkeFunnetException
import no.nav.eessi.pensjon.personoppslag.personv3.PersonV3Service
import no.nav.eessi.pensjon.services.pensjonsinformasjon.PensjonsinformasjonClient
import no.nav.eessi.pensjon.services.pensjonsinformasjon.PensjonsinformasjonClientMother
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.typeRefs
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.ComponentScan
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status


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

    @MockBean
    lateinit var auditLogger: AuditLogger

    @MockBean
    lateinit var mockPensjonClient: PensjonsinformasjonClient

    @Test
    fun `getPerson should return Person as json`() {

        doNothing().whenever(auditLogger).log(any(), any())
        whenever(mockAktoerregisterService.hentGjeldendeNorskIdentForAktorId(anAktorId)).thenReturn(anFnr)
        whenever(mockPersonV3Service.hentPersonResponse(anFnr)).thenReturn(hentPersonResponse)

        val response = mvc.perform(
            get("/person/$anAktorId")
                .accept(MediaType.APPLICATION_JSON))
            .andReturn().response

        JSONAssert.assertEquals(personAsJson, response.contentAsString, false)
    }

    @Test
    fun `getNameOnly should return names as json`() {
        doNothing().whenever(auditLogger).log(any(), any())
        whenever(mockAktoerregisterService.hentGjeldendeNorskIdentForAktorId(anAktorId)).thenReturn(anFnr)
        whenever(mockPersonV3Service.hentPersonResponse(anFnr)).thenReturn(hentPersonResponse)

        val response = mvc.perform(
            get("/personinfo/$anAktorId")
                .accept(MediaType.APPLICATION_JSON))
            .andReturn().response

        JSONAssert.assertEquals(namesAsJson, response.contentAsString, false)
    }

    @Test
    fun `should return NOT_FOUND hvis personen ikke finnes i TPS`() {
        doThrow(PersonV3IkkeFunnetException("Error is Expected")).whenever(mockPersonV3Service).hentPersonResponse(anFnr)
        doNothing().whenever(auditLogger).log(any(), any())
        doReturn(anFnr).whenever(mockAktoerregisterService).hentGjeldendeNorskIdentForAktorId(anAktorId)

        mvc.perform(
            get("/personinfo/$anAktorId")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound)
    }

    @Test
    fun `getDeceased should return a list of deceased parents given a remaining, living child` (){
        //given two deceased (mor & far), with a (living) remaining child
        val avdodMorAktorId = "111111"
        val avdodFarAktorId = "222222"

        val gjenlevBarnAktorId = "333333"

        val vedtaksInfo = PensjonsinformasjonService(PensjonsinformasjonClientMother.fraFil("P6000-GP-401.xml")).hentMedVedtak("any-given-string")
        vedtaksInfo.avdod.avdodMorAktorId = avdodMorAktorId
        vedtaksInfo.avdod.avdodFarAktorId = avdodFarAktorId
        vedtaksInfo.person.aktorId = gjenlevBarnAktorId

        //and mor is part of the system (insured)
        val personMor = HentPersonResponse()
                .withPerson(Person()
                        .withPersonnavn(Personnavn()
                                .withFornavn("MOR")))
        //and far is part of the system (insured)
        val personFar = HentPersonResponse()
                .withPerson(Person()
                        .withPersonnavn(Personnavn()
                                .withFornavn("FAR")))

        val vedtaksId = "22455454"
        val avdodMorFnr = "11111111111"
        val avdodFarFnr = "22222222222"

        doReturn(vedtaksInfo).whenever(mockPensjonClient).hentAltPaaVedtak(vedtaksId)
        doReturn(Result.Found(NorskIdent(avdodMorFnr)))
                .whenever(mockAktoerregisterService).hentGjeldendeIdentFraGruppe(IdentGruppe.NorskIdent, AktoerId(avdodMorAktorId))
        doReturn(Result.Found(NorskIdent(avdodFarFnr)))
                .whenever(mockAktoerregisterService).hentGjeldendeIdentFraGruppe(IdentGruppe.NorskIdent, AktoerId(avdodFarAktorId))
        doReturn(personFar).whenever(mockPersonV3Service).hentPersonResponse(avdodFarFnr)
        doReturn(personMor).whenever(mockPersonV3Service).hentPersonResponse(avdodMorFnr)

        val response = mvc.perform(
                get("/person/333333/avdode/vedtak/22455454")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn().response

        print("response: " + response.contentAsString)

        //then the response should contain json list of mor & far
        Assertions.assertTrue(response.contentAsString.contains("\"fnr\":\"$avdodFarFnr\",\"aktorId\":\"$avdodFarAktorId\""))
        Assertions.assertTrue(response.contentAsString.contains("\"fnr\":\"$avdodMorFnr\",\"aktorId\":\"$avdodMorAktorId\""))

        val emptyList : List<PersonController.PersoninformasjonAvdode> = mapJsonToAny(response.contentAsString, typeRefs())
        assert(emptyList.size == 2)
    }


    @Test
    fun `getDeceased should return a list of one parent given a remaining, living child` (){
        //given two deceased (mor & far), with a (living) remaining child
        val vedtaksInfo = PensjonsinformasjonService(PensjonsinformasjonClientMother.fraFil("P6000-GP-401.xml")).hentMedVedtak("any-given-string")
        vedtaksInfo.avdod.avdodFarAktorId = null
        vedtaksInfo.avdod.avdodMorAktorId = "111111"

        val gjenlevAktorId = vedtaksInfo.person.aktorId
        val avdodMorAktorId = vedtaksInfo.avdod.avdodMorAktorId

        val vedtaksId = "22455454"
        val avdodMorFnr = "11111111111"

        //and mor is part of the system (insured)
        val personMor = HentPersonResponse()
                .withPerson(Person()
                        .withPersonnavn(Personnavn()
                                .withFornavn("MOR")))

        doReturn(vedtaksInfo).whenever(mockPensjonClient).hentAltPaaVedtak(vedtaksId)
        doReturn(Result.Found(NorskIdent(avdodMorFnr)))
                .whenever(mockAktoerregisterService).hentGjeldendeIdentFraGruppe(IdentGruppe.NorskIdent, AktoerId(avdodMorAktorId))
        doReturn(personMor).whenever(mockPersonV3Service).hentPersonResponse(avdodMorFnr)

        val response = mvc.perform(
                get("/person/$gjenlevAktorId/avdode/vedtak/$vedtaksId")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn().response

        //then the response should contain json list of mor & far
        Assertions.assertTrue(response.contentAsString.contains("\"fnr\":\"$avdodMorFnr\",\"aktorId\":\"$avdodMorAktorId\""))

        val emptyList : List<PersonController.PersoninformasjonAvdode> = mapJsonToAny(response.contentAsString, typeRefs())
        assert(emptyList.size == 1)
    }


    @Test
    fun `getDeceased should return an empty list when both partents are alive` (){
        //given two deceased (mor & far), with a (living) remaining child
        val vedtaksInfo = PensjonsinformasjonService(PensjonsinformasjonClientMother.fraFil("P6000-GP-401.xml")).hentMedVedtak("any-given-string")
        vedtaksInfo.avdod.avdodFarAktorId = null
        vedtaksInfo.avdod.avdodMorAktorId = null

        val gjenlevAktorId = vedtaksInfo.person.aktorId
        val vedtaksId = "22455454"

        doReturn(vedtaksInfo).whenever(mockPensjonClient).hentAltPaaVedtak(vedtaksId)

        val response = mvc.perform(
                get("/person/$gjenlevAktorId/avdode/vedtak/$vedtaksId")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn().response
        val list : List<String>  = mapJsonToAny(response.contentAsString, typeRefs())
        assert(list.isEmpty())
    }


    private val anAktorId = "012345"
    private val anFnr = "01010123456"

    private val hentPersonResponse =
            HentPersonResponse()
                    .withPerson(Person()
                            .withPersonnavn(Personnavn()
                                    .withFornavn("OLA")
                                    .withEtternavn("NORDMANN")
                                    .withSammensattNavn("NORDMANN OLA")))

    private val personAsJson = """{
                  "person": {
                    "diskresjonskode": null,
                    "bostedsadresse": null,
                    "sivilstand": null,
                    "statsborgerskap": null,
                    "harFraRolleI": [],
                    "aktoer": null,
                    "kjoenn": null,
                    "personnavn": { "fornavn": "OLA", "etternavn": "NORDMANN", "mellomnavn": null, "sammensattNavn": "NORDMANN OLA", "endringstidspunkt": null, "endretAv": null, "endringstype": null},
                    "personstatus": null,
                    "postadresse": null,
                    "doedsdato": null,
                    "foedselsdato": null
                  }
                }"""

    private val namesAsJson = """{ fornavn: "OLA", etternavn: "NORDMANN", mellomnavn: null, fulltNavn: "NORDMANN OLA"}"""
}