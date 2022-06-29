package no.nav.eessi.pensjon.prefill.person

import no.nav.eessi.pensjon.eux.model.sed.Adresse
import no.nav.eessi.pensjon.personoppslag.pdl.model.KontaktinformasjonForDoedsbo
import no.nav.eessi.pensjon.personoppslag.pdl.model.Personnavn
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal fun preutfyllDodsboAdresse( kontaktinformasjonForDoedsbo: KontaktinformasjonForDoedsbo, landkode: String?, personnavnProvider: (identifikasjonsnummer: String) -> Personnavn): Adresse {

    val logger: Logger by lazy { LoggerFactory.getLogger("no.nav.eessi.pensjon.prefill.person.preutfyllDodsboAdresse") }

    val adresse = kontaktinformasjonForDoedsbo.adresse
    val sammensattInfoForKontakt =
        when {
            kontaktinformasjonForDoedsbo.personSomKontakt != null -> {
                val personnavn: Personnavn =
                    if (kontaktinformasjonForDoedsbo.personSomKontakt!!.personnavn != null) {
                        kontaktinformasjonForDoedsbo.personSomKontakt!!.personnavn!!
                    } else {
                        personnavnProvider(kontaktinformasjonForDoedsbo.personSomKontakt!!.identifikasjonsnummer!!)
                    }
                (joinNavn(personnavn)
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
    val gateFeltVerdi = "Dødsbo v/$sammensattInfoForKontakt"
    val gateFeltVerdiTrunkert =
        if (gateFeltVerdi.length <= 155) {
            gateFeltVerdi
        } else {
            val trunkert = gateFeltVerdi.substring(0, 155)
            logger.error("KontaktinformasjonForDoedsbo-felter for lange for SED sitt gate-felt, verdien er avkortet til 155 tegn.\n" +
                    "Person: ${kontaktinformasjonForDoedsbo.personSomKontakt}\n" +
                    "Advokat: ${kontaktinformasjonForDoedsbo.advokatSomKontakt}\n" +
                    "Organisasjon: ${kontaktinformasjonForDoedsbo.organisasjonSomKontakt}\n" +
                    "Adresse: ${kontaktinformasjonForDoedsbo.adresse}\n" +
                    "Resultat: ${trunkert}\n" +
                    "Feilen logges - vi fortsetter med 155 tegn."
            )
            trunkert
        }

    return Adresse(
        gate = gateFeltVerdiTrunkert,
        bygning = adresse.adresselinje2?.replace("\n", " "),
        by = adresse.poststedsnavn,
        postnummer = adresse.postnummer,
        land = landkode
    )
}

private fun joinNavn(personnavn: Personnavn) =
    (personnavn.fornavn + if (!personnavn.mellomnavn.isNullOrBlank()) " " + personnavn.mellomnavn else { "" } + " " + personnavn.etternavn)
