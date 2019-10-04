package no.nav.eessi.pensjon.fagmodul.prefill.person

import no.nav.eessi.pensjon.fagmodul.prefill.model.BrukerInformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.tps.NavFodselsnummer
import no.nav.eessi.pensjon.fagmodul.prefill.tps.PrefillAdresse
import no.nav.eessi.pensjon.fagmodul.prefill.tps.PrefillPersonDataFromTPS
import no.nav.eessi.pensjon.fagmodul.sedmodel.*
import no.nav.eessi.pensjon.fagmodul.sedmodel.Bruker
import no.nav.eessi.pensjon.fagmodul.sedmodel.Person
import no.nav.eessi.pensjon.fagmodul.sedmodel.Verge
import no.nav.eessi.pensjon.utils.simpleFormat
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

@Component
class PrefillNav(private val preutfyllingPersonFraTPS: PrefillPersonDataFromTPS,
                 private val prefillAdresse: PrefillAdresse,
                 @Value("\${eessi.pensjon_lokalid}") private val institutionid: String,
                 @Value("\${eessi.pensjon_lokalnavn}") private val institutionnavn: String) {


    companion object {
        private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillNav::class.java) }

        fun hentFodested(bruker: no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker): Foedested? {
            logger.debug("2.1.8.1       Fødested")

            val fsted = Foedested(
                    land = bruker.foedested ?: "Unknown",
                    by = "Unkown",
                    region = ""
            )
            if (fsted.land == "Unknown") {
                return null
            }
            return fsted
        }


    }

    fun prefill(prefillData: PrefillDataModel, fyllUtBarnListe: Boolean = false): Nav {

        // FIXME - det veksles mellom gjenlevende og bruker ... usikkert om dette er rett...
        val brukerEllerGjenlevende = preutfyllingPersonFraTPS.hentBrukerFraTPS(prefillData.brukerEllerGjenlevendeHvisDod())
        val bruker = preutfyllingPersonFraTPS.hentBrukerFraTPS(prefillData.personNr)

        val (ektepinid, ekteTypeValue) = filterEktefelleRelasjon(bruker)
        val ektefelleBruker = if(ektepinid.isBlank()) null else preutfyllingPersonFraTPS.hentBrukerFraTPS(ektepinid)

        val barn = if (fyllUtBarnListe) hentBarnFraTPS(prefillData.personNr) else listOf()

        return Nav(
                //1.0
                eessisak = createEssisakItem(prefillData.penSaksnummer),

                //createBrukerfraTPS død hvis etterlatt (etterlatt aktoerregister fylt ut)
                //2.0 For levende, eller hvis person er dod (hvis dod flyttes levende til 3.0)
                //3.0 Anstalleseforhold og
                //8.0 Bank
                bruker = createBrukerFraTPSOgRequest(brukerEllerGjenlevende, prefillData.getPersonInfoFromRequestData()),

                //4.0 Ytelser ligger under pensjon object (P2000)

                //5.0 ektefelle eller partnerskap
                ektefelle = createEktefelle(ektefelleBruker, ekteTypeValue),

                //6.0 skal denne kjøres hver gang? eller kun under P2000? P2100
                //sjekke om SED er P2x00 for utfylling av BARN
                //sjekke punkt for barn. pkt. 6.0 for P2000 og P2200 pkt. 8.0 for P2100
                barn = createBarnliste(barn),

                //7.0 verge
                verge = createVerge(),

                //8.0 Bank lagt in på bruker (P2000)

                //9.0  - Tillgeggsinfo og kravdata. benyttes i P2x000
                krav = createDiverseOgKravDato()
        )
    }

    enum class RelasjonEnum(val relasjon: String) {
        FAR("FARA"),
        MOR("MORA"),
        BARN("BARN");

        fun erSamme(relasjonTPS: String): Boolean {
            return relasjon == relasjonTPS
        }
    }

    fun createBruker(brukerTPS: no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker?, bank: Bank?, ansettelsesforhold: List<ArbeidsforholdItem>?): Bruker? {
        if (brukerTPS == null) return null
        var adresse: Adresse? = null
        var far: Foreldre? = null
        var mor: Foreldre? = null

        if (!isPersonAvdod(brukerTPS)) {
            adresse = prefillAdresse.createPersonAdresse(brukerTPS)
            far = createRelasjon(RelasjonEnum.FAR, brukerTPS)
            mor = createRelasjon(RelasjonEnum.MOR, brukerTPS)
        }

        return Bruker(
                person = createPersonData(brukerTPS),
                adresse = adresse,
                far = far,
                mor = mor,
                bank = bank,
                arbeidsforhold = ansettelsesforhold
        )
    }

    fun isPersonAvdod(personTPS: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person) : Boolean {
        val personstatus = hentPersonStatus(personTPS)
        if (personstatus == "DØD") {
            logger.debug("Person er avdod (ingen adresse å hente).")
            return true
        }
        return false
    }

    //mor / far
    private fun createRelasjon(relasjon: RelasjonEnum, person: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person): Foreldre? {
        person.harFraRolleI.forEach {
            val tpsvalue = it.tilRolle.value

            if (relasjon.erSamme(tpsvalue)) {
                logger.debug("              Relasjon til : $tpsvalue")
                val persontps = it.tilPerson as no.nav.tjeneste.virksomhet.person.v3.informasjon.Person

                val navntps = persontps.personnavn as Personnavn
                val relasjonperson = Person(
                        pin = listOf(
                                PinItem(
                                        institusjonsnavn = institutionnavn,
                                        institusjonsid = institutionid,
                                        identifikator = hentNorIdent(persontps),
                                        land = "NO"
                                )
                        ),
                        fornavn = navntps.fornavn,
                        etternavnvedfoedsel = if (RelasjonEnum.MOR.erSamme(tpsvalue)) null else navntps.etternavn
                )
                return Foreldre(person = relasjonperson)
            }
        }
        return null
    }

    //persondata - nav-sed format
    private fun createPersonData(brukerTps: no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker): Person {
        logger.debug("2.1           Persondata (forsikret person / gjenlevende person / barn)")
        return Person(
                //2.1.1     familiy name
                etternavn = (brukerTps.personnavn as Personnavn).etternavn,
                //2.1.2     forname
                fornavn = (brukerTps.personnavn as Personnavn).fornavn,
                //2.1.3
                foedselsdato = datoFormat(brukerTps),
                //2.1.4     //sex
                kjoenn = mapKjonn(brukerTps.kjoenn),
                //2.1.6
                fornavnvedfoedsel = (brukerTps.personnavn as Personnavn).fornavn,
                //2.1.7
                pin = hentPersonPinNorIdent(brukerTps),
                //2.2.1.1
                statsborgerskap = listOf(hentStatsborgerskapTps(brukerTps)),
                //2.1.8.1           place of birth
                foedested = hentFodested(brukerTps),
                //2.2.2 -   P2100 = 5.2.2.
                //TODO skaper feil under P2100 utkommenter intillvidere
                sivilstand = null // if (isPersonAvdod(brukerTps)) null else createSivilstand(brukerTps)
        )
    }

    //hjelpe funkson for personstatus.
    private fun hentPersonStatus(personTPS: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person): String? {
        return personTPS.personstatus?.personstatus?.value
    }

    //knytes til nasjonalitet for utfylling P2x00
    private fun hentStatsborgerskapTps(person: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person): StatsborgerskapItem {
        logger.debug("2.2.1.1         Land / Statsborgerskap")

        val statsborgerskap = person.statsborgerskap as Statsborgerskap
        val land = statsborgerskap.land as Landkoder

        return StatsborgerskapItem(
                land = prefillAdresse.hentLandkode(land)
        )
    }

    //TODO: Mapping av kjønn skal defineres i codemapping i EUX
    private fun mapKjonn(kjonn: Kjoenn): String {
        logger.debug("2.1.4         Kjønn")
        val ktyper = kjonn.kjoenn
        return ktyper.value
    }

    //personnr fnr
    private fun hentNorIdent(person: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person): String {
        logger.debug("2.1.7.1.2         Personal Identification Number (PIN) personnr")
        val persident = person.aktoer as PersonIdent
        val pinid: NorskIdent = persident.ident
        return pinid.ident
    }

    //fdato i rinaformat
    private fun datoFormat(person: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person): String? {
        logger.debug("2.1.3         Date of birth")
        val fdato = person.foedselsdato
        logger.debug("              Date of birth: $fdato")
        return fdato?.foedselsdato?.simpleFormat()
    }

    private fun hentPersonPinNorIdent(brukerTps: no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker): List<PinItem> {
        logger.debug("2.1.7         Fodselsnummer/Personnummer")
        return listOf(
                PinItem(
                        //hentet lokal NAV insitusjondata fra applikasjon properties.
                        institusjonsnavn = institutionnavn,
                        institusjonsid = institutionid,

                        //NAV/Norge benytter ikke seg av sektor, setter denne til null

                        //personnr
                        identifikator = hentNorIdent(brukerTps),

                        // norsk personnr settes alltid til NO da vi henter NorIdent
                        land = "NO"
                )
        )
    }

    //7.0  TODO: 7. Informasjon om representant/verge hva kan vi hente av informasjon? fra hvor
    private fun createVerge(): Verge? {
        logger.debug("7.0           (IKKE NOE HER ENNÅ!!) Informasjon om representant/verge")
        return null
    }

    //8.0 Bank detalsjer om bank betalinger.
    private fun createBankData(personInfo: BrukerInformasjon): Bank {
        logger.debug("8.0           Informasjon om betaling")
        logger.debug("8.1           Informasjon om betaling")
        return Bank(
                    navn = personInfo.bankName,
                    konto = Konto(
                            innehaver = Innehaver(
                                    rolle = "01", //forsikkret bruker .. avventer med Verge "02",
                                    navn = personInfo.bankName
                            ),
                            sepa = Sepa(
                                    iban = personInfo.bankIban,
                                    swift = personInfo.bankBicSwift
                            )

                    ),
                    adresse = Adresse(
                            gate = personInfo.bankAddress,
                            land = personInfo.bankCountry?.currencyLabel
                    )
        )
    }

    //
    //TODO: Dette må hentes fra sak/krav
    private fun createDiverseOgKravDato(): Krav {
        logger.debug("9.1           (FRA V1SAK?) Dato for krav")
        return Krav(Date().simpleFormat())
    }

    // kan denne utfylling benyttes på alle SED?
    // etterlatt pensjon da er dette den avdøde.(ikke levende)
    // etterlatt pensjon da er den levende i pk.3 sed (gjenlevende) (pensjon.gjenlevende)
    private fun createBrukerFraTPSOgRequest(brukerTPS: no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker?, personInfo: BrukerInformasjon?): Bruker? {
        return createBruker(
                brukerTPS,
                if (personInfo == null) null else createBankData(personInfo),
                if (personInfo == null) null else createInformasjonOmAnsettelsesforhold(personInfo)
        )
    }

    //utfylling av liste av barn under 18år
    private fun createBarnliste(barn: List<Bruker?>): List<BarnItem>? {
        val barnlist = barn
                .filterNotNull()
                .map {
                    BarnItem(
                        person = it.person,
                        far = it.far,
                        mor = it.mor,
                        relasjontilbruker = "BARN"
                    )
                }
        return if (barnlist.isEmpty()) null else barnlist
    }

    private fun hentBarnFraTPS(personNr: String): List<Bruker?> {
        val barnaspin = barnsPinId(preutfyllingPersonFraTPS.hentBrukerFraTPS(personNr))
        val barn = barnaspin.map {
            createBruker(preutfyllingPersonFraTPS.hentBrukerFraTPS(it), null, null)
        }
        return barn
    }

    private fun barnsPinId(brukerTPS: no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker?): List<String> {
        if (brukerTPS == null) return listOf()

        val person = brukerTPS as no.nav.tjeneste.virksomhet.person.v3.informasjon.Person

        val resultat = mutableListOf<String>()
        person.harFraRolleI.forEach {
            val tpsvalue = it.tilRolle.value   //mulig nullpoint? kan tilRolle være null?
            if (RelasjonEnum.BARN.erSamme(tpsvalue)) {
                val persontps = it.tilPerson as no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
                val norIdent = hentNorIdent(persontps)
                if (NavFodselsnummer(norIdent).validate()) {
                    resultat.add(norIdent)
                } else {
                    logger.error("følgende ident funnet ikke gyldig: $norIdent")
                }
            }
        }
        return resultat.toList()
    }



    //lokal sak pkt 1.0 i gjelder alle SED
    private fun createEssisakItem(penSaksnummer: String): List<EessisakItem> {
        logger.debug("1.1           Lokalt saksnummer (hvor hentes disse verider ifra?")
        return listOf(EessisakItem(
                institusjonsid = institutionid,
                institusjonsnavn = institutionnavn,
                saksnummer = penSaksnummer,
                land = "NO"
        ))
    }

    private fun createInformasjonOmAnsettelsesforhold(personInfo: BrukerInformasjon): List<ArbeidsforholdItem>? {
        logger.debug("3.0           Informasjon om personens ansettelsesforhold og selvstendige næringsvirksomhet")
        logger.debug("3.1           Informasjon om ansettelsesforhold og selvstendig næringsvirksomhet ")
        return listOf(createAnsettelsesforhold(personInfo))
    }

    private fun createAnsettelsesforhold(personInfo: BrukerInformasjon): ArbeidsforholdItem {
        logger.debug("3.1           Ansettelseforhold/arbeidsforhold")
        return ArbeidsforholdItem(
                //3.1.1.
                yrke = "",
                //3.1.2
                type = personInfo.workType ?: "",
                //3.1.3
                planlagtstartdato = personInfo.workStartDate?.simpleFormat() ?: "",
                //3.1.4
                sluttdato = personInfo.workEndDate?.simpleFormat() ?: "",
                //3.1.5
                planlagtpensjoneringsdato = personInfo.workEstimatedRetirementDate?.simpleFormat() ?: "",
                //3.1.6
                arbeidstimerperuke = personInfo.workHourPerWeek ?: ""
        )
    }

    private fun createEktefelle(ektefelleBruker: no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker?, ekteTypeValue: String?): Ektefelle? {
        logger.debug("5.0           Utfylling av ektefelle")
        if (ektefelleBruker == null) return null
        val ektefellpartnerbruker = createBruker(ektefelleBruker, null, null)
        if (ektefellpartnerbruker == null) return null
        return Ektefelle(
                //type
                //5.1   -- 01 - ektefelle, 02, part i partnerskap, 3, samboer
                type = createEktefelleType(ekteTypeValue!!),
                //ektefelle (personobj kjører på nytt)
                person = ektefellpartnerbruker.person,
                //foreldre
                far = ektefellpartnerbruker.far,
                //foreldre
                mor = ektefellpartnerbruker.mor
        )
    }

    private fun createEktefelleType(typevalue: String): String {
        logger.debug("5.1           Ektefelle/Partnerskap-type")
        return when (typevalue) {
            "EKTE" -> "01"
            "PART" -> "02"
            else -> "03"
        }
    }

    private fun filterEktefelleRelasjon(bruker: no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker?): Pair<String, String> {
        if (bruker == null) return Pair("","")
        var ektepinid = ""
        var ekteTypeValue = ""

        bruker.harFraRolleI.forEach {
            if (it.tilRolle.value == "EKTE") { // FIXME TODO - dette begrenser til kun EKTEFELLE (ikke PARTNER og evt andre)

                ekteTypeValue = it.tilRolle.value
                val tilperson = it.tilPerson
                val pident = tilperson.aktoer as PersonIdent

                ektepinid = pident.ident.ident
                if (ektepinid.isNotBlank()) {
                    return@forEach
                }
            }
        }
        return Pair(ektepinid, ekteTypeValue)
    }
}

