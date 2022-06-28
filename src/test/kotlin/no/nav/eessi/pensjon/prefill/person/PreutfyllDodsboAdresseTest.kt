package no.nav.eessi.pensjon.prefill.person

import no.nav.eessi.pensjon.eux.model.sed.Adresse
import no.nav.eessi.pensjon.personoppslag.pdl.model.Folkeregistermetadata
import no.nav.eessi.pensjon.personoppslag.pdl.model.KontaktinformasjonForDoedsbo
import no.nav.eessi.pensjon.personoppslag.pdl.model.KontaktinformasjonForDoedsboAdresse
import no.nav.eessi.pensjon.personoppslag.pdl.model.KontaktinformasjonForDoedsboPersonSomKontakt
import no.nav.eessi.pensjon.personoppslag.pdl.model.KontaktinformasjonForDoedsboSkifteform
import no.nav.eessi.pensjon.personoppslag.pdl.model.Metadata
import no.nav.eessi.pensjon.personoppslag.pdl.model.Personnavn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PreutfyllDodsboAdresseTest {

    @Test
    fun `person med navn som kontakt`() {

        val kontaktinformasjonForDoedsbo = KontaktinformasjonForDoedsbo(
            personSomKontakt = KontaktinformasjonForDoedsboPersonSomKontakt(
                personnavn = Personnavn("Solsikke", "Gaselle",  "Soltilbeder")),
            adresse =  KontaktinformasjonForDoedsboAdresse(
                adresselinje1 = "Morgenstjerne Bakkemakker gate 351",
                adresselinje2 = "Leilighet 4, oppgang 5",
                postnummer = "68",
                poststedsnavn = "Gokk"
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
        val actual = preutfyllDodsboAdresse(kontaktinformasjonForDoedsbo, null)

        val expected = Adresse(
            gate = "Dødsbo v/Solsikke Gaselle Soltilbeder, Morgenstjerne Bakkemakker gate 351",
            bygning = "Leilighet 4, oppgang 5",
            by = "Gokk",
            postnummer = "68",
            land = null
        )
        assertEquals(expected, actual)

    }
}