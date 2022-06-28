package no.nav.eessi.pensjon.prefill.person

import no.nav.eessi.pensjon.eux.model.sed.Adresse
import no.nav.eessi.pensjon.personoppslag.pdl.model.KontaktinformasjonForDoedsbo

internal fun preutfyllDodsboAdresse( kontaktinformasjonForDoedsbo: KontaktinformasjonForDoedsbo, landkode: String?): Adresse {
    val adresse = kontaktinformasjonForDoedsbo.adresse
    val personnavn = kontaktinformasjonForDoedsbo.personSomKontakt!!.personnavn!!
    return Adresse(
        gate = "Dødsbo v/"
                + personnavn.fornavn
                + if (personnavn.mellomnavn != null) { " " + personnavn.mellomnavn } else { "" }
                + " " + personnavn.etternavn
                + ", " + adresse.adresselinje1.replace("\n", " "),
        bygning = adresse.adresselinje2?.replace("\n", " "),
        by = adresse.poststedsnavn,
        postnummer = adresse.postnummer,
        land = landkode
    )
}