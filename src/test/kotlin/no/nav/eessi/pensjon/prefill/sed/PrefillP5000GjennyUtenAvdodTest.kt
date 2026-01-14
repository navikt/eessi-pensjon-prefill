package no.nav.eessi.pensjon.prefill.sed

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.BucType.P_BUC_02
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.SedType.*
import no.nav.eessi.pensjon.eux.model.sed.P5000
import no.nav.eessi.pensjon.eux.model.sed.P6000
import no.nav.eessi.pensjon.eux.model.sed.P8000
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.model.AktoerId
import no.nav.eessi.pensjon.prefill.*
import no.nav.eessi.pensjon.prefill.etterlatte.EtterlatteService
import no.nav.eessi.pensjon.prefill.models.DigitalKontaktinfo
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.shared.api.ApiRequest
import no.nav.eessi.pensjon.shared.api.InstitusjonItem
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.statistikk.AutomatiseringStatistikkService
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.toJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

private const val AKTOERID = "0105094340092"

class PrefillP5000GjennyUtenAvdodTest {

//    private val personFnr = FodselsnummerGenerator.generateFnrForTest(65)
    private val personFnr = "04016143397"
    private val pesysSaksnummer = "21975717"

    lateinit var prefillData: PrefillDataModel

    lateinit var prefill: PrefillP5000
    lateinit var prefillNav: PrefillPDLNav

    lateinit var innhentingService: InnhentingService
    lateinit var etterlatteService: EtterlatteService
    lateinit var personDataService : PersonDataService
    lateinit var prefillGjennyService: PrefillGjennyService

    var eessiInformasjon = mockk<EessiInformasjon>(relaxed = true)
    var krrService = mockk<KrrService>(relaxed = true)
    var automatiseringStatistikkService = mockk<AutomatiseringStatistikkService>()
    var personservice = mockk<PersonService>()

    @BeforeEach
    fun setup() {
        prefillNav = BasePrefillNav.createPrefillNav()
        personDataService = PersonDataService(personservice)
        etterlatteService = EtterlatteService(mockk())
        innhentingService = InnhentingService(personDataService, pensjonsinformasjonService = mockk())
        prefillGjennyService = PrefillGjennyService(krrService, innhentingService, etterlatteService, automatiseringStatistikkService, prefillNav, eessiInformasjon)

        every { personservice.hentIdent(any(), any()) } returns AktoerId(personFnr)
        every { personservice.hentPerson(any()) } returns PersonPDLMock.createWith(true, "Avdød", "Død", personFnr, AKTOERID, true)
        justRun { automatiseringStatistikkService.genererAutomatiseringStatistikk(any(), any()) }
        every { krrService.hentPersonerFraKrr(eq(personFnr), any()) } returns DigitalKontaktinfo(
            aktiv = true,
            personident = personFnr
        )
    }

    @Disabled
    @Test
    fun `Forventer korrekt preutfylt P5000 med gjenlevende uten kjent avdod for gjenny`() {
        prefillData = PrefillDataModelMother.initialPrefillDataModel(
            P5000,
            personFnr,
            penSaksnummer = pesysSaksnummer,
            avdod = PersonInfo(null, null)
        )

        val apiReq = apiRequest(P5000)

        val p5000 = mapJsonToAny<P5000>(prefillGjennyService.prefillGjennySedtoJson(apiReq))

        assertEquals(personFnr, p5000.pensjon?.gjenlevende?.person?.pin?.firstOrNull()?.identifikator)
        assertEquals(null, p5000.nav?.bruker?.person?.fornavn)
        assertEquals(null, p5000.nav?.bruker?.person?.etternavn)


    }

    @Disabled
    @Test
    fun `Forventer korrekt utfylt P6000 med gjenlevende med avdod for gjenny`() {
        prefillData = PrefillDataModelMother.initialPrefillDataModel(
            P6000,
            personFnr,
            penSaksnummer = pesysSaksnummer,
            avdod = PersonInfo(null, null)
        )

        val apiReq = apiRequest(P6000)

        val p6000 = mapJsonToAny<P6000>(prefillGjennyService.prefillGjennySedtoJson(apiReq))

        assertEquals(null, p6000.pensjon?.gjenlevende?.person?.pin?.firstOrNull()?.identifikator)
        assertEquals(null, p6000.pensjon?.gjenlevende?.person?.pin?.get(1)?.identifikator)
//        assertEquals("BEVISST", p6000.pensjon?.gjenlevende?.person?.fornavn)
//        assertEquals("GAUPE", p6000.pensjon?.gjenlevende?.person?.etternavn)
        assertEquals("Avdød", p6000.nav?.bruker?.person?.fornavn)
        assertEquals(null, p6000.nav?.bruker?.person?.etternavn)

    }

