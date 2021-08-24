package no.nav.eessi.pensjon.fagmodul.prefill.sed

import no.nav.eessi.pensjon.eux.model.document.P6000Dokument
import no.nav.eessi.pensjon.eux.model.sed.AdressatForRevurderingItem
import no.nav.eessi.pensjon.eux.model.sed.BeloepItem
import no.nav.eessi.pensjon.eux.model.sed.BeregningItem
import no.nav.eessi.pensjon.eux.model.sed.Bruker
import no.nav.eessi.pensjon.eux.model.sed.EessisakItem
import no.nav.eessi.pensjon.eux.model.sed.Ektefelle
import no.nav.eessi.pensjon.eux.model.sed.Institusjon
import no.nav.eessi.pensjon.eux.model.sed.Nav
import no.nav.eessi.pensjon.eux.model.sed.P6000
import no.nav.eessi.pensjon.eux.model.sed.P6000Pensjon
import no.nav.eessi.pensjon.eux.model.sed.P7000
import no.nav.eessi.pensjon.eux.model.sed.P7000Pensjon
import no.nav.eessi.pensjon.eux.model.sed.PensjonAvslagItem
import no.nav.eessi.pensjon.eux.model.sed.Person
import no.nav.eessi.pensjon.eux.model.sed.PinItem
import no.nav.eessi.pensjon.eux.model.sed.ReduksjonItem
import no.nav.eessi.pensjon.eux.model.sed.SamletMeldingVedtak
import no.nav.eessi.pensjon.eux.model.sed.SedType
import no.nav.eessi.pensjon.eux.model.sed.TildeltPensjonItem
import no.nav.eessi.pensjon.eux.model.sed.YtelserItem
import no.nav.eessi.pensjon.fagmodul.models.PersonDataCollection
import no.nav.eessi.pensjon.fagmodul.models.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillSed
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.toJson
import no.nav.eessi.pensjon.utils.typeRefs
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

