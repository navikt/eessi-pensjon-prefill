package no.nav.eessi.pensjon.prefill

import no.nav.eessi.pensjon.personoppslag.pdl.model.AdressebeskyttelseGradering
import no.nav.eessi.pensjon.personoppslag.pdl.model.Bostedsadresse
import no.nav.eessi.pensjon.personoppslag.pdl.model.Doedsfall
import no.nav.eessi.pensjon.personoppslag.pdl.model.Endring
import no.nav.eessi.pensjon.personoppslag.pdl.model.Endringstype
import no.nav.eessi.pensjon.personoppslag.pdl.model.Familierelasjonsrolle
import no.nav.eessi.pensjon.personoppslag.pdl.model.Foedsel
import no.nav.eessi.pensjon.personoppslag.pdl.model.ForelderBarnRelasjon
import no.nav.eessi.pensjon.personoppslag.pdl.model.GeografiskTilknytning
import no.nav.eessi.pensjon.personoppslag.pdl.model.GtType
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentGruppe
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentInformasjon
import no.nav.eessi.pensjon.personoppslag.pdl.model.Kjoenn
import no.nav.eessi.pensjon.personoppslag.pdl.model.KjoennType
import no.nav.eessi.pensjon.personoppslag.pdl.model.Metadata
import no.nav.eessi.pensjon.personoppslag.pdl.model.Navn
import no.nav.eessi.pensjon.personoppslag.pdl.model.Oppholdsadresse
import no.nav.eessi.pensjon.personoppslag.pdl.model.Person
import no.nav.eessi.pensjon.personoppslag.pdl.model.Sivilstand
import no.nav.eessi.pensjon.personoppslag.pdl.model.Sivilstandstype
import no.nav.eessi.pensjon.personoppslag.pdl.model.Statsborgerskap
import no.nav.eessi.pensjon.personoppslag.pdl.model.UtenlandskAdresse
import no.nav.eessi.pensjon.personoppslag.pdl.model.Vegadresse
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import java.time.LocalDate
import java.time.LocalDateTime

object PersonPDLMock {
    internal fun createWith(landkoder: Boolean = true, fornavn: String = "Test", etternavn: String = "Testesen", fnr: String = "3123", aktoerid: String = "3213", erDod: Boolean? = false, metadata: Metadata = mockMeta()) :Person {
            val fdatoaar =  if (erDod != null && erDod == true) LocalDate.of(1921, 7, 12) else LocalDate.of(1988, 7, 12)
            val doeadfall = if (erDod != null && erDod == true) Doedsfall(LocalDate.of(2020, 10, 1), null, metadata) else null
            val kommuneLandkode = when(landkoder) {
                true -> "026123"
                else -> null
            }
            return Person(
                listOf(
                    IdentInformasjon(
                        fnr,
                        IdentGruppe.FOLKEREGISTERIDENT
                    ),
                    IdentInformasjon(
                        aktoerid,
                        IdentGruppe.AKTORID
                    )
                ),
                Navn(fornavn, null, etternavn, null, null, null, metadata),
                emptyList(),
                Bostedsadresse(
                    LocalDateTime.of(1980, 10, 1, 10, 10, 10),
                    LocalDateTime.of(2300, 10, 1, 10, 10, 10),
                    Vegadresse(
                        "Oppoverbakken",
                        "66",
                        null,
                        "1920",
                        null,
                        null
                    ),
                    null,
                    metadata
                ),
                null,
                listOf(Statsborgerskap(
                    "NOR",
                    LocalDate.of(1980, 10 , 1),
                    LocalDate.of(2300, 10, 1),
                    metadata)
                ),
                Foedsel(
                    fdatoaar,
                    null,
                    null,
                    null,
                    null,
                    metadata
                ),
                GeografiskTilknytning(
                    GtType.KOMMUNE,
                    kommuneLandkode,
                    null,
                    "NOR"
                ),
                Kjoenn(
                    KjoennType.MANN,
                    null,
                    metadata
                ),
                doeadfall,
                emptyList(),
                listOf(
                    Sivilstand(
                        Sivilstandstype.UGIFT,
                        LocalDate.of(2000, 10, 1),
                        null,
                        metadata
                    )
                ),
            null
            )
    }

    internal fun mockMeta(registrert: LocalDateTime = LocalDateTime.of(2010, 4, 2, 10, 14, 12)) : Metadata {
        return Metadata(
            listOf(
                Endring(
                "DOLLY",
                registrert,
                "Dolly",
                "FREG",
                Endringstype.OPPRETT
            )
            ),
            false,
            "FREG",
            "23123123-12312312-123123"
        )
    }

    internal fun Person.medSivilstand(person: Person): Person {
        val ident = person.identer.firstOrNull { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }?.ident
        return this.copy(sivilstand = listOf(
            Sivilstand(Sivilstandstype.GIFT, LocalDate.of (2010, 1,10), ident, mockMeta())
        ))
    }

    internal fun Person.medFodsel(date: LocalDate, land: String? = "NOR")  = this.copy(foedsel = Foedsel(date, land,null, null, null, mockMeta()))

    internal fun Person.medKjoenn(type: KjoennType) = this.copy(kjoenn = Kjoenn(type, null, mockMeta()))

    internal fun Person.medBeskyttelse(gradering: AdressebeskyttelseGradering) = this.copy(adressebeskyttelse = listOf(gradering))

