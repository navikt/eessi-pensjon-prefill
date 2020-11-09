package no.nav.eessi.pensjon.fagmodul.prefill.person

import no.nav.eessi.pensjon.fagmodul.prefill.model.BrukerInformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.model.PersonData
import no.nav.eessi.pensjon.fagmodul.prefill.model.PersonId
import no.nav.eessi.pensjon.fagmodul.prefill.tps.PrefillAdresse
import no.nav.eessi.pensjon.fagmodul.sedmodel.Adresse
import no.nav.eessi.pensjon.fagmodul.sedmodel.ArbeidsforholdItem
import no.nav.eessi.pensjon.fagmodul.sedmodel.Bank
import no.nav.eessi.pensjon.fagmodul.sedmodel.BarnItem
import no.nav.eessi.pensjon.fagmodul.sedmodel.Bruker
import no.nav.eessi.pensjon.fagmodul.sedmodel.EessisakItem
import no.nav.eessi.pensjon.fagmodul.sedmodel.Ektefelle
import no.nav.eessi.pensjon.fagmodul.sedmodel.Foedested
import no.nav.eessi.pensjon.fagmodul.sedmodel.Foreldre
import no.nav.eessi.pensjon.fagmodul.sedmodel.Innehaver
import no.nav.eessi.pensjon.fagmodul.sedmodel.Konto
import no.nav.eessi.pensjon.fagmodul.sedmodel.Nav
import no.nav.eessi.pensjon.fagmodul.sedmodel.Person
import no.nav.eessi.pensjon.fagmodul.sedmodel.PinItem
import no.nav.eessi.pensjon.fagmodul.sedmodel.Sepa
import no.nav.eessi.pensjon.fagmodul.sedmodel.StatsborgerskapItem
import no.nav.eessi.pensjon.utils.simpleFormat
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kjoenn
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker as InformasjonBruker
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person as InformasjonPerson

private val UGYLDIGE_LAND_RINA = listOf("XXK")

