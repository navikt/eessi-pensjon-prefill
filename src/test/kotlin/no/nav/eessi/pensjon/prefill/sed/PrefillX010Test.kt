package no.nav.eessi.pensjon.prefill.sed

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.sed.SedType
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PersonId
import no.nav.eessi.pensjon.prefill.models.PrefillDataModel
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.person.FodselsnummerMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

class PrefillX010Test {
    private val personFnr = FodselsnummerMother.generateRandomFnr(68)
    private val ekteFnr = FodselsnummerMother.generateRandomFnr(70)
    private val pesysSaksnummer = "14398627"
    lateinit var prefillData: PrefillDataModel
    lateinit var prefill: PrefillX010
    lateinit var prefillNav: PrefillPDLNav
    lateinit var persondataCollection: PersonDataCollection

    @BeforeEach
    fun setup() {
        persondataCollection = PersonPDLMock.createEnkelFamilie(personFnr, ekteFnr)

        prefillNav = PrefillPDLNav(
            prefillAdresse = mockk(){
                every { hentLandkode(any()) } returns "NO"
                every { createPersonAdresse(any()) } returns mockk(relaxed = true)
            },
            institutionid = "NO:noinst002",
            institutionnavn = "NOINST002, NO INST002, NO")

        prefill = PrefillX010(prefillNav)
        prefillData = PrefillDataModelMother.initialPrefillDataModel(
            SedType.X010,
            personFnr,
            penSaksnummer = pesysSaksnummer,
            avdod = PersonId("12345678910", "123456789")
        )

    }


    @Test
    fun prefillWorkingX010asValidJson() {
        val x010sed = prefill.prefill(
            prefillData.penSaksnummer,
            prefillData.bruker,
            null,
            prefillData.getPersonInfoFromRequestData(),
            persondataCollection
        )

        val json = x010sed.toJsonSkipEmpty()
        println(json)

        JSONAssert.assertEquals(expectedSed(), json , true)
    }

    private fun expectedSed(): String {
        return """
            {
              "sed" : "X010",
              "sedGVer" : "4",
              "sedVer" : "2",
              "nav" : {
                "sak" : {
                  "kontekst" : {
                    "bruker" : {
                      "person" : {
                        "etternavn" : "BALDER",
                        "fornavn" : "ODIN ETTØYE",
                        "kjoenn" : "M",
                        "foedselsdato" : "1988-07-12"
                      }
                    }
                  },
                  "paaminnelse" : {
                    "svar" : {
                      "informasjon" : { }
                    }
                  }
                }
              }
            }
        """.trimIndent()
    }

}