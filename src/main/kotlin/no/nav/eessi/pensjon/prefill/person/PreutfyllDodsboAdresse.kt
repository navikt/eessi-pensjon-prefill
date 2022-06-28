package no.nav.eessi.pensjon.prefill.person

import no.nav.eessi.pensjon.eux.model.sed.Adresse
import no.nav.eessi.pensjon.personoppslag.pdl.model.KontaktinformasjonForDoedsbo

internal fun preutfyllDodsboAdresse(prefillPDLAdresse: PrefillPDLAdresse, kontaktinformasjonForDoedsbo: KontaktinformasjonForDoedsbo): Adresse {
    val adresse = kontaktinformasjonForDoedsbo.adresse
    return Adresse(
        gate = adresse.adresselinje1.replace("\n", " "),
        bygning = adresse.adresselinje2?.replace("\n", " "),
        by = adresse.poststedsnavn,
        postnummer = adresse.postnummer,
        land = prefillPDLAdresse.hentLandkode(adresse.landkode)
    )
}