    @Test
    fun `Forventer korrekt utfylt P6000 med gjenlevende uten avdod for gjenny`() {
        prefillData = PrefillDataModelMother.initialPrefillDataModel(
            P6000,
            personFnr,
            penSaksnummer = pesysSaksnummer,
            avdod = PersonInfo(null, null)
        )

        val apiReq = apiRequest(P6000)
        val p6000 = mapJsonToAny<P6000>(prefillGjennyService.prefillGjennySedtoJson(apiReq))

        assertEquals(personFnr, p6000.pensjon?.gjenlevende?.person?.pin?.firstOrNull()?.identifikator)
        assertEquals("Avdød", p6000.pensjon?.gjenlevende?.person?.fornavn)
        assertEquals("Død", p6000.pensjon?.gjenlevende?.person?.etternavn)
        assertEquals(null, p6000.nav?.bruker?.person?.fornavn)
        assertEquals(null, p6000.nav?.bruker?.person?.etternavn)
    }

    @Disabled
    @Test
    fun `Forventer korrekt preutfylt P8000 med gjenlevende uten avdod for gjenny`() {
        val apiReq = apiRequest(P8000)

        val p8000 = mapJsonToAny<P8000>(prefillGjennyService.prefillGjennySedtoJson(apiReq))
        println("@@@P8000: ${p8000.toJson()}")

        assertEquals(personFnr, p8000.pensjon?.gjenlevende?.person?.pin?.firstOrNull()?.identifikator)
        assertEquals(null, p8000.nav?.bruker?.person?.fornavn)
        assertEquals(null, p8000.nav?.bruker?.person?.etternavn)

    }

    private fun apiRequest(sedType: SedType): ApiRequest = ApiRequest(
        subjectArea = "Pensjon",
        sakId = pesysSaksnummer,
        institutions = listOf(InstitusjonItem("NO", "Institutt", "InstNavn")),
        euxCaseId = "123456",
        sed = sedType,
        buc = P_BUC_02,
        aktoerId = AKTOERID,
        avdodfnr = null,
        gjenny = true

    )