    internal fun Person.medForeldre(foreldre: Person): Person {
        val foreldreRolle = familieRolle(foreldre)
        val foreldrefnr = foreldre.identer.firstOrNull { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }?.ident
        val list = mutableListOf<ForelderBarnRelasjon>()
        list.addAll(this.forelderBarnRelasjon)
        list.add(
            ForelderBarnRelasjon(
            relatertPersonsIdent = foreldrefnr!!,
            relatertPersonsRolle = foreldreRolle,
            minRolleForPerson = Familierelasjonsrolle.BARN,
            metadata = mockMeta()
            )
        )
        return this.copy(forelderBarnRelasjon = list)
    }

    internal fun Person.medBarn(barn: Person): Person {
        val minRolle  = familieRolle(this)
        val barnfnr = barn.identer.firstOrNull { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }?.ident
        val list = mutableListOf<ForelderBarnRelasjon>()
        list.addAll(this.forelderBarnRelasjon)
        list.add(
            ForelderBarnRelasjon(
                relatertPersonsIdent = barnfnr!!,
                relatertPersonsRolle = Familierelasjonsrolle.BARN,
                minRolleForPerson = minRolle,
                metadata = mockMeta()
            )
        )
        println("Relasjonlist : " + list)
        return this.copy(forelderBarnRelasjon = list)
    }

    //subsitutt for tps testfiler Person-20000.json... .enke med barn
    internal fun createEnkeWithBarn(enkefnr: String, barn1fnr: String, barn2fnr: String? = null): PersonDataCollection {
        val enkePerson = createWith(true, "JESSINE TORDNU", "BOUWMANS", enkefnr, "212")
            .medFodsel(LocalDate.of(1967, 5,3))
            .medKjoenn(KjoennType.KVINNE)

        val barn1 = createWith(true, "TOPPI DOTTO",  "BOUWMANS", barn1fnr)
            .medForeldre(enkePerson)
            .medFodsel(LocalDate.of(1990, 5, 3))
            .medKjoenn(KjoennType.KVINNE)

        val barn2 = barn2fnr?.let { bfnr ->
            createWith(true,"EGIDIJS MASKOT", "BOUWMANS", bfnr)
                .medForeldre(enkePerson)
                .medKjoenn(KjoennType.KVINNE)
                .medFodsel(LocalDate.of(1988,5,3))
        }

        return PersonDataCollection(
            forsikretPerson = enkePerson,
            ektefellePerson = null,
            sivilstandstype = Sivilstandstype.ENKE_ELLER_ENKEMANN,
            gjenlevendeEllerAvdod = enkePerson,
            barnPersonList = listOfNotNull(
                barn1,
                barn2
            ))
    }

    private fun familieRolle(person: Person) : Familierelasjonsrolle {
        return when(person.kjoenn?.kjoenn) {
            KjoennType.MANN -> Familierelasjonsrolle.FAR
            KjoennType.KVINNE -> Familierelasjonsrolle.MOR
            else -> Familierelasjonsrolle.MEDMOR
        }
    }

    internal fun Person.medAdresse(gate: String, postnummer: String, husnr: String? = "") = this.copy(bostedsadresse = Bostedsadresse(
        gyldigFraOgMed = LocalDateTime.of(2000, 10, 1, 10, 10, 10),
        gyldigTilOgMed = LocalDateTime.of(2300, 10, 1, 10 , 10, 10),
        vegadresse = Vegadresse(
            adressenavn = gate,
            husnummer = husnr,
            husbokstav = null,
            postnummer = postnummer,
            kommunenummer = null,
            bydelsnummer = null
        ),
        utenlandskAdresse = null,
        metadata = mockMeta()
    )
    )

    internal fun Person.medUtlandAdresse(gateOgnr: String, postnummer: String, landkode: String) = this.copy(bostedsadresse = null, oppholdsadresse = Oppholdsadresse(
        gyldigFraOgMed = LocalDateTime.of(2000, 10, 1, 10, 10, 10),
        gyldigTilOgMed = LocalDateTime.of(2300, 10, 1, 10 , 10, 10),
        vegadresse = null,
        utenlandskAdresse = UtenlandskAdresse(
            landkode = landkode,
            adressenavnNummer = gateOgnr,
            postkode = postnummer,
            bySted = "UTLANDBY",
            bygningEtasjeLeilighet = null,
            postboksNummerNavn = null,
            regionDistriktOmraade = null),
        metadata = mockMeta()
        )
    )

    //subsitutt for tps-11000-gift og ekte
    internal fun createEnkelFamilie(personfnr: String, ektefellefnr: String) : PersonDataCollection {
        val person = createWith(fornavn = "ODIN ETTØYE", etternavn = "BALDER", fnr = personfnr)
            .medAdresse("FORUSBEEN", "0010", "2294")
        val ekte = createWith(fornavn = "THOR-DOPAPIR", etternavn = "RAGNAROK", fnr = ektefellefnr)

        return PersonDataCollection(
            forsikretPerson = person,
            sivilstandstype = Sivilstandstype.GIFT,
            gjenlevendeEllerAvdod = person,
            ektefellePerson = ekte
        )
    }

    //subsitutt for tps-30000-gift og ekte
    internal fun createAvdodFamilie(gjenlevFnr: String, avdodEktefelleFnr: String) : PersonDataCollection {
        val person = createWith(fornavn = "BAMSE ULUR", etternavn = "DOLLY", fnr = gjenlevFnr)
            .medAdresse("FORUSBEEN", "0010", "2294")
            .medKjoenn(KjoennType.KVINNE)
        val avdod = createWith(fornavn = "BAMSE LUR", etternavn = "MOMBALO", fnr = avdodEktefelleFnr, erDod =  true)

        return PersonDataCollection(
            forsikretPerson = person,
            sivilstandstype = Sivilstandstype.ENKE_ELLER_ENKEMANN,
            gjenlevendeEllerAvdod = avdod
        )
    }
}