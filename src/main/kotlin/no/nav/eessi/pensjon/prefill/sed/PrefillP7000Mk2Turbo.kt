package no.nav.eessi.pensjon.prefill.sed

import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.document.P6000Dokument
import no.nav.eessi.pensjon.eux.model.document.Retning
import no.nav.eessi.pensjon.eux.model.sed.*
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection

import no.nav.eessi.pensjon.prefill.person.PrefillSed
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.toJson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

class PrefillP7000Mk2Turbo(private val prefillSed: PrefillSed) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP7000Mk2Turbo::class.java) }

    fun prefill(prefillData: PrefillDataModel, personData: PersonDataCollection): P7000 {

        val sed = prefillSed.prefill(prefillData, personData)
        logger.debug("Prefill ***P7000 Mk2 Turbo*** med preutfylling med data fra en eller flere P6000")

        val person = sed.nav?.bruker?.person
        val perspin =  person?.pin
        val eessielm = sed.nav?.eessisak?.firstOrNull()

        val gjenlevendePerson = sed.pensjon?.gjenlevende?.person
        val gjenlevendePin = sed.pensjon?.gjenlevende?.person?.pin

        //dekode liste av P6000 for preutfylling av P7000
        val partpayload = prefillData.partSedAsJson[SedType.P7000.name]
        val listP6000 =  partpayload?.let { payload -> mapJsonToAny<List<Pair<P6000Dokument, P6000>>>(payload) }

        logger.info("Prefill med antall P6000: ${listP6000?.size}, land: ${listP6000?.map { it.first.fraLand }} ")


        val eessisakerall = mapGyldigeEessisakerFraP6000(listP6000, eessielm)

        val p7000 = P7000(
            nav = Nav(
                eessisak = eessisakerall,
                bruker = Bruker(
                    person = Person(
                        etternavn = person?.etternavn,
                        fornavn = person?.fornavn,
                        foedselsdato = person?.foedselsdato,
                        kjoenn = person?.kjoenn,
                        pin = hentAllePinFraP6000(listP6000, perspin!!)
                    )
                ),
                //mappe om etternavn til mappingfeil
                ektefelle = Ektefelle(person = Person(etternavn = sed.nav?.bruker?.person?.etternavn))
            ),
            //mappe om kjoenn for mappingfeil
            pensjon = P7000Pensjon(
                gjenlevende = prefillGjenlevende(gjenlevendePerson, gjenlevendePin, listP6000),
                samletVedtak = prefilSamletMeldingVedtak(listP6000)
            )
        )

        logger.debug("Tilpasser  Mk2 Turb P7000 forenklet preutfylling, Ferdig.")
        return p7000
    }

    fun prefillGjenlevende(gjenlevendePerson: Person?, gjenlevendePin: List<PinItem>?, p6000list: List<Pair<P6000Dokument, P6000>>?): Bruker? {
        if (gjenlevendePerson == null) return null
        return Bruker(
            person = Person (
                etternavn = gjenlevendePerson.etternavn,
                fornavn = gjenlevendePerson.fornavn,
                foedselsdato = gjenlevendePerson.foedselsdato,
                kjoenn = gjenlevendePerson.kjoenn,
                pin = gjenlevendePin?.let { hentAllePinFraP6000(p6000list, it ) }
            )
        )
    }

    fun hentAllePinFraP6000(p6000List: List<Pair<P6000Dokument, P6000>>?, localPin: List<PinItem>): List<PinItem>? {

        val allePins = p6000List?.mapNotNull {
            val p6000 = it.second
            val korrektbruker = finnKorrektBruker(p6000)
            korrektbruker?.person?.pin
        }?.flatten()
            ?.filterNot { it.land == "NO" }

        return allePins?.plus(localPin)

    }

    fun mapGyldigeEessisakerFraP6000(document: List<Pair<P6000Dokument, P6000>>?, eessiSakNo: EessisakItem?): List<EessisakItem> {
        //fylle opp eessisaker kap. 1.0 P6000
        val eessisakerutland = document?.filter { p6000 -> p6000.first.retning == Retning.IN }
            ?.mapNotNull { p6000 -> p6000.second.nav?.eessisak }
            ?.flatten()
            ?.filter { it.land != "NO" }
            ?: emptyList()

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
            tildeltepensjoner = pensjonTildelt(document), //kap 4
            utsendtDato = null // kap. 6 dato
        )
    }

    //tildelt pensjon fra P6000
    fun pensjonTildelt(document: List<Pair<P6000Dokument, P6000>>?): List<TildeltPensjonItem>? {
        return document?.mapNotNull { doc ->
            val fraLand = doc.first.fraLand //documentItem participant (hvilket land P6000 kommer ifra)
            val sistMottattDato = doc.first.sistMottatt

            val p6000 = doc.second //P6000 seden
            val p6000pensjon = p6000.pensjon

            val eessisak = p6000.nav?.eessisak?.firstOrNull { it.land == fraLand }

            val p6000vedtak = p6000pensjon?.vedtak?.firstOrNull { it.resultat != "02" } //P6000; 01 = invilgelse, 02 = avslag, 03, 04..

            if (p6000vedtak != null) {
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
                    institusjon = mapInstusjonP6000(eessisak, p6000bruker, p6000pensjon.tilleggsinformasjon,  fraLand),
                    reduksjonsGrunn = finnReduksjonsGrunn(p6000pensjon.reduksjon?.firstOrNull()),
                    revurderingtidsfrist = p6000pensjon.sak?.kravtype?.firstOrNull { it.datoFrist != null }?.datoFrist,
                    innvilgetPensjon = mapP6000artikkelTilInnvilgetPensjon(p6000vedtak.artikkel)

                )

            } else {
                null
            }
        }
    }

    private fun mapP6000artikkelTilInnvilgetPensjon(artikkel: String?): String? {
        return when (artikkel) {
            "01" -> "01"
            "02" -> "02"
            "03", "04" -> "03"
            "05" -> "04"
            else -> null
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
        return beregniger?.map { beregn ->
            YtelserItem(
                startdatoretttilytelse = beregn.periode?.fom,
                sluttdatoUtbetaling = beregn.periode?.tom,
                beloep = listOf(
                    BeloepItem(
                        valuta = beregn.valuta,
                        betalingshyppighetytelse = Betalingshyppighet.maaned_12_per_aar,
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
        return p6000.pensjon?.gjenlevende ?: p6000.nav?.bruker
    }

    fun mapInstusjonP6000(eessiSak: EessisakItem?, p6000bruker: Bruker?, tilleggsinformasjon: Tilleggsinformasjon?, fraLand: String?): Institusjon {
        val andreinst = tilleggsinformasjon?.andreinstitusjoner?.firstOrNull{ it.land == fraLand }

        // P7000Institusjon -> Pinitem, eessisak,  institusjon


        return Institusjon(
            saksnummer = eessiSak?.saksnummer, // 1.1.2 hentes fra P6000
            land =  fraLand, //buc sender countrycode , // eessiSak?.land, //1.1.2 (P6000)
            personNr = p6000bruker?.person?.pin?.firstOrNull { it.land == fraLand }?.identifikator,

            institusjonsid = andreinst?.institusjonsid, //eessiSak?.institusjonsid,
            institusjonsnavn = andreinst?.institusjonsnavn  //eessiSak?.institusjonsnavn,
        )

    }

    //avslag på pensjon fra P6000
    fun pensjonsAvslag(document: List<Pair<P6000Dokument, P6000>>?): List<PensjonAvslagItem>? {
        val res = document?.mapNotNull { doc ->

            val fraLand = doc.first.fraLand
            val sistMottattDato = doc.first.sistMottatt

            val p6000 = doc.second
            val p6000pensjon = p6000.pensjon
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
                        p6000.pensjon?.tilleggsinformasjon?.dato,
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





