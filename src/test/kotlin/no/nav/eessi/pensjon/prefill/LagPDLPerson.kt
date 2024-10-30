package no.nav.eessi.pensjon.prefill

import no.nav.eessi.pensjon.personoppslag.pdl.model.*
import no.nav.eessi.pensjon.personoppslag.pdl.model.Bostedsadresse
import no.nav.eessi.pensjon.personoppslag.pdl.model.Familierelasjonsrolle
import no.nav.eessi.pensjon.personoppslag.pdl.model.ForelderBarnRelasjon
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentGruppe.*
import no.nav.eessi.pensjon.personoppslag.pdl.model.Kontaktadresse
import no.nav.eessi.pensjon.personoppslag.pdl.model.KontaktadresseType
import no.nav.eessi.pensjon.personoppslag.pdl.model.UtenlandskAdresseIFrittFormat
import no.nav.eessi.pensjon.personoppslag.pdl.model.Vegadresse
import no.nav.eessi.pensjon.shared.person.Fodselsnummer
import no.nav.eessi.pensjon.shared.person.FodselsnummerGenerator
import java.time.LocalDate
import java.time.LocalDateTime


class LagPDLPerson {
    companion object {
        fun lagPerson(fnrEllerNpid: String = FodselsnummerGenerator.generateFnrForTest(60), fornavn: String = "OLE", etternavn: String = "OLSEN", land: String = "NOR", kjoennType: KjoennType = KjoennType.MANN, erDod: Boolean? = false): PdlPerson {
            val personfnr = Fodselsnummer.fra(fnrEllerNpid)
            val fdatoaar =  if (erDod != null && erDod == true || personfnr?.erNpid == true) LocalDate.of(1921, 7, 12) else personfnr?.getBirthDate()
            val doeadfall = if (erDod != null && erDod == true || personfnr?.erNpid == true) Doedsfall(LocalDate.of(2020, 10, 1), null, mockMeta()) else null
            return PdlPerson(
                identer = if(personfnr?.erNpid == true) listOf(IdentInformasjon(fnrEllerNpid, NPID))
                else listOf(IdentInformasjon(fnrEllerNpid, FOLKEREGISTERIDENT)),
                navn = Navn(fornavn, null, etternavn, null, null, null, mockMeta()),
                adressebeskyttelse = emptyList(),
                bostedsadresse = null,
                oppholdsadresse = null,
                statsborgerskap = listOf(Statsborgerskap(land, LocalDate.of(2000, 10, 1), LocalDate.of(2300, 10, 1), mockMeta())),
                foedsel = Foedsel(fdatoaar, "NOR", null, null, null , mockMeta()),
                geografiskTilknytning = null,
                kjoenn = Kjoenn(kjoennType, null, mockMeta()),
                doedsfall = doeadfall,
                forelderBarnRelasjon = emptyList(),
                sivilstand = emptyList(),
                kontaktadresse = null,
                utenlandskIdentifikasjonsnummer = emptyList()
            )
        }

        internal fun mockMeta(registrert: LocalDateTime = LocalDateTime.of(2010, 4, 2, 13, 21, 32)) : Metadata {
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

        fun PdlPerson.medBarn(barnfnr: String): PdlPerson {
                val minRolle = familieRolle(this)
                val list = mutableListOf<ForelderBarnRelasjon>()
                list.addAll(this.forelderBarnRelasjon)
                list.add(ForelderBarnRelasjon(
                    relatertPersonsIdent = barnfnr,
                    relatertPersonsRolle = Familierelasjonsrolle.BARN,
                    minRolleForPerson = minRolle,
                    metadata = mockMeta()
                )
                )
                return this.copy(forelderBarnRelasjon = list)
        }

        fun PdlPerson.medForeldre(foreldre: PdlPerson): PdlPerson {
            val foreldreRolle = familieRolle(foreldre)
            val foreldrefnr = foreldre.identer.firstOrNull { it.gruppe == FOLKEREGISTERIDENT || it.gruppe == NPID }?.ident
            val list = mutableListOf<ForelderBarnRelasjon>()
            list.addAll(this.forelderBarnRelasjon)
            list.add(ForelderBarnRelasjon(
                relatertPersonsIdent = foreldrefnr!!,
                relatertPersonsRolle = foreldreRolle,
                minRolleForPerson = Familierelasjonsrolle.BARN,
                metadata = mockMeta()
            )
            )
            return this.copy(forelderBarnRelasjon = list)
        }

        private fun familieRolle(person: PdlPerson) : Familierelasjonsrolle {
            return when(person.kjoenn?.kjoenn) {
                KjoennType.MANN -> Familierelasjonsrolle.FAR
                KjoennType.KVINNE -> Familierelasjonsrolle.MOR
                else -> Familierelasjonsrolle.MEDMOR
            }
        }

        fun PdlPerson.medKontaktadresseUtland() = this.copy(kontaktadresse = Kontaktadresse(
            coAdressenavn = "CoAdressenavn",
            folkeregistermetadata = null,
            gyldigFraOgMed = LocalDateTime.of(2000, 10, 1, 10, 10, 10),
            gyldigTilOgMed = LocalDateTime.of(2300, 10, 1, 10 , 10, 10),
            metadata = mockMeta(),
            type = KontaktadresseType.Utland,
            utenlandskAdresse = null,
            utenlandskAdresseIFrittFormat = UtenlandskAdresseIFrittFormat(
                adresselinje1 = "Adresselinje 1",
                adresselinje2 = "Adresselinje 2",
                adresselinje3 = "Adresselinje 3",
                byEllerStedsnavn = null,
                landkode = "SWE",
                postkode = null
            ),
            vegadresse = null,
            postadresseIFrittFormat = null
        ))


        fun PdlPerson.medAdresse(gate: String?) = this.copy(bostedsadresse = Bostedsadresse(
                gyldigFraOgMed = LocalDateTime.of(2000, 10, 1, 10, 10, 10),
                gyldigTilOgMed = LocalDateTime.of(2300, 10, 1, 10 , 10, 10),
                vegadresse = Vegadresse(
                    adressenavn = gate,
                    husnummer = "12",
                    husbokstav = null,
                    postnummer = "0101",
                    bydelsnummer = null,
                    kommunenummer = null
                ),
                utenlandskAdresse = null,
                metadata = mockMeta()
        )
        )

        fun createPersonMedEktefellePartner(personPersonnr: String, ektefellePersonnr: String, type: Sivilstandstype): Pair<PdlPerson, PdlPerson> {

            val person = lagPerson(personPersonnr, "Ola", "Testbruker")
            val ektefelle = lagPerson(ektefellePersonnr, "Jonna", "Dolla", kjoennType = KjoennType.KVINNE)

            val nyPerson = person.copy(sivilstand = listOf(Sivilstand(type, LocalDate.of(2000,10, 1), ektefellePersonnr, mockMeta())))
            val nyEktefell = ektefelle.copy(sivilstand = listOf(Sivilstand(type, LocalDate.of(2000,10, 1), personPersonnr, mockMeta())))

            return Pair(nyPerson, nyEktefell)
        }
    }
}