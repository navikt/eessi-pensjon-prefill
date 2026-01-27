package no.nav.eessi.pensjon.prefill.sed

import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.eux.model.sed.X009
import no.nav.eessi.pensjon.prefill.BasePrefillNav
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

class PrefillX010Test {
    private val personFnr = FodselsnummerGenerator.generateFnrForTest(68)
    private val ekteFnr = FodselsnummerGenerator.generateFnrForTest(70)
    lateinit var prefillData: PrefillDataModel
    lateinit var prefill: PrefillX010
    lateinit var prefillNav: PrefillPDLNav
    lateinit var persondataCollection: PersonDataCollection

    @BeforeEach
    fun setup() {
        persondataCollection = PersonPDLMock.createEnkelFamilie(personFnr, ekteFnr)
        prefillNav = BasePrefillNav.createPrefillNav()
        prefill = PrefillX010(prefillNav)

        prefillData = PrefillDataModelMother.initialPrefillDataModel(
            SedType.X010,
            personFnr,
            penSaksnummer = "14398627",
            avdod = PersonInfo("12345678910", "123456789")
        )

    }


    @Test
    fun `Prefill X010 med data fra X009 med flere detaljer også mangelfull`() {
//        val x009 = SED.fromJsonToConcrete(PrefillTestHelper.readJsonResponse("/json/nav/X009-NAV.json")) as X009

        val x010sed = prefill.prefill(
            prefillData.penSaksnummer,
            prefillData.bruker,
            null,
            prefillData.getBankOgArbeidFromRequest(),
            persondataCollection,
//            x009
        )
        val json = x010sed.toJsonSkipEmpty()

        JSONAssert.assertEquals(expectedX010medfleredetaljer(), json , true)
    }

    @Test
    fun `Prefill X010 med data fra X009 hvor det er mangelfull detaljer`() {
//        val x009 = SED.fromJsonToConcrete(PrefillTestHelper.readJsonResponse("/json/nav/X009-TOM-NAV.json")) as X009

        val x010sed = prefill.prefill(
            prefillData.penSaksnummer,
            prefillData.bruker,
            null,
            prefillData.getBankOgArbeidFromRequest(),
            persondataCollection,
//            x009
        )

        val json = x010sed.toJsonSkipEmpty()

        JSONAssert.assertEquals(expectedX010MedmangelfullX009(), json , true)
    }

    private fun expectedX010MedmangelfullX009(): String {
        return """
            {
              "sed" : "X010",
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
                      "informasjon" : {
                        "ikketilgjengelig" : [ {
                          "type" : "sed",
                          "opplysninger" : "Missing details",
                          "grunn" : {
                            "type" : "annet",
                            "annet" : "Missing details"
                          }
                        } ]
                      }
                    }
                  }
                }
              },
              "sedGVer" : "4",
              "sedVer" : "2"
            }
        """.trimIndent()
    }

    private fun expectedX010medfleredetaljer(): String {
    return """
        {
          "sed" : "X010",
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
                  "informasjon" : {
                    "ikketilgjengelig" : [ {
                      "type" : "sed",
                      "opplysninger" : "Missing details",
                      "grunn" : {
                        "type" : "annet",
                        "annet" : "Missing details"
                      }
                    } ],
                    "kommersenere" : [ {
                      "type" : "dokument",
                      "opplysninger" : "æøå"
                    }, {
                      "type" : "sed",
                      "opplysninger" : "P5000"
                    } ]
                  }
                }
              }
            }
          },
          "sedGVer" : "4",
          "sedVer" : "2"
        }
            """.trimIndent()
    }

}

