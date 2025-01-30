package no.nav.eessi.pensjon.integrationtest.sed

import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.*

class SedBuilder {

    class ValidResponseBuilder {
        var sed: SedType = SedType.P2000
        var sedGVer: String = "4"
        var sedVer: String = "2"
        var pensjon: Pensjon? = null
        var nav: NavBuilder = NavBuilder()

        fun nav(init: NavBuilder.() -> Unit) = apply { nav.init() }
        fun build(): SED {
            return when (sed) {
                SedType.P2000 -> return P2000(sed, p2000pensjon = pensjon?.let { pensjon as P2000Pensjon}, nav = nav.build())
                SedType.P4000 -> return P4000(sed, p4000Pensjon =  P4000Pensjon(null), nav = nav.build())
                SedType.P6000 -> return P6000(sed, pensjon = pensjon?.let { pensjon as P6000Pensjon}, nav = nav.apply { krav = null } .build())
                else -> { SED(sed, sedGVer = sedGVer, sedVer = sedVer, pensjon = null, nav = nav.build()) }
            }
         }
    }

    class NavBuilder {
        var eessisak: List<EessiSakBuilder> = listOf(EessiSakBuilder())
        var bruker: BrukerBuilder = BrukerBuilder()
        var krav : KravBuilder? = KravBuilder()
        fun bruker(init: BrukerBuilder.() -> Unit) = apply { bruker.init() }
        fun build() = Nav(eessisak = eessisak.map { it.build() }, bruker = bruker.build(), krav = krav?.build())
    }

    class EessiSakBuilder {
        var institusjonsid: String = "NO:noinst002"
        var institusjonsnavn: String = "NOINST002, NO INST002, NO"
        var saksnummer: String = "22874955"
        var land: String = "NO"

        fun build() = EessisakItem(institusjonsnavn = institusjonsnavn, institusjonsid = institusjonsid, saksnummer = saksnummer, land = land)
    }

    class BrukerBuilder {
        var person: PersonBuilder = PersonBuilder()
        var adresse: AdresseBuilder = AdresseBuilder()

        fun person(init: PersonBuilder.() -> Unit) = apply { person.init() }
        fun adresse(init: AdresseBuilder.() -> Unit) = apply { adresse.init() }

        fun build() = Bruker(person = person.build(), adresse = adresse.build())
    }

    class PersonBuilder {
        var pinList: MutableList<PinBuilder> = mutableListOf(PinBuilder(institusjonsnavn = "NOINST002, NO INST002, NO", institusjonsid = "NO:noinst002", identifikator = "12312312312", land = "NO"), PinBuilder(identifikator = "123123123", land = "QX"))
        var statsborgerskap: List<StatsborgerskapBuilder> = listOf(StatsborgerskapBuilder())
        var etternavn: String = "Gjenlev"
        var fornavn: String = "Lever"
        var kjoenn: String = "M"
        var foedselsdato: String = "1988-07-12"
        var sivilstand: List<SivilstandBuilder> = listOf(SivilstandBuilder())
        var kontakt: KontaktBuilder = KontaktBuilder()

        fun kontakt(init: KontaktBuilder.() -> Unit) = apply { kontakt.init() }

        fun build() = Person(pin = pinList.map { it.build() },
            statsborgerskap = statsborgerskap.map { it.build()},
            fornavn = fornavn,
            etternavn = etternavn,
            kjoenn = kjoenn,
            foedselsdato = foedselsdato,
            sivilstand = sivilstand.map { it.build() },
            kontakt = kontakt.build()
        )
    }

    class PinBuilder(var institusjonsnavn: String? = null, var institusjonsid: String? = null, var identifikator: String? = null, var land: String? = null) {
        fun build() = PinItem(institusjonsnavn = institusjonsnavn, institusjonsid = institusjonsid, identifikator = identifikator, land = land)
    }

    class StatsborgerskapBuilder(var land: String = "QX") {
        fun build() = StatsborgerskapItem(land = land)
    }

