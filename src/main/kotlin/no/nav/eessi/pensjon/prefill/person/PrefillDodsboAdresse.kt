package no.nav.eessi.pensjon.prefill.person

import no.nav.eessi.pensjon.eux.model.sed.Adresse
import no.nav.eessi.pensjon.personoppslag.pdl.model.KontaktinformasjonForDoedsbo
import no.nav.eessi.pensjon.personoppslag.pdl.model.Navn
import no.nav.eessi.pensjon.personoppslag.pdl.model.Personnavn
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillDodsboAdresse {

    val logger: Logger by lazy { LoggerFactory.getLogger(PrefillDodsboAdresse::class.java) }

    internal fun preutfyllDodsboAdresse(
        kontaktinfo: KontaktinformasjonForDoedsbo,
        landkode: String?,
        personnavnProvider: (identifikasjonsnummer: String) -> Navn
    ): Adresse {

        val gateFeltVerdiFullLengde = "Dødsbo v/" +
            when {
                kontaktinfo.personSomKontakt != null -> settSammenPersonInfo(kontaktinfo, personnavnProvider)
                kontaktinfo.advokatSomKontakt != null -> settSammenAdvokatInfo(kontaktinfo)
                else -> settSammenOrganisasjonInfo(kontaktinfo)
            }

        if (gateFeltVerdiFullLengde.length > 155) {
            logger.error(
                "KontaktinformasjonForDoedsbo-felter for lange for SED sitt gate-felt, verdien er avkortet til 155 tegn.\n" +
                        "Person: ${kontaktinfo.personSomKontakt}\n" +
                        "Advokat: ${kontaktinfo.advokatSomKontakt}\n" +
                        "Organisasjon: ${kontaktinfo.organisasjonSomKontakt}\n" +
                        "Adresse: ${kontaktinfo.adresse}\n" +
                        "Resultat: ${maks155tegn(gateFeltVerdiFullLengde)}\n" +
                        "Feilen logges - vi fortsetter med 155 tegn."
            )
        }

        val gateFeltVerdi = maks155tegn(gateFeltVerdiFullLengde)

        return Adresse(
            gate = gateFeltVerdi,
            bygning = kontaktinfo.adresse.adresselinje2?.replace("\n", " "),
            by = kontaktinfo.adresse.poststedsnavn,
            postnummer = kontaktinfo.adresse.postnummer,
            land = landkode
        )
    }

    private fun maks155tegn(gateFeltVerdi: String) =
        if (gateFeltVerdi.length <= 155) gateFeltVerdi else gateFeltVerdi.substring(0, 155)

    private fun settSammenPersonInfo(
        kontaktinfo: KontaktinformasjonForDoedsbo,
        personnavnProvider: (identifikasjonsnummer: String) -> Navn
    ): String {
        val person = kontaktinfo.personSomKontakt!!
        val joinetNavn: String =
            if (person.personnavn != null) {
                joinNavn(person.personnavn!!)
            } else {
                val navn = personnavnProvider(person.identifikasjonsnummer!!)
                (navn.fornavn + if (!navn.mellomnavn.isNullOrBlank()) " " + navn.mellomnavn else { "" } + " " + navn.etternavn)
            }
        return (joinetNavn + ", " + kontaktinfo.adresse.adresselinje1.utenLinjeskift())
    }

    private fun settSammenAdvokatInfo(kontaktinfo: KontaktinformasjonForDoedsbo): String {
        val advokat = kontaktinfo.advokatSomKontakt!!
        return (joinNavn(advokat.personnavn)
                + if (!advokat.organisasjonsnavn.isNullOrBlank()) ", " + advokat.organisasjonsnavn else { "" }
                + ", " + kontaktinfo.adresse.adresselinje1.utenLinjeskift())
    }

    private fun settSammenOrganisasjonInfo(kontaktinfo: KontaktinformasjonForDoedsbo): String {
        val organisasjon = kontaktinfo.organisasjonSomKontakt!!
        return (if (organisasjon.kontaktperson != null) joinNavn(organisasjon.kontaktperson!!) + ", " else { "" }
                        + organisasjon.organisasjonsnavn
                        + ", " + kontaktinfo.adresse.adresselinje1.utenLinjeskift())
    }

    private fun joinNavn(personnavn: Personnavn) =
        (personnavn.fornavn
                + if (!personnavn.mellomnavn.isNullOrBlank()) " " + personnavn.mellomnavn else { "" } + " " + personnavn.etternavn)
}

private fun String.utenLinjeskift() = this.replace("\n", " ")
