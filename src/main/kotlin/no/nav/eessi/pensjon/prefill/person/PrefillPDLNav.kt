package no.nav.eessi.pensjon.prefill.person

import no.nav.eessi.pensjon.eux.model.sed.*
import no.nav.eessi.pensjon.kodeverk.KodeverkClient.Companion.toJson
import no.nav.eessi.pensjon.personoppslag.pdl.model.*
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentGruppe.FOLKEREGISTERIDENT
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentGruppe.NPID
import no.nav.eessi.pensjon.personoppslag.pdl.model.Sivilstandstype.*
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.shared.api.BankOgArbeid
import no.nav.eessi.pensjon.shared.api.PersonInfo
import no.nav.eessi.pensjon.shared.person.Fodselsnummer
import no.nav.eessi.pensjon.utils.simpleFormat
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

private val UGYLDIGE_LAND_RINA = listOf("XXK")

@Component
class PrefillPDLNav(private val prefillAdresse: PrefillPDLAdresse,
                    @Value("\${eessi-pensjon-institusjon}") private val institutionid: String,
                    @Value("\${eessi-pensjon-institusjon-navn}") private val institutionnavn: String) {

    companion object {
        private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillPDLNav::class.java) }

        private fun PdlPerson.norskIdent() = this.identer.firstOrNull { it.gruppe == FOLKEREGISTERIDENT }?.ident
        private fun PdlPerson.npidIdent() = this.identer.firstOrNull { it.gruppe == NPID }?.ident

        private fun PdlPerson.kortKjonn() = when(this.kjoenn?.kjoenn) {
            KjoennType.MANN -> "M"
            KjoennType.KVINNE -> "K"
            else -> "U"
        }
        private fun PdlPerson.foedseldato(): String {
            val fdato = this.foedsel?.foedselsdato

            if (fdato == null) {
                val fnr = this.identer.firstOrNull { it.gruppe == FOLKEREGISTERIDENT }?.ident
                return Fodselsnummer.fra(fnr)?.getBirthDate().toString()
            }
            return fdato.toString()
        }

        fun isPersonAvdod(pdlperson: PdlPerson) : Boolean {
            return pdlperson.erDoed()
                .also {
                 if (it)
                  logger.debug("Person er avdod (ingen adresse å hente).")
                }
        }

        //8.0 Bank detalsjer om bank betalinger.
        private fun createBankData(personInfo: BankOgArbeid): Bank {
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
                    .map { BarnItem(mor = it.mor, person = it.person, far = it.far, relasjontilbruker43 = Familierelasjonsrolle.BARN.name) }
                    .takeIf { it.isNotEmpty() }
        }

        private fun createEktefelleType(typevalue: Sivilstandstype): String {
            logger.debug("5.1           Ektefelle/Partnerskap-type : $typevalue")
            return when (typevalue) {
                GIFT -> "ektefelle"
                REGISTRERT_PARTNER -> "part_i_et_registrert_partnerskap"
                else -> "" // endring fra TPS istede for SAMBOER så blank hvis ukjent/ikke gift/partner.
            }
        }



        private fun createAnsettelsesforhold(personInfo: BankOgArbeid): ArbeidsforholdItem {
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

        private fun createInformasjonOmAnsettelsesforhold(personInfo: BankOgArbeid): List<ArbeidsforholdItem> {
            logger.debug("3.0           Informasjon om personens ansettelsesforhold og selvstendige næringsvirksomhet")
            logger.debug("3.1           Informasjon om ansettelsesforhold og selvstendig næringsvirksomhet ")
            return listOf(createAnsettelsesforhold(personInfo))
        }

        //lokal sak pkt 1.0 i gjelder alle SED
        private fun createEssisakItem(penSaksnummer: String?, institusjonId: String, institusjonNavn: String): List<EessisakItem> {
            logger.debug("1.1           Lokalt saksnummer (hvor hentes disse verider ifra?")
            return listOf(EessisakItem(
                    institusjonsid = institusjonId,
                    institusjonsnavn = institusjonNavn,
                    saksnummer = penSaksnummer,
                    land = "NO"
            ))
        }
    }

    fun prefill(
        penSaksnummer: String?,
        bruker: PersonInfo,
        avdod: PersonInfo?,
        personData: PersonDataCollection,
        bankOgArbeid: BankOgArbeid?,
        krav: Krav? = null,
        annenPerson: Bruker? = null
    ): Nav {
        val forsikretPerson = personData.forsikretPerson
        val avdodEllerGjenlevende = personData.gjenlevendeEllerAvdod
        val ektefellePerson = personData.ektefellePerson
        val barnPersonList = personData.barnPersonList

        logger.debug(
            """
                ----------------------------------------------------------------------------------------
                forsikret: ${forsikretPerson?.navn?.sammensattNavn}
                avdød    : ${avdodEllerGjenlevende?.navn?.sammensattNavn}
                ----------------------------------------------------------------------------------------
            """.trimIndent()
        )

        return Nav(
                //1.0
                eessisak = createEssisakItem(penSaksnummer, institutionid, institutionnavn),

                //createBruker fra Persondataløsning
                //2.0 For levende, eller hvis person er dod (hvis dod flyttes levende til 3.0)
                //3.0 Anstalleseforhold og
                //8.0 Bank
                bruker = avdodEllerGjenlevende?.let { it ->
                    createBruker(
                                    it,
                                    bankOgArbeid?.let { createBankData(it) },
                                    bankOgArbeid?.let { createInformasjonOmAnsettelsesforhold(it) },
                                    bruker
                            )
                },

                //4.0 Ytelser ligger under pensjon object (P2000)

                //5.0 ektefelle eller partnerskap
                ektefelle = ektefellePerson?.let {
                        createEktefellePartner(createBruker(it, null, null, bruker), avdodEllerGjenlevende?.sivilstand?.firstOrNull()?.type)
                },

                //6.0 skal denne kjøres hver gang? eller kun under P2000? P2100
                //sjekke om SED er P2x00 for utfylling av BARN
                //sjekke punkt for barn. pkt. 6.0 for P2000 og P2200 pkt. 8.0 for P2100
                barn = createBarnliste(barnPersonList.map { createPersonBarn(it, personData) }),
                annenperson = annenPerson,
                krav = krav
        )
    }

    fun createGjenlevende(gjenlevendeBruker: PdlPerson?, personInfoBruker: PersonInfo): Bruker? {
        logger.info("          Utfylling gjenlevende (etterlatt persjon.gjenlevende)")
        return createBruker(gjenlevendeBruker!!, personInfoBruker)
    }

    fun createBruker(pdlperson: PdlPerson, personInfo: PersonInfo) = createBruker(pdlperson, null, null, personInfo)

    fun createBruker(pdlperson: PdlPerson,
                     bank: Bank?,
                     ansettelsesforhold: List<ArbeidsforholdItem>?, personInfo: PersonInfo?): Bruker? {
            return Bruker(
                person = createPersonData(pdlperson, personInfo),
                adresse = prefillAdresse.createPersonAdresse(pdlperson),
                bank = bank,
                arbeidsforhold = ansettelsesforhold,)
    }

    fun createPersonBarn(pdlperson: PdlPerson, personData: PersonDataCollection): Bruker? {
        logger.debug("6.0 barn og familierelasjoner far/mor")
        return Bruker(
            person = createPersonData(pdlperson),
            far = createFamilieRelasjon(Familierelasjonsrolle.FAR, pdlperson, personData),
            mor = createFamilieRelasjon(Familierelasjonsrolle.MOR, pdlperson, personData)
        )
    }


    //mor / far
    private fun createFamilieRelasjon(relasjon: Familierelasjonsrolle, barnpdlperson: PdlPerson, personData: PersonDataCollection): Foreldre? {

        //ident til relasjon (FAR/MOR)
        val relasjonIdent = barnpdlperson.forelderBarnRelasjon.firstOrNull { it.relatertPersonsRolle == relasjon }?.relatertPersonsIdent

        val forsikretPerson = personData.forsikretPerson
        val ektePerson = personData.ektefellePerson

        //hvem er foreldre for denne relasjonen (FAR/MOR)
        val foreldrePerson = when (relasjonIdent) {
            ektePerson?.norskIdent() -> ektePerson
            ektePerson?.npidIdent() -> ektePerson
            forsikretPerson?.norskIdent() -> forsikretPerson
            forsikretPerson?.npidIdent() -> forsikretPerson
            else -> {
                null
            }
        } ?: return null

        logger.debug("              Relasjon til : ${foreldrePerson.navn?.sammensattNavn}")
        val navn = foreldrePerson.navn
        val relasjonperson = Person(
            pin = listOf(
                PinItem(
                    institusjonsnavn = institutionnavn,
                    institusjonsid = institutionid,
                    identifikator = foreldrePerson.norskIdent() ?: foreldrePerson.npidIdent(),
                    land = "NO"
                )
            ),
            fornavn = navn?.fornavn,
        )
        return Foreldre(person = relasjonperson)
    }

    //persondata - nav-sed format
    private fun createPersonData(
        pdlperson: PdlPerson,
        personInfo: PersonInfo? = null): Person {

        logger.debug("2.1           Persondata (forsikret person / gjenlevende person / barn)")

        return Person(
                //2.1.1     familiy name
                etternavn = pdlperson.navn?.etternavn,
                //2.1.2     forname
                fornavn = createFornavnMellomNavn(pdlperson.navn),
                //2.1.3
                foedselsdato = pdlperson.foedseldato(),
                //2.1.4     //sex
                kjoenn = pdlperson.kortKjonn(),
                //2.1.7
                pin = createPersonPin(pdlperson, institutionid, institutionnavn),
                //2.2.1.1     statsborgerskap
                statsborgerskap = createStatsborgerskap(pdlperson),
                //2.2.1.1     sivilstand
                sivilstand = createSivilstand(pdlperson),
                //2.1.8.1           place of birth
                foedested = createFodested(pdlperson),

                kontakt = createKontakt(personInfo)

        )
    }

    fun createPersonPin(
        personpdl: PdlPerson,
        institusjonId: String,
        institusjonNavn: String): List<PinItem> {
        logger.debug("2.1.7         Fodselsnummer/Personnummer")
        val norskeIdenter = personpdl.identer.filter { it.gruppe == FOLKEREGISTERIDENT || it.gruppe == NPID }.map {
            PinItem(
                //hentet lokal NAV insitusjondata fra applikasjon properties.
                institusjonsnavn = institusjonNavn,
                institusjonsid = institusjonId,
                identifikator = it.ident,
                land = "NO"
            )
        }
        val utenlandskeIdenter = personpdl.utenlandskIdentifikasjonsnummer.map {
            PinItem(
                //Utenlandsk ident
                identifikator = it.identifikasjonsnummer,
                land = prefillAdresse.hentLandkode(it.utstederland)
            )
        }
        return norskeIdenter + utenlandskeIdenter
    }

    private fun createKontakt(personInfo: PersonInfo?): Kontakt? {
        logger.debug("Persondata kontakt: ${personInfo?.toJson()}")

        personInfo ?: return null
        val telefonList = personInfo.telefonKrr?.let { listOf(Telefon("mobil", it)) }
        val emailList = personInfo.epostKrr?.let { listOf(Email(it)) }

        return if (telefonList == null && emailList == null) null else Kontakt(telefonList, emailList)
    }

    private fun createFornavnMellomNavn(personnavn: Navn?): String {
       return listOfNotNull(personnavn?.fornavn, personnavn?.mellomnavn)
           .joinToString(separator = " ")
    }

    private fun createEktefellePartner(ektefellpartnerbruker: Bruker?, ekteTypeValue: Sivilstandstype?): Ektefelle? {
        logger.debug("5.0           Utfylling av ektefelle")
        if (ektefellpartnerbruker == null) return null
        return Ektefelle(
                //type
                //5.1   -- 01 - ektefelle, 02, part i partnerskap, 3, samboer
                type = createEktefelleType(ekteTypeValue!!),
                //ektefelle (personobj kjører på nytt)
                person = ektefellpartnerbruker.person
        )
    }

    private fun validateUgyldigeLand(landkode: String?): String? {
        return if(UGYLDIGE_LAND_RINA.contains(landkode)){
            null
        } else {
            return prefillAdresse.hentLandkode(landkode)
        }
    }

    fun createFodested(pdlperson: PdlPerson): Foedested? {
        logger.debug("2.1.8.1       Fødested")
        logger.debug("              foedsel : ${pdlperson.foedsel}")

        val foedested = pdlperson.foedsel?.foedested
        val landkode = validateUgyldigeLand(pdlperson.foedsel?.foedeland)
        val fsted = Foedested(
            land = landkode ?: "Unknown",
            by =  foedested ?: "Unknown",
            region = ""
        )
        logger.info("foedsel land: ${fsted.land != "Unknown"}, by: ${fsted.by != "Unknown"}")
        return fsted.takeUnless { it.land == "Unknown" }
    }

    /**
     * Prefiller to-bokstavs statsborgerskap
     * Hopper over Kosovo (XXK) fordi Rina ikke støttet Kosovo
     */
    private fun createStatsborgerskap(pdlperson: PdlPerson): List<StatsborgerskapItem> {
        logger.debug("2.2.1.1         Land / Statsborgerskap")
        val statsborgerskap = pdlperson.statsborgerskap
            .filterNot { validateUgyldigeLand(it.land) == null }
            .map {
                logger.debug("              Statsborgerskap: ${it.land}")
            StatsborgerskapItem(prefillAdresse.hentLandkode(it.land))
        }

        return statsborgerskap.distinct()
    }

    /**
     * Prefiller sivilstand-status og -dato fra PDL
     */
    private fun createSivilstand(pdlperson: PdlPerson): List<SivilstandItem> {
        logger.debug("2.2.2.1        Sivilstand")
        val sivilstand = pdlperson.sivilstand
            .filterNot { it.gyldigFraOgMed == null }
            .map {
                logger.info("Sivilstand: ${it.type} dato: ${it.gyldigFraOgMed}")
                when(it.type){
                    UGIFT -> SivilstandItem(it.gyldigFraOgMed.toString(), SivilstandRina.enslig)
                    GIFT -> SivilstandItem(it.gyldigFraOgMed.toString(), SivilstandRina.gift)
                    SKILT -> SivilstandItem(it.gyldigFraOgMed.toString(), SivilstandRina.skilt)
                    REGISTRERT_PARTNER -> SivilstandItem(it.gyldigFraOgMed.toString(), SivilstandRina.registrert_partnerskap)
                    else -> SivilstandItem(null, null)
                }
            }
        return sivilstand.distinct()
    }
}