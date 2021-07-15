package no.nav.eessi.pensjon.fagmodul.prefill.sed

import no.nav.eessi.pensjon.eux.model.document.P6000Dokument
import no.nav.eessi.pensjon.eux.model.sed.Bruker
import no.nav.eessi.pensjon.eux.model.sed.EessisakItem
import no.nav.eessi.pensjon.eux.model.sed.Ektefelle
import no.nav.eessi.pensjon.eux.model.sed.Institusjon
import no.nav.eessi.pensjon.eux.model.sed.Nav
import no.nav.eessi.pensjon.eux.model.sed.P6000
import no.nav.eessi.pensjon.eux.model.sed.P7000
import no.nav.eessi.pensjon.eux.model.sed.P7000Pensjon
import no.nav.eessi.pensjon.eux.model.sed.PensjonAvslagItem
import no.nav.eessi.pensjon.eux.model.sed.Person
import no.nav.eessi.pensjon.eux.model.sed.PinItem
import no.nav.eessi.pensjon.eux.model.sed.SamletMeldingVedtak
import no.nav.eessi.pensjon.eux.model.sed.SedType
import no.nav.eessi.pensjon.fagmodul.models.PersonDataCollection
import no.nav.eessi.pensjon.fagmodul.models.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillSed
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.typeRefs
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
        val listP6000 = partpayload?.let { payload -> mapJsonToAny(payload, typeRefs<List<Pair<P6000Dokument, P6000>>>() ) }


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
            vedtaksammendrag = null, // kap. 6 dato
            tildeltepensjoner = null, //kap 4
            startdatoPensjonsRettighet = null, // hva ?

        )
    }

    fun pensjonsAvslag(document: List<Pair<P6000Dokument, P6000>>?): List<PensjonAvslagItem>? {
        val res = document?.mapNotNull { doc ->

            val fraLand = doc.first.fraLand
            val p6000 = doc.second
            //resultat = "02" er avslag
            val avslag = p6000.p6000Pensjon?.vedtak?.firstOrNull { it.resultat == "02" }

            PensjonAvslagItem(
                    pensjonType = avslag?.type,
                    begrunnelse = avslag?.avslagbegrunnelse?.first()?.begrunnelse,
                    dato = p6000.p6000Pensjon?.tilleggsinformasjon?.dato,
                    pin = finnKorrektBruker(p6000)?.person?.pin?.firstOrNull { it.land == fraLand },
                    adresse = finnKorrektBruker(p6000)?.adresse?.toString()
                )
            }
        return res
    }

    fun finnKorrektBruker(p6000: P6000): Bruker? {
        return p6000?.p6000Pensjon?.gjenlevende ?: p6000.nav?.bruker
    }

}
