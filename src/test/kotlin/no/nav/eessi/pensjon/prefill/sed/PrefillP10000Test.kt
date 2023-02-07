package no.nav.eessi.pensjon.prefill.sed

import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.personoppslag.FodselsnummerGenerator
import no.nav.eessi.pensjon.prefill.PersonPDLMock
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModelMother
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.shared.api.PersonId
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PrefillP10000Test {
    private val personFnr = FodselsnummerGenerator.generateFnrForTest(68)
    private val ekteFnr = FodselsnummerGenerator.generateFnrForTest(70)
    private val pesysSaksnummer = "14398627"
    lateinit var prefillData: PrefillDataModel
    lateinit var prefill: PrefillP10000
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

        prefill = PrefillP10000(prefillNav)
        prefillData = PrefillDataModelMother.initialPrefillDataModel(
            SedType.P8000,
            personFnr,
            penSaksnummer = pesysSaksnummer,
            avdod = PersonId("12345678910", "123456789")
        )

    }

    @Test
    fun `Gitt en preutfylt P10 000 så fyll ut annen person dersom den finnes`() {
        val p10000 = prefill.prefill(
            prefillData.penSaksnummer,
            prefillData.bruker,
            prefillData.avdod,
            prefillData.getBankOgArbeidFromRequest(),
            persondataCollection)

        assertNotNull(p10000.nav!!.annenperson)
    }
}

