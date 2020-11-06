package no.nav.eessi.pensjon.fagmodul.prefill.sed

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import no.nav.eessi.pensjon.fagmodul.prefill.LagTPSPerson.Companion.lagTPSBruker
import no.nav.eessi.pensjon.fagmodul.prefill.LagTPSPerson.Companion.medAdresse
import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.model.PersonData
import no.nav.eessi.pensjon.fagmodul.prefill.model.PersonId
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModelMother
import no.nav.eessi.pensjon.fagmodul.prefill.model.ReferanseTilPerson
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonService
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillSed
import no.nav.eessi.pensjon.fagmodul.prefill.sed.krav.KravArsak
import no.nav.eessi.pensjon.fagmodul.prefill.tps.FodselsnummerMother.generateRandomFnr
import no.nav.eessi.pensjon.fagmodul.prefill.tps.PrefillAdresse
import no.nav.eessi.pensjon.personoppslag.aktoerregister.AktoerregisterService
import no.nav.eessi.pensjon.personoppslag.personv3.PersonV3Service
import no.nav.eessi.pensjon.services.geo.PostnummerService
import no.nav.eessi.pensjon.services.kodeverk.KodeverkClient
import no.nav.eessi.pensjon.utils.convertToXMLocal
import no.nav.pensjon.v1.kravhistorikk.V1KravHistorikk
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Doedsdato
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate


@ExtendWith(MockitoExtension::class)
class PrefillP8000P_BUC_05Test {

    private val personFnr = generateRandomFnr(68)
    private val pesysSaksnummer = "14398627"

    lateinit var prefillData: PrefillDataModel

    @Mock
    lateinit var personV3Service: PersonV3Service

    lateinit var prefill: PrefillP8000
    lateinit var prefillNav: PrefillNav
    lateinit var personData: PersonData

    @Mock
    lateinit var pensjoninformasjonservice: PensjonsinformasjonService

    @Mock
    lateinit var aktorRegisterService: AktoerregisterService

    @Mock
    lateinit var kodeverkClient: KodeverkClient

    lateinit var prefillAdresse: PrefillAdresse
    lateinit var prefillSEDService: PrefillSEDService

    @BeforeEach
    fun setup() {

        prefillAdresse = PrefillAdresse(PostnummerService(), kodeverkClient)
        prefillNav = PrefillNav(
                prefillAdresse = prefillAdresse,
                institutionid = "NO:noinst002",
                institutionnavn = "NOINST002, NO INST002, NO")

        val prefillSed = PrefillSed(prefillNav, null)


        prefillSEDService = PrefillSEDService(prefillNav, personV3Service, EessiInformasjon(), pensjoninformasjonservice, aktorRegisterService)

        prefill = PrefillP8000(prefillSed)
        prefillData = PrefillDataModelMother.initialPrefillDataModel("P8000", personFnr, penSaksnummer = pesysSaksnummer)

    }

    @Test
    fun `Forventer korrekt utfylt P8000 med adresse`() {
        val fnr = generateRandomFnr(68)
        val forsikretPerson = lagTPSBruker(fnr, "Christopher", "Robin")
                .medAdresse("Gate",  "SWE")

        personData = PersonData(forsikretPerson = forsikretPerson, ekteTypeValue = "", ektefelleBruker = null, gjenlevendeEllerAvdod = forsikretPerson, barnBrukereFraTPS = listOf())

        doReturn("NO").whenever(kodeverkClient).finnLandkode2("NOR")
        doReturn("SE").whenever(kodeverkClient).finnLandkode2("SWE")

        val sed = prefill.prefill(prefillData, personData,null)

        assertEquals("Christopher", sed.nav?.bruker?.person?.fornavn)
        assertEquals("Gate 12", sed.nav?.bruker?.adresse?.gate)
        assertEquals("SE", sed.nav?.bruker?.adresse?.land)
        assertEquals(pesysSaksnummer, sed.nav?.eessisak?.firstOrNull()?.saksnummer)
        assertEquals("Robin", sed.nav?.bruker?.person?.etternavn)
        assertEquals(fnr, sed.nav?.bruker?.person?.pin?.firstOrNull()?.identifikator)

    }

