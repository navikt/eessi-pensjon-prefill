package no.nav.eessi.pensjon.prefill.person

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.personoppslag.pdl.model.AdressebeskyttelseGradering
import no.nav.eessi.pensjon.personoppslag.pdl.model.Bostedsadresse
import no.nav.eessi.pensjon.personoppslag.pdl.model.Folkeregistermetadata
import no.nav.eessi.pensjon.personoppslag.pdl.model.Kontaktadresse
import no.nav.eessi.pensjon.personoppslag.pdl.model.KontaktadresseType
import no.nav.eessi.pensjon.personoppslag.pdl.model.KontaktinformasjonForDoedsbo
import no.nav.eessi.pensjon.personoppslag.pdl.model.KontaktinformasjonForDoedsboAdresse
import no.nav.eessi.pensjon.personoppslag.pdl.model.KontaktinformasjonForDoedsboSkifteform
import no.nav.eessi.pensjon.personoppslag.pdl.model.Person
import no.nav.eessi.pensjon.personoppslag.pdl.model.PostadresseIFrittFormat
import no.nav.eessi.pensjon.personoppslag.pdl.model.UtenlandskAdresse
import no.nav.eessi.pensjon.personoppslag.pdl.model.UtenlandskAdresseIFrittFormat
import no.nav.eessi.pensjon.personoppslag.pdl.model.Vegadresse
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.PersonPDLMock.medBeskyttelse
import no.nav.eessi.pensjon.services.geo.PostnummerService
import no.nav.eessi.pensjon.services.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.toJson
import no.nav.eessi.pensjon.utils.typeRefs
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

class PrefillPDLAdresseTest{

    private lateinit var prefillAdresse: PrefillPDLAdresse
    private var kodeverkClient: KodeverkClient = mockk()

    private val deugLogger: Logger = LoggerFactory.getLogger("no.nav.eessi.pensjon") as Logger
    private val listAppender = ListAppender<ILoggingEvent>()

    @BeforeEach
    fun beforeStart() {
        deugLogger.addAppender(listAppender)
        listAppender.start()
        prefillAdresse = PrefillPDLAdresse(PostnummerService(), kodeverkClient)
    }

    @AfterEach
    fun after() {
        listAppender.stop()
    }


    @Test
    fun `create personAdresse`() {
        val person = PersonPDLMock.createWith()
            .copy(bostedsadresse = Bostedsadresse(
                LocalDateTime.of(2000, 9, 2, 4,3),
                LocalDateTime.of(2300, 9, 2, 4,3),
                Vegadresse(
                    "Kirkeveien",
                    "12",
                    null,
                    "0123",
                    null,
                    null
                ),
                utenlandskAdresse = null,
                metadata = no.nav.eessi.pensjon.personoppslag.pdl.model.Metadata(emptyList(), false, "DOLLY", "Doll")
            ))

        val result = prefillAdresse.createPersonAdresse(person)!!

        assertNotNull(result)
        assertEquals("NO", result.land)
        assertEquals("Kirkeveien 12", result.gate)
        assertEquals("OSLO", result.by)
    }

    @Test
    fun adresseFeltDiskresjonFortroligPerson() {
        val person = PersonPDLMock.createWith()
            .medBeskyttelse(AdressebeskyttelseGradering.FORTROLIG)

        val acual = prefillAdresse.createPersonAdresse(person)

        assertEquals(null, acual)
    }

    @Test
    fun adresseFeltDiskresjonStrengtFortoligPerson() {
        val person = PersonPDLMock.createWith()
            .medBeskyttelse(AdressebeskyttelseGradering.STRENGT_FORTROLIG)

        val acual = prefillAdresse.createPersonAdresse(person)

        assertEquals(null, acual)
    }


    @Test
    fun `utfylling av doedsboAdresseUtenlinjeskift`() {
        every { kodeverkClient.finnLandkode(eq("NOR")) } returns "NO"

        val doedMeta = PersonPDLMock.mockMeta()

        val person = PersonPDLMock.createWith(
            landkoder = true,
            fnr = "123123123",
            aktoerid = "2312312",
            erDod = true
        ).copy(
            bostedsadresse = null,
            oppholdsadresse = null,
            kontaktinformasjonForDoedsbo = KontaktinformasjonForDoedsbo(
                adresse = KontaktinformasjonForDoedsboAdresse(
                    adresselinje1 = "testlinej1 123\n23123 osloby",
                    landkode = "NOR",
                    postnummer = "1231",
                    poststedsnavn = "osloby"
                ),
                LocalDate.of(2020, 10, 1),
                Folkeregistermetadata(null),
                skifteform = KontaktinformasjonForDoedsboSkifteform.ANNET,
                metadata = doedMeta,
            ))


        val actual = prefillAdresse.createPersonAdresse(person)

        assertNotNull(actual)
        assertEquals("testlinej1 123 23123 osloby", actual?.gate)
        assertEquals(null, actual?.bygning)
        assertEquals("osloby", actual?.by)
        assertEquals("1231", actual?.postnummer)

    }

