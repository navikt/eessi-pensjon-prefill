package no.nav.eessi.pensjon.prefill.person

import no.nav.eessi.pensjon.eux.model.sed.Adresse
import no.nav.eessi.pensjon.personoppslag.pdl.model.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PrefillDodsboAdresseTest {

    @Test
    fun `person som kontakt med navn`() {
        val kontaktinformasjonForDoedsbo = createKontaktinformasjonForDoedsbo(
            adresselinje1 = "Morgenstjerne Bakkemakker gate 351",
            adresselinje2 = "Leilighet 4, oppgang 5",
            postnummer = "6870",
            poststedsnavn = "Gokk"
        ).medPersonSomKontakt(fornavn = "Solsikke", mellomnavn = "Gaselle", etternavn = "Soltilbeder")

        val actual = preutfyllDodsboAdresse(kontaktinformasjonForDoedsbo, null, dummyProvider)

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
    fun `person som kontakt med navn med for lang adresse skal logge error og kjøre videre med 155 tegn adresse`() {
        val kontaktinformasjonForDoedsbo = createKontaktinformasjonForDoedsbo(
            adresselinje1 = "Morgenstjerne Bakkemakker gate 351 her kommer det en veldig lang adresse som er over 155 tegn og som skal avsluttes på 155 eller noe sånt",
            adresselinje2 = "Leilighet 4, oppgang 5",
            postnummer = "6870",
            poststedsnavn = "Gokk"
        ).medPersonSomKontakt(fornavn = "Solsikke", mellomnavn = "Gaselle", etternavn = "Soltilbeder")

        val actual = preutfyllDodsboAdresse(kontaktinformasjonForDoedsbo, null, dummyProvider)

        val expected = Adresse(
            gate = "Dødsbo v/Solsikke Gaselle Soltilbeder, Morgenstjerne Bakkemakker gate 351 her kommer det en veldig lang adresse som er over 155 tegn og som skal avsluttes ",
            bygning = "Leilighet 4, oppgang 5",
            by = "Gokk",
            postnummer = "6870",
            land = null
        )
        assertEquals(expected, actual)
    }

    private val dummyProvider: (identifikasjonsnummer: String) -> Personnavn =
        { throw AssertionError("Denne skal ikke kalles i testen") }

    @Test
    fun `person som kontakt med identifikasjonsnummer`() {
        val kontaktinformasjonForDoedsbo = createKontaktinformasjonForDoedsbo(
            adresselinje1 = "Morgenstjerne Bakkemakker gate 351",
            adresselinje2 = "Leilighet 4, oppgang 5",
            postnummer = "6870",
            poststedsnavn = "Gokk"
        ).medPersonSomKontakt(identifikasjonsnummer = "29118599999")

        val actual = preutfyllDodsboAdresse(kontaktinformasjonForDoedsbo, null, { identifikasjonsnummer ->
            if (identifikasjonsnummer == "29118599999") {
                Personnavn("Navn", "Fra", "Pdl")
            } else {
                throw AssertionError("Fikk feil identifikasjonsnummer")
            }
        })

        val expected = Adresse(
            gate = "Dødsbo v/Navn Fra Pdl, Morgenstjerne Bakkemakker gate 351",
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
        val actual = preutfyllDodsboAdresse(kontaktinformasjonForDoedsbo, null, dummyProvider)

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

        val actual = preutfyllDodsboAdresse(kontaktinformasjonForDoedsbo, null, dummyProvider)

        val expected = Adresse(
            gate = "Dødsbo v/Nestor Mentor Mortensen, c/o Mentor Nestor Arnesen",
            bygning = "Barbakken 4",
            by = "Leikanger",
            postnummer = "7080",
            land = null
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `organisasjon som kontakt uten kontaktperson`() {
        val kontaktinformasjonForDoedsbo = createKontaktinformasjonForDoedsbo(
            adresselinje1 = "Ærlighetvarerlengstveien 65",
            postnummer = "7080",
            poststedsnavn = "Leikanger"
        ).medOrganisasjonSomKontakt(
            organisasjonsnavn = "Vi Tar Ikke Arven Fra Noen AS"
        )

        val actual = preutfyllDodsboAdresse(kontaktinformasjonForDoedsbo, null, dummyProvider)

        val expected = Adresse(
            gate = "Dødsbo v/Vi Tar Ikke Arven Fra Noen AS, Ærlighetvarerlengstveien 65",
            by = "Leikanger",
            postnummer = "7080",
            land = null
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `organisasjon som kontakt med kontaktperson`() {
        val kontaktinformasjonForDoedsbo = createKontaktinformasjonForDoedsbo(
            adresselinje1 = "c/o Vi Lager Selskap For Alt AS",
            adresselinje2 = "Bokstaver Blirtilordveien 14A",
            postnummer = "7080",
            poststedsnavn = "Leikanger"
        ).medOrganisasjonSomKontakt(
            organisasjonsnavn = "Bare Ren og Skjær Veldedighet",
            fornavn = "Fetter",
            mellomnavn = "Anton",
            etternavn = "Værsågo-Takkskaruha"
        )

        val actual = preutfyllDodsboAdresse(kontaktinformasjonForDoedsbo, null, dummyProvider)

        val expected = Adresse(
            gate = "Dødsbo v/Fetter Anton Værsågo-Takkskaruha, Bare Ren og Skjær Veldedighet, c/o Vi Lager Selskap For Alt AS",
            bygning = "Bokstaver Blirtilordveien 14A",
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
    fun KontaktinformasjonForDoedsbo.medPersonSomKontakt(identifikasjonsnummer: String) =
        this.copy(
            personSomKontakt = KontaktinformasjonForDoedsboPersonSomKontakt(
                identifikasjonsnummer = identifikasjonsnummer
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


    private fun KontaktinformasjonForDoedsbo.medOrganisasjonSomKontakt(
        organisasjonsnavn: String
    ) = this.copy(
     organisasjonSomKontakt = KontaktinformasjonForDoedsboOrganisasjonSomKontakt(
         organisasjonsnavn = organisasjonsnavn
        )
    )

    private fun KontaktinformasjonForDoedsbo.medOrganisasjonSomKontakt(
        organisasjonsnavn: String,
        fornavn: String,
        mellomnavn: String? = null,
        etternavn: String
    ) = this.copy(
     organisasjonSomKontakt = KontaktinformasjonForDoedsboOrganisasjonSomKontakt(
         kontaktperson = Personnavn(
             fornavn = fornavn,
             mellomnavn = mellomnavn,
             etternavn = etternavn
         ),
         organisasjonsnavn = organisasjonsnavn
        )
    )

}