@Component
class PrefillNav(private val prefillAdresse: PrefillAdresse,
                 @Value("\${eessi-pensjon-institusjon}") private val institutionid: String,
                 @Value("\${eessi-pensjon-institusjon-navn}") private val institutionnavn: String) {

    companion object {
        private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillNav::class.java) }

        fun createFodested(bruker: no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker): Foedested? {
            logger.debug("2.1.8.1       Fødested")

            val fsted = Foedested(
                    land = bruker.foedested ?: "Unknown",
                    by = "Unkown",
                    region = ""
            )

            return fsted.takeUnless { it.land == "Unknown" }
        }

        enum class RelasjonEnum(private val relasjon: String) {
            FAR("FARA"),
            MOR("MORA"),
            BARN("BARN");

            fun erSamme(relasjonTPS: String): Boolean {
                return relasjon == relasjonTPS
            }
        }

        fun isPersonAvdod(personTPS: InformasjonPerson) : Boolean {
            val personstatus = hentPersonStatus(personTPS)

            return (personstatus == "DØD")
                    .also { logger.debug("Person er avdod (ingen adresse å hente).") }
        }

        //hjelpe funkson for personstatus.
        private fun hentPersonStatus(personTPS: InformasjonPerson): String? {
            return personTPS.personstatus?.personstatus?.value
        }

        //knytes til nasjonalitet for utfylling P2x00
        //TODO: Mapping av kjønn skal defineres i codemapping i EUX
        private fun mapKjonn(kjonn: Kjoenn): String {
            logger.debug("2.1.4         Kjønn")
            val ktyper = kjonn.kjoenn
            return ktyper.value
        }

        //personnr fnr
        private fun hentNorIdent(person: InformasjonPerson): String {
            logger.debug("2.1.7.1.2         Personal Identification Number (PIN) personnr")
            val persident = person.aktoer as PersonIdent
            val pinid: NorskIdent = persident.ident
            return pinid.ident
        }

        //fdato i rinaformat
        private fun datoFormat(person: InformasjonBruker): String? {
            logger.debug("2.1.3         Date of birth")
            val fdato = person.foedselsdato
            logger.debug("              Date of birth: $fdato")
            return fdato?.foedselsdato?.simpleFormat()
        }

        private fun createPersonPinNorIdent(
                brukerTps: InformasjonBruker,
                institusjonId: String,
                institusjonNavn: String): List<PinItem> {
            logger.debug("2.1.7         Fodselsnummer/Personnummer")
            return listOf(
                    PinItem(
                            //hentet lokal NAV insitusjondata fra applikasjon properties.
                            institusjonsnavn = institusjonNavn,
                            institusjonsid = institusjonId,

                            //NAV/Norge benytter ikke seg av sektor, setter denne til null

                            //personnr
                            identifikator = hentNorIdent(brukerTps),

                            // norsk personnr settes alltid til NO da vi henter NorIdent
                            land = "NO"
                    )
            )
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

        //utfylling av liste av barn under 18år
        private fun createBarnliste(barn: List<Bruker?>): List<BarnItem>? {
            return barn
                    .filterNotNull()
                    .map { BarnItem(mor = it.mor, person = it.person, far = it.far, relasjontilbruker = "BARN") }
                    .takeIf { it.isNotEmpty() }
        }

        private fun createEktefelleType(typevalue: String): String {
            logger.debug("5.1           Ektefelle/Partnerskap-type : $typevalue")
            return when (typevalue) {
                "EKTE" -> "ektefelle"
                "REPA" -> "part_i_et_registrert_partnerskap"
                else -> "samboer"
            }
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

        private fun createInformasjonOmAnsettelsesforhold(personInfo: BrukerInformasjon): List<ArbeidsforholdItem>? {
            logger.debug("3.0           Informasjon om personens ansettelsesforhold og selvstendige næringsvirksomhet")
            logger.debug("3.1           Informasjon om ansettelsesforhold og selvstendig næringsvirksomhet ")
            return listOf(createAnsettelsesforhold(personInfo))
        }

        //lokal sak pkt 1.0 i gjelder alle SED
        private fun createEssisakItem(penSaksnummer: String, institusjonId: String, institusjonNavn: String): List<EessisakItem> {
            logger.debug("1.1           Lokalt saksnummer (hvor hentes disse verider ifra?")
            return listOf(EessisakItem(
                    institusjonsid = institusjonId,
                    institusjonsnavn = institusjonNavn,
                    saksnummer = penSaksnummer,
                    land = "NO"
            ))
        }
    }

    fun prefill(penSaksnummer: String, bruker: PersonId, avdod: PersonId?, personData: PersonData, brukerInformasjon: BrukerInformasjon?): Nav {

        val brukerEllerGjenlevende = personData.gjenlevendeEllerAvdod
        val ektefelleBruker = personData.ektefelleBruker
        val ekteTypeValue= personData.ekteTypeValue
        val barnBrukereFraTPS = personData.barnBrukereFraTPS

        val personInfo = brukerInformasjon

        return Nav(
                //1.0
                eessisak = createEssisakItem(penSaksnummer, institutionid, institutionnavn),

                //createBrukerfraTPS død hvis etterlatt (etterlatt aktoerregister fylt ut)
                //2.0 For levende, eller hvis person er dod (hvis dod flyttes levende til 3.0)
                //3.0 Anstalleseforhold og
                //8.0 Bank
                bruker = if (brukerEllerGjenlevende == null) null else
                            createBruker(
                                    brukerEllerGjenlevende,
                                    if (personInfo == null) null else createBankData(personInfo),
                                    if (personInfo == null) null else createInformasjonOmAnsettelsesforhold(personInfo)),

                //4.0 Ytelser ligger under pensjon object (P2000)

                //5.0 ektefelle eller partnerskap
                ektefelle = if (ektefelleBruker == null) null else
                        createEktefellePartner(createBruker(ektefelleBruker, null, null), ekteTypeValue),

                //6.0 skal denne kjøres hver gang? eller kun under P2000? P2100
                //sjekke om SED er P2x00 for utfylling av BARN
                //sjekke punkt for barn. pkt. 6.0 for P2000 og P2200 pkt. 8.0 for P2100
                barn = createBarnliste(barnBrukereFraTPS.map { createBruker(it, null, null) })
        )
    }

    fun createBruker(brukerTPS: InformasjonBruker,
                     bank: Bank?,
                     ansettelsesforhold: List<ArbeidsforholdItem>?): Bruker? {
            return Bruker(
                person = createPersonData(brukerTPS),
                adresse = if (isPersonAvdod(brukerTPS)) null else prefillAdresse.createPersonAdresse(brukerTPS),
                far = if (isPersonAvdod(brukerTPS)) null else createRelasjon(RelasjonEnum.FAR, brukerTPS),
                mor = if (isPersonAvdod(brukerTPS)) null else createRelasjon(RelasjonEnum.MOR, brukerTPS),
                bank = bank,
                arbeidsforhold = ansettelsesforhold)
    }

    //persondata - nav-sed format
    private fun createPersonData(brukerTps: InformasjonBruker): Person {
        logger.debug("2.1           Persondata (forsikret person / gjenlevende person / barn)")

        val landKode = brukerTps.statsborgerskap.land.value
        return Person(
                //2.1.1     familiy name
                etternavn = (brukerTps.personnavn as Personnavn).etternavn,
                //2.1.2     forname
                fornavn = createFornavnMellomNavn(brukerTps.personnavn),
                //2.1.3
                foedselsdato = datoFormat(brukerTps),
                //2.1.4     //sex
                kjoenn = mapKjonn(brukerTps.kjoenn),
                //2.1.6
                fornavnvedfoedsel = null,
                //2.1.7
                pin = createPersonPinNorIdent(brukerTps, institutionid, institutionnavn),
                //2.2.1.1
                statsborgerskap = listOf(createStatsborgerskap(landKode)),
                //2.1.8.1           place of birth
                foedested = createFodested(brukerTps),
                //2.2.2 -   P2100 = 5.2.2.
                sivilstand = null
        )
    }

    fun createFornavnMellomNavn(personnavn: Personnavn): String? {
       return if (personnavn.mellomnavn != null) personnavn.fornavn + " " + personnavn.mellomnavn else personnavn.fornavn
    }


    //mor / far
    private fun createRelasjon(relasjon: RelasjonEnum, person: InformasjonPerson): Foreldre? {
        person.harFraRolleI.forEach {
            val tpsvalue = it.tilRolle.value

            if (relasjon.erSamme(tpsvalue)) {
                logger.debug("              Relasjon til : $tpsvalue")
                val persontps = it.tilPerson as InformasjonPerson

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
                        etternavnvedfoedsel = null //if (RelasjonEnum.MOR.erSamme(tpsvalue)) null else navntps.etternavn
                )
                return Foreldre(person = relasjonperson)
            }
        }
        return null
    }




    private fun createEktefellePartner(ektefellpartnerbruker: Bruker?, ekteTypeValue: String?): Ektefelle? {
        logger.debug("5.0           Utfylling av ektefelle")
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

    /**
     * Prefiller to-bokstavs statsborgerskap
     * Hopper over Kosovo (XXK) fordi Rina ikke støttet Kosovo
     */
    private fun createStatsborgerskap(landkode: String): StatsborgerskapItem {
        logger.debug("2.2.1.1         Land / Statsborgerskap")
        if(UGYLDIGE_LAND_RINA.contains(landkode)){
            return StatsborgerskapItem()
        }
        return StatsborgerskapItem(prefillAdresse.hentLandkode(landkode))
    }
}