    @Test
    fun `create Adresse med BostedAdresse og adresseIFrittFormat`() {
        val person = PersonPDLMock.createWith()
            .copy(bostedsadresse = Bostedsadresse(
                LocalDateTime.of(2000, 9, 2, 4,3),
                LocalDateTime.of(2300, 9, 2, 4,3),
                Vegadresse(
                    "Kirkeveien",
                    "12",
                    null,
                    "0123",
                    null,
                    null
                ),
                utenlandskAdresse = null,
                metadata = no.nav.eessi.pensjon.personoppslag.pdl.model.Metadata(emptyList(), false, "DOLLY", "Doll")
            ), kontaktadresse = Kontaktadresse(
                coAdressenavn = null,
                type = KontaktadresseType.Innland,
                postadresseIFrittFormat = PostadresseIFrittFormat(
                    adresselinje1 = "Kirkeveien",
                ),
                metadata = no.nav.eessi.pensjon.personoppslag.pdl.model.Metadata(emptyList(), false, "DOLLY", "Doll")
            )
            )

        val result = prefillAdresse.createPersonAdresse(person)!!

        assertEquals("NO", result.land)
        assertEquals("Kirkeveien 12", result.gate)
        assertEquals("0123", result.postnummer)
        assertEquals("OSLO", result.by)
    }

    @Test
    fun `create utenlandsadresse med feil format`() {
        every { kodeverkClient.finnLandkode(any()) }.returns("SC")

        val person = PersonPDLMock.createWith()
            .copy(bostedsadresse = null,
                kontaktadresse = Kontaktadresse(
                    coAdressenavn = null,
                    type = KontaktadresseType.Innland,
                    postadresseIFrittFormat = null,
                    utenlandskAdresse = UtenlandskAdresse(
                        adressenavnNummer = "adressenavnummer",
                        bySted = "bysted",
                        landkode = "SC",
                        postkode = "Edinburg bladi bladi bladi bladi bladi"
                    ),
                    metadata = no.nav.eessi.pensjon.personoppslag.pdl.model.Metadata(emptyList(), false, "DOLLY", "Doll")
                ))

        val result = prefillAdresse.createPersonAdresse(person)!!

        assertEquals("bysted", result.by)
        assertEquals("SC", result.land)
        assertEquals("adressenavnummer", result.gate)

    }

    @Test
    fun `create utenlandsadresse med riktig format`() {
        val person = PersonPDLMock.createWith()
            .copy(bostedsadresse = null,
                kontaktadresse = Kontaktadresse(
                    coAdressenavn = null,
                    type = KontaktadresseType.Innland,
                    postadresseIFrittFormat = null,
                    utenlandskAdresse = UtenlandskAdresse(
                        adressenavnNummer = "adressenavnummer",
                        bySted = "bysted",
                        landkode = "SCT",
                        postkode = "EH99"
                    ),
                    metadata = no.nav.eessi.pensjon.personoppslag.pdl.model.Metadata(emptyList(), false, "DOLLY", "Doll")
                ))

        every { kodeverkClient.finnLandkode(any()) }.returns("SC")

        val result = prefillAdresse.createPersonAdresse(person)!!

        assertEquals("SC", result.land)
        assertEquals("adressenavnummer", result.gate)
        assertEquals("EH99", result.postnummer)
        assertEquals("bysted", result.by)

    }

    @Test
    fun `create tom utenlandsadresse der utenlandsadresse i fritt format er tom eller mangler`() {
        val person = PersonPDLMock.createWith()
            .copy(bostedsadresse = null,
                kontaktadresse = Kontaktadresse(
                    coAdressenavn = null,
                    type = KontaktadresseType.Innland,
                    postadresseIFrittFormat = null,
                    utenlandskAdresseIFrittFormat = UtenlandskAdresseIFrittFormat(),
                    metadata = no.nav.eessi.pensjon.personoppslag.pdl.model.Metadata(emptyList(), false, "DOLLY", "Doll")
                ))

        every { kodeverkClient.finnLandkode(any()) }.returns("SC")

        val result = prefillAdresse.createPersonAdresse(person)!!

        val expected = """
            {
              "gate" : "",
              "bygning" : "",
              "by" : "",
              "postnummer" : "",
              "postkode" : null,
              "region" : null,
              "land" : "",
              "kontaktpersonadresse" : null,
              "datoforadresseendring" : null,
              "postadresse" : null,
              "startdato" : null,
              "type" : null,
              "annen" : null
            }
        """.trimIndent()

        JSONAssert.assertEquals(expected, result.toJson(), true)

    }