class PrefillP7000Mk2Turbo(private val prefillSed: PrefillSed) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP7000Mk2Turbo::class.java) }

    fun prefill(prefillData: PrefillDataModel, personData: PersonDataCollection): P7000 {

        val sed = prefillSed.prefill(prefillData, personData)
        logger.debug("Prefill ***P7000 Mk2 Turbo*** med preutfylling med data fra en eller flere P6000")

        val person = sed.nav?.bruker?.person
        val perspin = person?.pin?.firstOrNull()
        val eessielm = sed.nav?.eessisak?.firstOrNull()


        //dekode liste av P6000 for preutfylling av P7000
        val partpayload = prefillData.partSedAsJson[SedType.P7000.name]
        val listP6000 =
            partpayload?.let { payload -> mapJsonToAny(payload, typeRefs<List<Pair<P6000Dokument, P6000>>>()) }

        val eessisakerall = eessisaker(listP6000, eessielm)

        val p7000 = P7000(
            nav = Nav(
                eessisak = eessisakerall,
                bruker = Bruker(
                    person = Person(
                        etternavn = person?.etternavn,
                        fornavn = person?.fornavn,
                        foedselsdato = person?.foedselsdato,
                        kjoenn = person?.kjoenn,
                        pin = listOf(
                            PinItem(
                                identifikator = perspin?.identifikator,
                                land = perspin?.land,
                                institusjon = Institusjon(
                                    institusjonsid = perspin?.institusjon?.institusjonsid,
                                    institusjonsnavn = perspin?.institusjon?.institusjonsnavn
                                )
                            )
                        )
                    )
                ),
                //mappe om etternavn til mappingfeil
                ektefelle = Ektefelle(person = Person(etternavn = sed.nav?.bruker?.person?.etternavn))
            ),
            //mappe om kjoenn for mappingfeil
            p7000Pensjon = P7000Pensjon(
                //TODO Tar bort bruker mapping for mk2 sjekk mot mapping til
                //bruker = Bruker(person = Person(kjoenn = sed.nav?.bruker?.person?.kjoenn)),
                gjenlevende = sed.pensjon?.gjenlevende,
                samletVedtak = prefilSamletMeldingVedtak(listP6000)
            )
        )

        logger.debug("Tilpasser P7000 forenklet preutfylling, Ferdig.")
        return p7000
    }

    fun eessisaker(document: List<Pair<P6000Dokument, P6000>>?, eessiSakNo: EessisakItem?): List<EessisakItem> {
        //fylle opp eessisaker kap. 1.0
        val eessisakerutland = document?.filterNot { p6 -> p6.first.fraLand == "NO" }
            ?.mapNotNull { p6 -> p6.second.nav?.eessisak?.firstOrNull { it.land == p6.first.fraLand } }
            ?.toList() ?: emptyList()

        val eessisakno = listOf<EessisakItem>(
            EessisakItem(
                land = eessiSakNo?.land,
                saksnummer = eessiSakNo?.saksnummer,
                institusjonsid = eessiSakNo?.institusjonsid,
                institusjonsnavn = eessiSakNo?.institusjonsnavn
            )
        )

        return eessisakno + eessisakerutland
    }

    fun prefilSamletMeldingVedtak(document: List<Pair<P6000Dokument, P6000>>?): SamletMeldingVedtak? {
        if (document == null || document.isEmpty()) {
            return null
        }

        return SamletMeldingVedtak(
            avslag = pensjonsAvslag(document), //kap 5
            utsendtDato = null, // kap. 6 dato
            tildeltepensjoner = pensjonTildelt(document), //kap 4
        )
    }

    //tildelt pensjon fra P6000
    fun pensjonTildelt(document: List<Pair<P6000Dokument, P6000>>?): List<TildeltPensjonItem>? {
        return document?.mapNotNull { doc ->
            val fraLand = doc.first.fraLand //documentItem

            val p6000 = doc.second //P6000 seden
            val p6000pensjon = p6000.p6000Pensjon

            val eessisak = p6000.nav?.eessisak?.firstOrNull { it.land == fraLand }
            val p6000vedtak = p6000pensjon?.vedtak?.firstOrNull { it.resultat != "02" } // 02 = avslag

            if (p6000vedtak != null) {
                val sistMottattDato = doc.first.sistMottatt
                val p6000bruker = finnKorrektBruker(p6000)

                TildeltPensjonItem(
                    pensjonType = p6000vedtak.type,
                    adressatForRevurdering = preutfyllAdressatForRevurdering(p6000pensjon),
                    tildeltePensjonerLand = fraLand,
                    vedtaksDato = mapVedtakDatoEllerSistMottattdato(
                        p6000pensjon.tilleggsinformasjon?.dato,
                        sistMottattDato
                    ),
                    startdatoPensjonsRettighet = p6000vedtak.virkningsdato,
                    ytelser = mapYtelserP6000(p6000vedtak.beregning),
                    institusjon = mapInstusjonP6000(eessisak, p6000bruker, fraLand),
                    reduksjonsGrunn = finnReduksjonsGrunn(p6000pensjon.reduksjon?.firstOrNull()),
                    revurderingtidsfrist = p6000pensjon.sak?.kravtype?.firstOrNull { it.datoFrist != null }?.datoFrist,
                    innvilgetPensjon = p6000pensjon.vedtak?.firstOrNull { it.artikkel != null }?.artikkel
                )
            } else {
                null
            }
        }
    }

    private fun finnReduksjonsGrunn(reduksjonItem: ReduksjonItem?): String {
        if (reduksjonItem?.aarsak != null) {
            return "03"
        } else if (reduksjonItem != null) {
            return reduksjonItem.type.toString()
        }
        return ""
    }

    fun preutfyllAdressatForRevurdering(pensjon: P6000Pensjon?): List<AdressatForRevurderingItem>? {
        return pensjon?.tilleggsinformasjon?.andreinstitusjoner?.map { andreinst ->

            val nonEmptyAdressItems = listOf(
                andreinst.institusjonsnavn,
                andreinst.institusjonsadresse,
                andreinst.bygningsnavn,
                andreinst.poststed,
                andreinst.postnummer,
                andreinst.region,
                andreinst.land,
            ).filterNotNull()
            AdressatForRevurderingItem(nonEmptyAdressItems.joinToString("\n"))
        }
    }

    // mottattdato dersom dato ikke finnes.!
    fun mapVedtakDatoEllerSistMottattdato(vedtakDato: String?, sistMottatt: LocalDate): String {
        return vedtakDato ?: sistMottatt.toString()
    }

    fun mapYtelserP6000(beregniger: List<BeregningItem>?): List<YtelserItem>? {
        logger.debug("beregning: ${beregniger?.toJson()}")
        return beregniger?.mapNotNull { beregn ->
            YtelserItem(
                startdatoretttilytelse = beregn.periode?.fom,
                sluttdatoUtbetaling = beregn.periode?.tom,
                beloep = listOf(
                    BeloepItem(
                        valuta = beregn.valuta,
                        betalingshyppighetytelse = mapUtbetalingHyppighet(beregn.utbetalingshyppighet),
                        utbetalingshyppighetAnnen = beregn.utbetalingshyppighetAnnen,
                        beloepBrutto = beregn.beloepBrutto?.beloep
                    )
                )
            )
        }
    }

    private fun mapUtbetalingHyppighet(utbetalingshyppighet: String?): String? {
        val sjekkformap = mapOf(
            "aarlig" to "01",
            "kvartalsvis" to "02",
            "maaned_12_per_aar" to "03",
            "maaned_13_per_aar" to "04",
            "maaned_14_per_aar" to "05",
            "ukentlig" to "06",
            "annet" to "99"
        )
        return sjekkformap[utbetalingshyppighet] ?: utbetalingshyppighet
    }


    fun finnKorrektBruker(p6000: P6000): Bruker? {
        return p6000.p6000Pensjon?.gjenlevende ?: p6000.nav?.bruker
    }

    fun mapInstusjonP6000(eessiSak: EessisakItem?, p6000bruker: Bruker?, fraLand: String?): Institusjon? {
        return Institusjon(
            saksnummer = eessiSak?.saksnummer,
            personNr = p6000bruker?.person?.pin?.firstOrNull { it.land == fraLand }?.identifikator,
            land = fraLand,
            institusjonsid = eessiSak?.institusjonsid,
            institusjonsnavn = eessiSak?.institusjonsnavn,
        )

    }

    //avslag på pensjon fra P6000
    fun pensjonsAvslag(document: List<Pair<P6000Dokument, P6000>>?): List<PensjonAvslagItem>? {
        val res = document?.mapNotNull { doc ->

            val fraLand = doc.first.fraLand
            val sistMottattDato = doc.first.sistMottatt

            val p6000 = doc.second
            val p6000pensjon = p6000.p6000Pensjon
            //resultat = "02" er avslag
            val avslag = p6000pensjon?.vedtak?.firstOrNull { it.resultat == "02" }

            if (avslag == null) {
                null
            }
            else{
                PensjonAvslagItem(
                    pensjonType = avslag.type,
                    begrunnelse = avslag.avslagbegrunnelse?.first()?.begrunnelse,
                    dato = mapVedtakDatoEllerSistMottattdato(
                        p6000.p6000Pensjon?.tilleggsinformasjon?.dato,
                        sistMottattDato
                    ),  //dato mottatt dato ikke finnes
                    pin = finnKorrektBruker(p6000)?.person?.pin?.firstOrNull { it.land == fraLand },
                    tidsfristForRevurdering = p6000pensjon.sak?.kravtype?.firstOrNull { it.datoFrist != null }?.datoFrist,
                    adressatforRevurderingAvslag = preutfyllAdressatForRevurdering(p6000pensjon)
                )
            }
        }
        return res
    }
}





