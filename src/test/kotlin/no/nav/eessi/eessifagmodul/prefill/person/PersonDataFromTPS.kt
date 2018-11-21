package no.nav.eessi.eessifagmodul.prefill.person

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.whenever
import no.nav.eessi.eessifagmodul.models.InstitusjonItem
import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.prefill.PrefillDataModel
import no.nav.eessi.eessifagmodul.prefill.nav.PrefillNav
import no.nav.eessi.eessifagmodul.prefill.nav.PrefillPersonDataFromTPS
import no.nav.eessi.eessifagmodul.services.LandkodeService
import no.nav.eessi.eessifagmodul.services.PostnummerService
import no.nav.eessi.eessifagmodul.services.personv3.PersonV3Service
import no.nav.eessi.eessifagmodul.utils.mapJsonToAny
import no.nav.eessi.eessifagmodul.utils.typeRefs
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.util.ResourceUtils

@RunWith(MockitoJUnitRunner::class)
abstract class PersonDataFromTPS(private val mocktps: Set<MockTPS>) {

    @Mock
    lateinit var mockPersonV3Service: PersonV3Service

    protected lateinit var preutfyllingTPS: PrefillPersonDataFromTPS

    @Mock
    protected lateinit var prefillNav: PrefillNav

    @Before
    fun setup() {
        preutfyllingTPS = mockPrefillPersonDataFromTPS()
        prefillNav = PrefillNav(preutfyllingTPS)

        prefillNav.institutionid = "NO:noinst002"
        prefillNav.institutionnavn = "NOINST002, NO INST002, NO"
    }


    private fun initMockHentPersonResponse(mockFile: String): HentPersonResponse {
        val resource = ResourceUtils.getFile("classpath:personv3/$mockFile").readText()
        val mapper = jacksonObjectMapper()
        val rootNode = mapper.readValue(resource, JsonNode::class.java)

        val bostedsadresse = rootNode["bostedsadresse"]
        val sivilstand = rootNode["sivilstand"]
        val personnavn = rootNode["personnavn"]
        val personstatus = rootNode["personstatus"]
        val statsborgerskap = rootNode["statsborgerskap"]
        val kjoenn = rootNode["kjoenn"]
        val aktoer = rootNode["aktoer"]
        val strukturertAdresse = bostedsadresse["strukturertAdresse"]
        val poststed = strukturertAdresse["poststed"]
        val landkode = strukturertAdresse["landkode"]
        val harFraRolleI = rootNode["harFraRolleI"]

        val v3struktAdr = mapJsonToAny(bostedsadresse.toString(), typeRefs<Gateadresse>())
        val v3personstatus = mapJsonToAny(personstatus.toString(), typeRefs<Personstatus>())
        val v3statsborgerskap = mapJsonToAny(statsborgerskap.toString(), typeRefs<Statsborgerskap>())
        val v3pernavn = mapJsonToAny(personnavn.toString(), typeRefs<Personnavn>())
        val v3sivilstand = mapJsonToAny(sivilstand.toString(), typeRefs<Sivilstand>())
        val v3kjoenn = mapJsonToAny(kjoenn.toString(), typeRefs<Kjoenn>())
        val v3aktoer = mapJsonToAny(aktoer.toString(), typeRefs<PersonIdent>())
        val v3poststed = mapJsonToAny(poststed.toString(), typeRefs<Postnummer>())
        val v3landkode = mapJsonToAny(landkode.toString(), typeRefs<Landkoder>())

        val v3person = Bruker()

        harFraRolleI.forEach {
            val v3familieRelasjon = Familierelasjon()

            val tilRolle = it["tilRolle"]

            val tilPerson = it["tilPerson"]
            val kjoennitem = tilPerson["kjoenn"]
            val aktoeritem = tilPerson["aktoer"]
            val personnavnitem = tilPerson["personnavn"]

            v3familieRelasjon.tilRolle = mapJsonToAny(tilRolle.toString(), typeRefs<Familierelasjoner>())
            v3familieRelasjon.tilPerson = Bruker()
            if (!kjoennitem.isNull) {
                v3familieRelasjon.tilPerson.kjoenn = mapJsonToAny(kjoennitem.toString(), typeRefs<Kjoenn>())
            }
            v3familieRelasjon.tilPerson.aktoer = mapJsonToAny(aktoeritem.toString(), typeRefs<PersonIdent>())
            v3familieRelasjon.tilPerson.personnavn = mapJsonToAny(personnavnitem.toString(), typeRefs<Personnavn>())

            v3person.harFraRolleI.add(v3familieRelasjon)
        }

        v3person.bostedsadresse = Bostedsadresse()
        v3person.bostedsadresse.strukturertAdresse = v3struktAdr // Gateadresse
        v3struktAdr.poststed = v3poststed
        v3struktAdr.landkode = v3landkode
        v3person.sivilstand = v3sivilstand // v3sivilstand
        v3person.personnavn = v3pernavn // v3pernavn
        v3person.personstatus = v3personstatus
        v3person.statsborgerskap = v3statsborgerskap
        v3person.kjoenn = v3kjoenn
        v3person.aktoer = v3aktoer

        val v3PersonResponse = HentPersonResponse()
        v3PersonResponse.person = v3person

        return v3PersonResponse
    }

    fun mockPrefillPersonDataFromTPS(): PrefillPersonDataFromTPS {
        mocktps.forEach {
            whenever(mockPersonV3Service.hentPerson(it.mockPin)).thenReturn(initMockHentPersonResponse(it.mockFile))
        }
        return PrefillPersonDataFromTPS(mockPersonV3Service, PostnummerService(), LandkodeService())
    }

    fun generatePrefillData(sedName: String = "P2000", pinId: String = "123456789"): PrefillDataModel {
        val items = listOf(InstitusjonItem(country = "NO", institution = "DUMMY"))
        return PrefillDataModel().apply {
            rinaSubject = "Pensjon"
            sed = SED.create(sedName)
            penSaksnummer = "12345"
            //vedtakId = "12312312"
            vedtakId = ""
            buc = "P_BUC_99"
            aktoerID = "123456789"
            personNr = pinId
            institution = items
        }
    }

    data class MockTPS(
            val mockFile: String,
            val mockPin: String
    )

}