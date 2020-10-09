package no.nav.eessi.pensjon.fagmodul.prefill

import no.nav.eessi.pensjon.fagmodul.prefill.tps.NavFodselsnummer
import no.nav.eessi.pensjon.fagmodul.sedmodel.Person
import no.nav.eessi.pensjon.fagmodul.sedmodel.PinItem
import no.nav.eessi.pensjon.fagmodul.sedmodel.StatsborgerskapItem
import no.nav.eessi.pensjon.utils.convertToXMLocal
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*

class LagTPSPerson {

    companion object {

        fun lagPerson(foreldersPin: String, fornavn: String, etternavn: String, fdato: String? = null, someInstitutionId: String? = null, someIntitutionNavn: String? = null) =
                Person(
                        pin = listOf(PinItem(
                                institusjonsnavn = someIntitutionNavn,
                                institusjonsid = someInstitutionId,
                                identifikator = foreldersPin,
                                land = "NO")),
                        statsborgerskap = listOf(StatsborgerskapItem(land = "NO")),
                        etternavn = etternavn,
                        fornavn = fornavn,
                        kjoenn = "M",
                        foedselsdato = fdato,
                        fornavnvedfoedsel = null)

        fun lagTPSBruker(foreldersPin: String, fornavn: String? = null, etternavn: String? = null) =
                Bruker()
                        .withPersonnavn(Personnavn()
                                .withEtternavn(etternavn)
                                .withFornavn(fornavn))
                        .withKjoenn(Kjoenn().withKjoenn(Kjoennstyper().withValue("M")))
                        .withAktoer(PersonIdent().withIdent(NorskIdent().withIdent(foreldersPin)))
                        .withStatsborgerskap(Statsborgerskap().withLand(Landkoder().withValue("NOR")))

        fun Bruker.medBarn(barnetsPin: String): Bruker =
                this
                        .withHarFraRolleI(Familierelasjon()
                                .withTilRolle(Familierelasjoner()
                                        .withValue("BARN"))
                                .withTilPerson(no.nav.tjeneste.virksomhet.person.v3.informasjon.Person()
                                        .withAktoer(PersonIdent()
                                                .withIdent(NorskIdent()
                                                        .withIdent(barnetsPin)))))

        fun Bruker.medAdresse(gate: String?, by: String? , land: String?): Bruker {
            val bosted = Bostedsadresse()
            val gateadresse = Gateadresse()
            gateadresse.gatenavn = gate

            bosted.strukturertAdresse = gateadresse
            return this.withBostedsadresse(bosted)

        }

        fun createPersonMedEktefellePartner(personPersonnr: String, ektefellePersonnr: String, relasjonType: String): Pair<Bruker, Bruker> {
            val personfnr = NavFodselsnummer(personPersonnr)
            val ektefnr = NavFodselsnummer(ektefellePersonnr)

            val ektefelle = lagTPSBruker(ektefellePersonnr, "Jonna", "Dolla")
            val person = lagTPSBruker(personPersonnr, "Ola", "Testbruker")

            person.withHarFraRolleI(Familierelasjon().withTilRolle(Familierelasjoner().withValue(relasjonType)).withTilPerson(ektefelle))
            person.withFoedselsdato(Foedselsdato().withFoedselsdato(convertToXMLocal(personfnr.getBirthDate())))

            ektefelle.withHarFraRolleI(Familierelasjon().withTilRolle(Familierelasjoner().withValue(relasjonType)).withTilPerson(person))
            ektefelle.withFoedselsdato(Foedselsdato().withFoedselsdato(convertToXMLocal(ektefnr.getBirthDate())))

            return Pair<Bruker, Bruker>(person, ektefelle)
        }
    }
}