    @Test
    fun `Når utenlandskadresseIFrittFormat er null og utenlandskadresse finnes saa skal vi preutfylle med utenlandskadresse`() {
        val person = PersonPDLMock.createWith()
            .copy(
                bostedsadresse = null,
                kontaktadresse = Kontaktadresse(
                    coAdressenavn = null,
                    type = KontaktadresseType.Innland,
                    postadresseIFrittFormat = null,
                    utenlandskAdresse = UtenlandskAdresse(
                        adressenavnNummer = "adressenavnummer",
                        bySted = "bysted",
                        landkode = "SCT",
                        postkode = "EH99"
                    ),
                    utenlandskAdresseIFrittFormat = null,
                    metadata = no.nav.eessi.pensjon.personoppslag.pdl.model.Metadata(
                        emptyList(),
                        false,
                        "DOLLY",
                        "Doll"
                    )
                )
            )

        every { kodeverkClient.finnLandkode(any()) }.returns("SC")

        val result = prefillAdresse.createPersonAdresse(person)!!

        assertEquals("EH99", result.postnummer)
    }

    @Test
    fun `Når utenlandskadresse i bostedsadresse er fylt ut saa skal denne benyttes`() {

        val bostedsadresseJson = """
            {
               "vegadresse":null,
               "utenlandskAdresse":{
                  "adressenavnNummer":"1KOLEJOWA 6/5",
                  "bygningEtasjeLeilighet":"Londonshire",
                  "postboksNummerNavn":null,
                  "postkode":"3000",
                  "bySted":"CAPITAL WEST",
                  "regionDistriktOmraade":"18-500 KOLNO",
                  "landkode":"MCO"
               },
               "gyldigFraOgMed":"1954-06-28T00:00",
               "gyldigTilOgMed":null,
               "metadata":{
                  "endringer":[
                     {
                        "kilde":"Dolly",
                        "registrert":"2022-05-30T09:53:44",
                        "registrertAv":"srvdolly",
                        "systemkilde":"srvdolly",
                        "type":"OPPRETT"
                     }
                  ],           
                  
                  "master":"PDL",
                  "opplysningsId":"c6064616-915f-4cbe-94d3-cec3117c9f1f",
                  "historisk":false
               }
            }
        """.trimIndent()

        val bostedsadresse = mapJsonToAny(bostedsadresseJson, typeRefs<Bostedsadresse>())

        val person = PersonPDLMock.createWith()
            .copy(
                bostedsadresse =  bostedsadresse,
                kontaktadresse = null
            )

        every { kodeverkClient.finnLandkode(any()) }.returns("SC")

        val result = prefillAdresse.createPersonAdresse(person)!!
        assertEquals("CAPITAL WEST", result.by)
        assertEquals("3000", result.postnummer)
        assertEquals("SC", result.land)
        assertEquals("18-500 KOLNO", result.region)
        assertEquals("Londonshire", result.bygning)
    }

    @Test
    fun `Når vegadresse i bostedsadresse er fylt ut saa skal denne benyttes`() {

        val bostedsadresseJson = """
            {
               "vegadresse":{
                  "adressenavn":"1KOLEJOWA 6/5",
                  "husnummer":"",
                  "postnummer":"0301",
                  "bydelsnummer":"CAPITAL WEST",
                  "kommunenummer":"18-500 KOLNO"
               },
               "utenlandskAdresse":null,
               "gyldigFraOgMed":"1954-06-28T00:00",
               "gyldigTilOgMed":null,
               "metadata":{
                  "endringer":[
                     {
                        "kilde":"Dolly",
                        "registrert":"2022-05-30T09:53:44",
                        "registrertAv":"srvdolly",
                        "systemkilde":"srvdolly",
                        "type":"OPPRETT"
                     }
                  ],           
                  
                  "master":"PDL",
                  "opplysningsId":"c6064616-915f-4cbe-94d3-cec3117c9f1f",
                  "historisk":false
               }
            }
        """.trimIndent()

        val bostedsadresse = mapJsonToAny(bostedsadresseJson, typeRefs<Bostedsadresse>())

        val person = PersonPDLMock.createWith()
            .copy(
                bostedsadresse =  bostedsadresse,
                kontaktadresse = null
            )

        every { kodeverkClient.finnLandkode(any()) }.returns("SC")

        val result = prefillAdresse.createPersonAdresse(person)!!
        assertEquals("OSLO", result.by)
        assertEquals("0301", result.postnummer)
    }


    @Test
    fun `Det skal gies warning naar pdlPerson har flere enn gyldige adresser`() {
        val mockPerson = mockk<Person>(relaxed = true)
        val bostedsadresse = mockk<Bostedsadresse>(relaxed = true)
        every { mockPerson.bostedsadresse } returns bostedsadresse

        prefillAdresse.createPersonAdresse(mockPerson)!!

        assertNotNull(getLogMsg("Fant flere gyldig adresser: "))
    }

    private fun getLogMsg(logMsg: String) : String? {
        val logsList: List<ILoggingEvent> = listAppender.list
        return  logsList.find { message -> message.formattedMessage.contains(logMsg)}?.message
    }
}