    @Test
    fun `Forventerkorrekt utfylt P8000 hvor det finnes en sak i Pesys som er gjenlevendepensjon eller barnepensjon - henvendelse gjelder avdøde`() {
        val fnr = generateRandomFnr(40)
        val forsikretPerson = lagTPSBruker(fnr, "Christopher", "Robin")
                .medAdresse("Gate",  "SWE")

        val avdodFnr = generateRandomFnr(93)
        val avdod = lagTPSBruker(avdodFnr, "Winnie", "Pooh")
        avdod.doedsdato = Doedsdato().withDoedsdato(convertToXMLocal(LocalDate.now()))


        doReturn(forsikretPerson).`when`(personV3Service).hentBruker(fnr)
        doReturn(avdod).`when`(personV3Service).hentBruker(avdodFnr)

        doReturn("NO").whenever(kodeverkClient).finnLandkode2("NOR")
        doReturn("SE").whenever(kodeverkClient).finnLandkode2("SWE")

        prefillData = PrefillDataModelMother.initialPrefillDataModel("P8000", fnr, penSaksnummer = pesysSaksnummer, avdod = PersonId(norskIdent = avdodFnr, aktorId = "21323"),  refTilPerson = ReferanseTilPerson.AVDOD)

        val sed =  prefillSEDService.prefill(prefillData)

        //daua person
        assertEquals("Winnie", sed.nav?.bruker?.person?.fornavn)
        assertEquals("Pooh", sed.nav?.bruker?.person?.etternavn)

        //levende person
        assertEquals("Christopher", sed.nav?.annenperson?.person?.fornavn)
        assertEquals("Robin", sed.nav?.annenperson?.person?.etternavn)
        assertEquals("01", sed.nav?.annenperson?.person?.rolle)
        assertEquals("01",  sed.pensjon?.anmodning?.referanseTilPerson)

    }

    @Test
    fun `Forventer korrekt utfylt P8000 hvor det finnes en sak i Pesys som er gjenlevendepensjon eller barnepensjon - henvendelse gjelder gjenlevende-søker`() {
        val fnr = generateRandomFnr(40)
        val forsikretPerson = lagTPSBruker(fnr, "Christopher", "Robin")
                .medAdresse("Gate",  "SWE")

        val avdodFnr = generateRandomFnr(93)
        val avdod = lagTPSBruker(avdodFnr, "Winnie", "Pooh")
        avdod.doedsdato = Doedsdato().withDoedsdato(convertToXMLocal(LocalDate.now()))

        doReturn(forsikretPerson).`when`(personV3Service).hentBruker(fnr)
        doReturn(avdod).`when`(personV3Service).hentBruker(avdodFnr)

        doReturn("NO").whenever(kodeverkClient).finnLandkode2("NOR")
        doReturn("SE").whenever(kodeverkClient).finnLandkode2("SWE")

        prefillData = PrefillDataModelMother.initialPrefillDataModel("P8000", fnr, penSaksnummer = pesysSaksnummer, avdod = PersonId(norskIdent = avdodFnr, aktorId = "21323"), refTilPerson = ReferanseTilPerson.SOKER )

        val sed =  prefillSEDService.prefill(prefillData)

        //daua person
        assertEquals("Winnie", sed.nav?.bruker?.person?.fornavn)
        assertEquals("Pooh", sed.nav?.bruker?.person?.etternavn)

        //levende person
        assertEquals("Christopher", sed.nav?.annenperson?.person?.fornavn)
        assertEquals("Robin", sed.nav?.annenperson?.person?.etternavn)
        assertEquals("01", sed.nav?.annenperson?.person?.rolle)

        //$pensjon.anmodning.referanseTilPerson"
        assertEquals("02",  sed.pensjon?.anmodning?.referanseTilPerson)
    }


    @Test
    fun `Forventerkorrekt utfylt P8000 hvor det finnes en sak i Pesys som har alderpensjon og henvendelsen gjelder gjenlevende`() {
        val fnr = generateRandomFnr(40)
        val forsikretPerson = lagTPSBruker(fnr, "Christopher", "Robin")
                .medAdresse("Gate",  "SWE")

        val avdodFnr = generateRandomFnr(93)
        val avdod = lagTPSBruker(avdodFnr, "Winnie", "Pooh")
        avdod.doedsdato = Doedsdato().withDoedsdato(convertToXMLocal(LocalDate.now()))

        val v1Kravhistorikk = V1KravHistorikk()
        v1Kravhistorikk.kravArsak = KravArsak.GJNL_SKAL_VURD.name

        doReturn(forsikretPerson).`when`(personV3Service).hentBruker(fnr)
        doReturn(avdod).`when`(personV3Service).hentBruker(avdodFnr)

        doReturn("NO").whenever(kodeverkClient).finnLandkode2("NOR")
        doReturn("SE").whenever(kodeverkClient).finnLandkode2("SWE")

        prefillData = PrefillDataModelMother.initialPrefillDataModel("P8000", fnr, penSaksnummer = pesysSaksnummer, avdod = PersonId(norskIdent = avdodFnr, aktorId = "21323"),  refTilPerson = ReferanseTilPerson.SOKER)

        val sed =  prefillSEDService.prefill(prefillData)

        //Gjenlevende/SOKER
        assertEquals("Winnie", sed.nav?.bruker?.person?.fornavn)
        assertEquals("Pooh", sed.nav?.bruker?.person?.etternavn)
        assertEquals("Robin", sed.nav?.annenperson?.person?.etternavn)
        assertEquals("02",  sed.pensjon?.anmodning?.referanseTilPerson)

    }

}

