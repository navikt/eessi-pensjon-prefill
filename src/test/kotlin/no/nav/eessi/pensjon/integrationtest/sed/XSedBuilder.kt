package no.nav.eessi.pensjon.integrationtest.sed

import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.*

class XSedBuilder {

    class ValidResponseBuilderXSEd {
        var sed: SedType = SedType.X010
        var sedGVer: String = "4"
        var sedVer: String = "2"
        var xnav: XNavBuilder = XNavBuilder()

        fun build(): SED {
            return X010(sed, xnav = xnav.build())
         }
    }

    class XNavBuilder {
        var sak: XNavsakBuilder = XNavsakBuilder()
        fun build() = XNav(sak = sak.build())
    }

    class XNavsakBuilder {
        var kontekst: XKontekstBuilder = XKontekstBuilder()
        var leggtilinstitusjon: XLeggtilinstitusjonBuilder = XLeggtilinstitusjonBuilder()
        var paaminnelse: XPaaminnelseBuilder = XPaaminnelseBuilder()
        fun build() = Navsak(kontekst = kontekst.build(), leggtilinstitusjon = leggtilinstitusjon.build(), paaminnelse = paaminnelse.build())
    }

    class XLeggtilinstitusjonBuilder() {
        fun build() =  Leggtilinstitusjon()
    }
    class XRefusjonskravBuilder() {
        fun build() =  Refusjonskrav()
    }

    class XArbeidsgiverBuilder() {
        fun build() =  XArbeidsgiver()
    }

    class XKontekstBuilder {
        var bruker: XBrukerBuilder = XBrukerBuilder()
        var refusjonskrav: XRefusjonskravBuilder = XRefusjonskravBuilder()
        var arbeidsgiver: XArbeidsgiverBuilder = XArbeidsgiverBuilder()
        fun build() = Kontekst(bruker = bruker.build(), refusjonskrav = refusjonskrav.build(), arbeidsgiver = arbeidsgiver.build())
    }

    class XBrukerBuilder {
        var person: XPersonBuilder = XPersonBuilder()
        fun build() = Bruker(person = person.build())
    }

    class XSvarBuilder {
        var informasjon: InformasjonBuilder = InformasjonBuilder()
        fun build() = Svar(informasjon = informasjon.build())
    }

    class InformasjonBuilder {
        var ikketilgjengelig: IkkeTilgjengeligBuilder = IkkeTilgjengeligBuilder()
        var kommersenere: KommersenereItemBuilder = KommersenereItemBuilder()

        fun build() = Informasjon(ikketilgjengelig = listOf(ikketilgjengelig.build()), kommersenere = kommersenere.build())
    }

    class IkkeTilgjengeligBuilder {
        var grunn: GrunnBuilder = GrunnBuilder()
        fun build() = IkkeTilgjengelig(type = "sed", opplysninger = "Missing details", grunn.build())
    }

    class KommersenereItemBuilder {
        fun build() = listOf(KommersenereItem("dokument", opplysninger = "æøå"), KommersenereItem("sed", opplysninger = "P5000"))
    }

    class GrunnBuilder {
        fun build() = Grunn(type = "annet", annet = "Missing details")
    }

    class XPaaminnelseBuilder {
        var svar: XSvarBuilder = XSvarBuilder()
        fun build() = Paaminnelse(svar = svar.build())
    }

    class XPersonBuilder {
        var etternavn: String = "Testesen"
        var fornavn: String = "Test"
        var kjoenn: String = "M"
        var foedselsdato: String = "1988-07-12"

        fun build() = Person(
            fornavn = fornavn,
            etternavn = etternavn,
            kjoenn = kjoenn,
            foedselsdato = foedselsdato
        )
    }
}
