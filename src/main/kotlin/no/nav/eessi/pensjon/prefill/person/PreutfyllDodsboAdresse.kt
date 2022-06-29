package no.nav.eessi.pensjon.prefill.person

import no.nav.eessi.pensjon.eux.model.sed.Adresse
import no.nav.eessi.pensjon.personoppslag.pdl.model.KontaktinformasjonForDoedsbo
import no.nav.eessi.pensjon.personoppslag.pdl.model.Personnavn

internal fun preutfyllDodsboAdresse( kontaktinformasjonForDoedsbo: KontaktinformasjonForDoedsbo, landkode: String?): Adresse {
    val adresse = kontaktinformasjonForDoedsbo.adresse
    val sammensattInfoForKontakt =
        when {
            kontaktinformasjonForDoedsbo.personSomKontakt != null -> {
                (joinNavn(kontaktinformasjonForDoedsbo.personSomKontakt!!.personnavn!!)
                        + ", " + adresse.adresselinje1.replace("\n", " "))
            }
            kontaktinformasjonForDoedsbo.advokatSomKontakt != null -> {
                val advokat = kontaktinformasjonForDoedsbo.advokatSomKontakt!!
                (joinNavn(advokat.personnavn)
                        + if (!advokat.organisasjonsnavn.isNullOrBlank()) ", " + advokat.organisasjonsnavn else { "" }
                        + ", " + adresse.adresselinje1.replace("\n", " "))
            }
            else -> {
                val organisasjon = kontaktinformasjonForDoedsbo.organisasjonSomKontakt!!
                (if (organisasjon.kontaktperson != null) joinNavn(organisasjon.kontaktperson!!) + ", " else { "" }
                        + organisasjon.organisasjonsnavn
                        + ", " + adresse.adresselinje1.replace("\n", " "))
            }
        }
    return Adresse(
        gate = "Dødsbo v/$sammensattInfoForKontakt",
        bygning = adresse.adresselinje2?.replace("\n", " "),
        by = adresse.poststedsnavn,
        postnummer = adresse.postnummer,
        land = landkode
    )
}

private fun joinNavn(personnavn: Personnavn) =
    (personnavn.fornavn + if (!personnavn.mellomnavn.isNullOrBlank()) " " + personnavn.mellomnavn else { "" } + " " + personnavn.etternavn)
