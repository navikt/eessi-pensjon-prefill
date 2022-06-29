package no.nav.eessi.pensjon.prefill.person

import no.nav.eessi.pensjon.eux.model.sed.Adresse
import no.nav.eessi.pensjon.personoppslag.pdl.model.Folkeregistermetadata
import no.nav.eessi.pensjon.personoppslag.pdl.model.KontaktinformasjonForDoedsbo
import no.nav.eessi.pensjon.personoppslag.pdl.model.KontaktinformasjonForDoedsboAdresse
import no.nav.eessi.pensjon.personoppslag.pdl.model.KontaktinformasjonForDoedsboAdvokatSomKontakt
import no.nav.eessi.pensjon.personoppslag.pdl.model.KontaktinformasjonForDoedsboPersonSomKontakt
import no.nav.eessi.pensjon.personoppslag.pdl.model.KontaktinformasjonForDoedsboSkifteform
import no.nav.eessi.pensjon.personoppslag.pdl.model.Metadata
import no.nav.eessi.pensjon.personoppslag.pdl.model.Personnavn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PreutfyllDodsboAdresseTest {

    @Test
    fun `person som kontakt med navn`() {
        val kontaktinformasjonForDoedsbo = createKontaktinformasjonForDoedsbo(
            adresselinje1 = "Morgenstjerne Bakkemakker gate 351",
            adresselinje2 = "Leilighet 4, oppgang 5",
            postnummer = "6870",
            poststedsnavn = "Gokk"
        ).medPersonSomKontakt(fornavn = "Solsikke", mellomnavn = "Gaselle", etternavn = "Soltilbeder")

        val actual = preutfyllDodsboAdresse(kontaktinformasjonForDoedsbo, null)

        val expected = Adresse(
            gate = "Dødsbo v/Solsikke Gaselle Soltilbeder, Morgenstjerne Bakkemakker gate 351",
            bygning = "Leilighet 4, oppgang 5",
            by = "Gokk",
            postnummer = "6870",
            land = null
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `advokat som kontakt med organisasjonsnavn`() {
        val kontaktinformasjonForDoedsbo = createKontaktinformasjonForDoedsbo(
            adresselinje1 = "Tobben Tiedemanns gate 2",
            adresselinje2 = "Suite 456",
            postnummer = "7070",
            poststedsnavn = "Verdal"
        ).medAdvokatSomKontakt(
            fornavn = "Lurendreier",
            etternavn = "Fantestreker",
            organisasjon = "Bakke, Motbakke, Vannfall, Tørketid Advokater ANS"
        )
        val actual = preutfyllDodsboAdresse(kontaktinformasjonForDoedsbo, null)

        val expected = Adresse(
            gate = "Dødsbo v/Lurendreier Fantestreker, Bakke, Motbakke, Vannfall, Tørketid Advokater ANS, Tobben Tiedemanns gate 2",
            bygning = "Suite 456",
            by = "Verdal",
            postnummer = "7070",
            land = null
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `advokat som kontakt uten organisasjonsnavn`() {
        val kontaktinformasjonForDoedsbo = createKontaktinformasjonForDoedsbo(
            adresselinje1 = "c/o Mentor Nestor Arnesen",
            adresselinje2 = "Barbakken 4",
            postnummer = "7080",
            poststedsnavn = "Leikanger"
        ).medAdvokatSomKontakt(
            fornavn = "Nestor",
            mellomnavn = "Mentor",
            etternavn = "Mortensen"
        )

        val actual = preutfyllDodsboAdresse(kontaktinformasjonForDoedsbo, null)

        val expected = Adresse(
            gate = "Dødsbo v/Nestor Mentor Mortensen, c/o Mentor Nestor Arnesen",
            bygning = "Barbakken 4",
            by = "Leikanger",
            postnummer = "7080",
            land = null
        )
        assertEquals(expected, actual)
    }

    fun createKontaktinformasjonForDoedsbo(
        adresselinje1: String,
        adresselinje2: String? = null,
        postnummer: String,
        poststedsnavn: String
    ) = KontaktinformasjonForDoedsbo(
            adresse = KontaktinformasjonForDoedsboAdresse(
                adresselinje1 = adresselinje1,
                adresselinje2 = adresselinje2,
                postnummer = postnummer,
                poststedsnavn = poststedsnavn
            ),
            attestutstedelsesdato = LocalDate.now(),
            folkeregistermetadata = Folkeregistermetadata(null),
            metadata = Metadata(
                endringer = emptyList(),
                historisk = false,
                master = "",
                opplysningsId = ""
            ),
            skifteform = KontaktinformasjonForDoedsboSkifteform.OFFENTLIG
        )

    fun KontaktinformasjonForDoedsbo.medPersonSomKontakt(
        fornavn: String,
        mellomnavn: String? = null,
        etternavn: String
    ) = this.copy(
            personSomKontakt = KontaktinformasjonForDoedsboPersonSomKontakt(
                personnavn = Personnavn(
                    fornavn = fornavn,
                    mellomnavn = mellomnavn,
                    etternavn = etternavn
                )
            )
        )

    fun KontaktinformasjonForDoedsbo.medAdvokatSomKontakt(
        fornavn: String,
        mellomnavn: String? = null,
        etternavn: String,
        organisasjon: String? = null
    ) = this.copy(
            advokatSomKontakt = KontaktinformasjonForDoedsboAdvokatSomKontakt(
                personnavn = Personnavn(
                    fornavn = fornavn,
                    mellomnavn = mellomnavn,
                    etternavn = etternavn
                ),
                organisasjonsnavn = organisasjon
            )
        )

}