    class KravBuilder(var dato: String = "2018-06-28") {
        fun build() = Krav(dato = dato)
    }

    class SivilstandBuilder(var fradato: String = "2000-10-01", var status: SivilstandRina = SivilstandRina.enslig) {
        fun build() =  SivilstandItem(fradato = fradato, status = status)
    }

    class KontaktBuilder {
        var telefon: List<TelefonBuilder> = listOf(TelefonBuilder())
        var email: List<EmailBuilder> = listOf(EmailBuilder())

        fun build() = Kontakt(telefon = telefon.map { it.build() }, email = email.map { it.build() })
    }

    class TelefonBuilder(var type: String = "mobil", var nummer: String = "11111111") {
        fun build() = Telefon(type = type, nummer = nummer)
    }

    class EmailBuilder(var adresse: String = "melleby11@melby.no") {
        fun build() = Email(adresse = adresse)
    }

    class AdresseBuilder(var gate: String = "Oppoverbakken 66", var by: String = "SØRUMSAND", var postnummer: String = "1920", var land: String = "NO") {
        fun build() = Adresse(gate = gate, postnummer = postnummer, by = by, land = land)
    }

    class P6000PensjonBuilder(){
        fun build() = P6000Pensjon(
            vedtak = listOf(
                VedtakItem(
                    type = "01",
                    resultat = "02",
                    avslagbegrunnelse = listOf(AvslagbegrunnelseItem("03"))
                )
            ),
            sak = Sak(
                kravtype = listOf(KravtypeItem("six weeks from the date the decision is received"))
            ),
            tilleggsinformasjon = Tilleggsinformasjon(
                dato = "2020-12-16",
                andreinstitusjoner = listOf(
                    AndreinstitusjonerItem(
                        institusjonsid = "NO:noinst002",
                        institusjonsnavn = "NOINST002, NO INST002, NO",
                        institusjonsadresse = "Postboks 6600 Etterstad TEST",
                        postnummer = "0607",
                        land = "NO",
                        poststed = "Oslo"
                    )
                )
            )
        )
    }

    class P2000PensjonBuilder(){
        var ytelser: List<YtelserItem> = YtelserBuilder().build()
        var kravDato: Krav? = Krav("2018-06-28")
        var etterspurtedokumenter = "P5000 and P6000"
        var forespurtstartdato: String? = null
        fun build() = P2000Pensjon(ytelser = ytelser, kravDato =kravDato, etterspurtedokumenter = etterspurtedokumenter, forespurtstartdato = forespurtstartdato)
    }
    class YtelserBuilder(
        var status: String = "01",
        var mottasbasertpaa: String? = "botid",
        var startdatoutbetaling: String? = null,
        var totalbruttobeloeparbeidsbasert: String? = null,
        var totalbruttobeloepbostedsbasert: String? = null,
        var startdatoretttilytelse: String? = null,
        var belop: List<BeloepItem>? = BelopBuilder().build()
    ) {
        fun build(): List<YtelserItem> {
            return listOf(
                YtelserItem(
                    mottasbasertpaa = mottasbasertpaa,
                    startdatoretttilytelse = startdatoretttilytelse,
                    ytelse = "10",
                    beloep = belop,
                    status = status,
                    startdatoutbetaling = startdatoutbetaling,
                    totalbruttobeloeparbeidsbasert = totalbruttobeloeparbeidsbasert,
                    totalbruttobeloepbostedsbasert = totalbruttobeloepbostedsbasert
                )
            )
        }
    }
    class BelopBuilder(var belop: String = "21232", var betalingshyppighetytelse: Betalingshyppighet? = null, var gjeldendesiden:String = "2018-08-01") {
        fun build(): List<BeloepItem> {
            return listOf(
                BeloepItem(
                    valuta = "NOK",
                    beloep = belop,
                    gjeldendesiden = gjeldendesiden,
                    betalingshyppighetytelse = betalingshyppighetytelse,
                )
            )
        }
    }
}