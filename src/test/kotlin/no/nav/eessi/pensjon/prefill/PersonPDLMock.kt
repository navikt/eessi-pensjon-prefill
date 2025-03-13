package no.nav.eessi.pensjon.prefill

import no.nav.eessi.pensjon.personoppslag.pdl.model.*
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object PersonPDLMock {
    internal fun createWith(landkoder: Boolean = true, fornavn: String = "Test", etternavn: String = "Testesen", fnr: String = "3123", aktoerid: String = "3213", erDod: Boolean? = false, metadata: Metadata = mockMeta()) :PdlPerson {
        val folkeregisterMetadata = Folkeregistermetadata(gyldighetstidspunkt = LocalDateTime.parse("2021-02-16T10:15:30"))
        val fdatoaar =  if (erDod != null && erDod == true) LocalDate.of(1921, 7, 12) else LocalDate.of(1988, 7, 12)
            val doeadfall = if (erDod != null && erDod == true) Doedsfall(LocalDate.of(2020, 10, 1), null, metadata) else null
            val kommuneLandkode = when(landkoder) {
                true -> "026123"
                else -> null
            }
            return PdlPerson(
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
                listOf(
                    Statsborgerskap(
                    "NOR",
                    LocalDate.of(1980, 10 , 1),
                    LocalDate.of(2300, 10, 1),
                    metadata
                ),
                    Statsborgerskap(
                        "SWE",
                        LocalDate.of(1980, 10, 1),
                        LocalDate.of(2300, 10, 1),
                        metadata
                    )
                ),
                Foedselsdato(
                    null,
                    fdatoaar.toString(),
                    folkeregisterMetadata,
                    metadata
                ).also { println("Foedselsdato: ${it.toJsonSkipEmpty()}") },
                foedested = null,
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
            null,
                null,
                listOf(UtenlandskIdentifikasjonsnummer(
                    "123123123",
                    "SWE",
                    false,
                    metadata = mockMeta(
                ))
            ))
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

    internal fun PdlPerson.medSivilstand(person: PdlPerson): PdlPerson {
        val ident = person.identer.firstOrNull { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }?.ident
        return this.copy(sivilstand = listOf(
            Sivilstand(Sivilstandstype.GIFT, LocalDate.of (2010, 1,10), ident, mockMeta())
        ))
    }

    internal fun PdlPerson.medFodsel(date: LocalDate): PdlPerson = this.copy(foedselsdato = Foedselsdato(null, date.format(
        DateTimeFormatter.ofPattern("yyyy-MM-dd")), null, mockMeta()))
//    internal fun PdlPerson.medFodsel(date: LocalDate, land: String? = "NOR")  = this.copy(foedsel = Foedsel(date, land,null, null, null, mockMeta()))

    internal fun PdlPerson.medKjoenn(type: KjoennType) = this.copy(kjoenn = Kjoenn(type, null, mockMeta()))

    internal fun PdlPerson.medBeskyttelse(gradering: AdressebeskyttelseGradering) = this.copy(adressebeskyttelse = listOf(gradering))

    internal fun PdlPerson.medForeldre(foreldre: PdlPerson): PdlPerson {
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

    internal fun PdlPerson.medBarn(barn: PdlPerson): PdlPerson {
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
            gjenlevendeEllerAvdod = enkePerson,
            barnPersonList = listOfNotNull(
                barn1,
                barn2
            ))
    }

    private fun familieRolle(person: PdlPerson) : Familierelasjonsrolle {
        return when(person.kjoenn?.kjoenn) {
            KjoennType.MANN -> Familierelasjonsrolle.FAR
            KjoennType.KVINNE -> Familierelasjonsrolle.MOR
            else -> Familierelasjonsrolle.MEDMOR
        }
    }

    internal fun PdlPerson.medAdresse(gate: String, postnummer: String, husnr: String? = "") = this.copy(
        bostedsadresse = Bostedsadresse(
            gyldigFraOgMed = LocalDateTime.of(2000, 10, 1, 10, 10, 10),
            gyldigTilOgMed = LocalDateTime.of(2300, 10, 1, 10, 10, 10),
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

    internal fun PdlPerson.medDodsboAdresse(
        kontaktpersonFornavn: String,
        kontaktpersonEtternavn: String,
        adresse1: String,
        bygning: String,
        postnummer: String,
        poststednavn: String = "",
        landkode: String
    ) = this.copy(
        kontaktinformasjonForDoedsbo = KontaktinformasjonForDoedsbo(
            personSomKontakt = KontaktinformasjonForDoedsboPersonSomKontakt(
                personnavn = Personnavn(fornavn = kontaktpersonFornavn, etternavn = kontaktpersonEtternavn)
            ),
            adresse = KontaktinformasjonForDoedsboAdresse(
                adresselinje1 = adresse1,
                adresselinje2 = bygning,
                landkode = landkode,
                postnummer = postnummer,
                poststedsnavn = poststednavn
            ),
            attestutstedelsesdato = LocalDate.of(2020, 10, 1),
            folkeregistermetadata = Folkeregistermetadata(null),
            skifteform = KontaktinformasjonForDoedsboSkifteform.ANNET,
            metadata = mockMeta(),
        )
    )


    internal fun PdlPerson.medUtlandAdresse(
        gateOgnr: String,
        postnummer: String,
        landkode: String,
        bygning: String? = "bygning",
        region: String? = "region",
        postboks: String? = "po.box",
        bySted: String? = "UTLANDBY"
    ) = this.copy(bostedsadresse = null, oppholdsadresse = Oppholdsadresse(
        gyldigFraOgMed = LocalDateTime.of(2000, 10, 1, 10, 10, 10),
        gyldigTilOgMed = LocalDateTime.of(2300, 10, 1, 10 , 10, 10),
        vegadresse = null,
        utenlandskAdresse = UtenlandskAdresse(
            landkode = landkode,
            adressenavnNummer = gateOgnr,
            postkode = postnummer,
            bySted = bySted,
            bygningEtasjeLeilighet = bygning,
            postboksNummerNavn = postboks,
            regionDistriktOmraade = region
        ),
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
            .medAdresse("Avdødadresse", "1111", "2222")

        return PersonDataCollection(
            forsikretPerson = person,
            gjenlevendeEllerAvdod = avdod
        )
    }

    internal fun createAvdodFamilieMedDødsboadresse(gjenlevFnr: String, avdodEktefelleFnr: String) : PersonDataCollection {
        val person = createWith(fornavn = "BAMSE ULUR", etternavn = "DOLLY", fnr = gjenlevFnr)
            .medAdresse("FORUSBEEN", "0010", "2294")
            .medKjoenn(KjoennType.KVINNE)
        val avdod = createWith(fornavn = "BAMSE LUR", etternavn = "MOMBALO", fnr = avdodEktefelleFnr, erDod =  true)
            .medDodsboAdresse("Michelle", "Etternavn", "Avdødadresse", "adresse 2", "1111", "2222", "NOR")

        return PersonDataCollection(
            forsikretPerson = person,
            gjenlevendeEllerAvdod = avdod
        )
    }
}