    fun personDataCollection(): String{
        return """
        {
          "gjenlevendeEllerAvdod": {
            "identer": [
              { "ident": "$personFnr", "gruppe": "FOLKEREGISTERIDENT" },
              { "ident": "2949022090048", "gruppe": "AKTORID" }
            ],
            "navn": {
              "fornavn": "BEVISST",
              "mellomnavn": null,
              "etternavn": "GAUPE",
              "forkortetNavn": null,
              "gyldigFraOgMed": "2014-08-19",
              "folkeregistermetadata": {
                "gyldighetstidspunkt": "2014-08-19T00:00",
                "ajourholdstidspunkt": null,
                "opphoerstidspunkt": null,
                "kilde": null,
                "aarsak": null,
                "sekvens": null
              },
              "metadata": {
                "endringer": [
                  {
                    "kilde": "Dolly",
                    "registrert": "2026-01-05T12:20",
                    "registrertAv": "Folkeregisteret",
                    "systemkilde": "FREG",
                    "type": "OPPRETT",
                    "hendelseId": null
                  }
                ],
                "historisk": false,
                "master": "FREG",
                "opplysningsId": "c70eec05-891e-476d-9835-2ebe77da9151"
              }
            },
            "adressebeskyttelse": [],
            "bostedsadresse": {
              "gyldigFraOgMed": "2014-08-19T00:00",
              "gyldigTilOgMed": null,
              "vegadresse": null,
              "utenlandskAdresse": {
                "adressenavnNummer": "1KOLEJOWA 6/5",
                "bySted": "CAPITAL WEST",
                "bygningEtasjeLeilighet": "",
                "landkode": "HRV",
                "postboksNummerNavn": null,
                "postkode": "3000",
                "regionDistriktOmraade": "18-500 KOLNO"
              },
              "metadata": {
                "endringer": [
                  {
                    "kilde": "Dolly",
                    "registrert": "2026-01-05T12:20",
                    "registrertAv": "dev-fss:dolly:testnav-pdl-proxy-trygdeetaten",
                    "systemkilde": "dev-fss:dolly:testnav-pdl-proxy-trygdeetaten",
                    "type": "OPPRETT",
                    "hendelseId": null
                  }
                ],
                "historisk": false,
                "master": "PDL",
                "opplysningsId": "65915176-85cd-4e59-a4d7-5bb49ee68a92"
              }
            },
            "oppholdsadresse": null,
            "statsborgerskap": [
              {
                "land": "GRD",
                "gyldigFraOgMed": null,
                "gyldigTilOgMed": null,
                "metadata": {
                  "endringer": [
                    {
                      "kilde": "Dolly",
                      "registrert": "2026-01-05T12:20",
                      "registrertAv": "Folkeregisteret",
                      "systemkilde": "FREG",
                      "type": "OPPRETT",
                      "hendelseId": null
                    }
                  ],
                  "historisk": false,
                  "master": "FREG",
                  "opplysningsId": "80348298-f9eb-423c-85bb-ea3312ff7f5f"
                }
              }
            ],
            "foedselsdato": {
              "foedselsaar": 2014,
              "foedselsdato": "2014-08-19",
              "folkeregistermetadata": {
                "gyldighetstidspunkt": "2014-08-19T00:00",
                "ajourholdstidspunkt": null,
                "opphoerstidspunkt": null,
                "kilde": null,
                "aarsak": null,
                "sekvens": null
              },
              "metadata": {
                "endringer": [
                  {
                    "kilde": "Dolly",
                    "registrert": "2026-01-05T12:20",
                    "registrertAv": "Folkeregisteret",
                    "systemkilde": "FREG",
                    "type": "OPPRETT",
                    "hendelseId": null
                  }
                ],
                "historisk": false,
                "master": "FREG",
                "opplysningsId": "5ec1b4e9-6ba4-4250-bfb3-fbccf095b415"
              }
            },
            "foedested": {
              "foedeland": "HRV",
              "foedested": null,
              "foedekommune": null,
              "folkeregistermetadata": {
                "gyldighetstidspunkt": "2026-01-05T12:19:01",
                "ajourholdstidspunkt": null,
                "opphoerstidspunkt": null,
                "kilde": null,
                "aarsak": null,
                "sekvens": null
              },
              "metadata": {
                "endringer": [
                  {
                    "kilde": "Dolly",
                    "registrert": "2026-01-05T12:20",
                    "registrertAv": "Folkeregisteret",
                    "systemkilde": "FREG",
                    "type": "OPPRETT",
                    "hendelseId": null
                  }
                ],
                "historisk": false,
                "master": "FREG",
                "opplysningsId": "1f614957-3495-4531-8c8e-a1e99d5f2d4a"
              }
            },
            "geografiskTilknytning": {
              "gtType": "UTLAND",
              "gtKommune": null,
              "gtBydel": null,
              "gtLand": "HRV"
            },
            "kjoenn": {
              "kjoenn": "MANN",
              "folkeregistermetadata": {
                "gyldighetstidspunkt": "2026-01-05T11:20",
                "ajourholdstidspunkt": null,
                "opphoerstidspunkt": null,
                "kilde": null,
                "aarsak": null,
                "sekvens": null
              },
              "metadata": {
                "endringer": [
                  {
                    "kilde": "Dolly",
                    "registrert": "2026-01-05T12:20",
                    "registrertAv": "Folkeregisteret",
                    "systemkilde": "FREG",
                    "type": "OPPRETT",
                    "hendelseId": null
                  }
                ],
                "historisk": false,
                "master": "FREG",
                "opplysningsId": "44430d95-5f3f-439a-b367-3e3b1ff732c8"
              }
            },
            "doedsfall": null,
            "forelderBarnRelasjon": [],
            "sivilstand": [
              {
                "type": "UGIFT",
                "gyldigFraOgMed": "2014-08-19",
                "relatertVedSivilstand": null,
                "metadata": {
                  "endringer": [
                    {
                      "kilde": "Dolly",
                      "registrert": "2026-01-05T12:20",
                      "registrertAv": "Folkeregisteret",
                      "systemkilde": "FREG",
                      "type": "OPPRETT",
                      "hendelseId": null
                    }
                  ],
                  "historisk": false,
                  "master": "FREG",
                  "opplysningsId": "abe1d09f-374d-41d1-9df0-3b18548ea8f2"
                }
              }
            ],
            "kontaktadresse": null,
            "kontaktinformasjonForDoedsbo": null,
            "utenlandskIdentifikasjonsnummer": []
          },
          "forsikretPerson": null,
          "ektefellePerson": null,
          "barnPersonList": []
        }           
        """.trimIndent()
    }
}

