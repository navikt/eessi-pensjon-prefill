package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.AndreinstitusjonerItem
import no.nav.eessi.eessifagmodul.models.IkkeGyldigKallException
import no.nav.eessi.eessifagmodul.services.pensjonsinformasjon.PensjonsinformasjonService
import no.nav.pensjon.v1.brukersbarn.V1BrukersBarn
import no.nav.pensjon.v1.ektefellepartnersamboer.V1EktefellePartnerSamboer
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.sak.V1Sak
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * hjelpe klass for utfylling av alle SED med pensjondata fra PESYS.
 * sakid eller vedtakid.
 */
@Component
class PensjonsinformasjonHjelper(private val pensjonsinformasjonService: PensjonsinformasjonService) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PensjonsinformasjonHjelper::class.java) }

    @Value("\${eessi.pensjon.lokalid}")
    lateinit var institutionid: String

    @Value("\${eessi.pensjon.lokalnavn}")
    lateinit var institutionnavn: String

    @Value("\${eessi.pensjon.adresse.gate}")
    lateinit var institutionGate: String

    @Value("\${eessi.pensjon.adresse.by}")
    lateinit var institutionBy: String

    @Value("\${eessi.pensjon.adresse.postnummer}")
    lateinit var institutionPostnr: String

    @Value("\${eessi.pensjon.adresse.land}")
    lateinit var institutionLand: String

    //hjelemetode for Vedtak P6000 P5000
    fun hentMedVedtak(prefillData: PrefillDataModel): Pensjonsinformasjon {
        val vedtakId = if (prefillData.vedtakId.isNotBlank()) prefillData.vedtakId else throw IkkeGyldigKallException("Mangler vedtakID")
        val pendata: Pensjonsinformasjon = pensjonsinformasjonService.hentAltPaaVedtak(vedtakId)
        createInstitusionReview(prefillData)

        logger.debug("Pensjonsinformasjon: $pendata"
                + "\nPensjonsinformasjon.vedtak: ${pendata.vedtak}"
                + "\nPensjonsinformasjon.vedtak.virkningstidspunkt: ${pendata.vedtak.virkningstidspunkt}"
                + "\nPensjonsinformasjon.sak: ${pendata.sakAlder}"
                + "\nPensjonsinformasjon.trygdetidListe: ${pendata.trygdetidListe}"
                + "\nPensjonsinformasjon.vilkarsvurderingListe: ${pendata.vilkarsvurderingListe}"
                + "\nPensjonsinformasjon.ytelsePerMaanedListe: ${pendata.ytelsePerMaanedListe}"
                + "\nPensjonsinformasjon.trygdeavtale: ${pendata.trygdeavtale}"
                + "\nPensjonsinformasjon.person: ${pendata.person}"
                + "\nPensjonsinformasjon.person.pin: ${pendata.person.pid}")

        return pendata
    }

    //hjelpe metode for å hente ut date for SAK/krav P2x00 fnr benyttes
    fun hentMedFnr(prefillData: PrefillDataModel): Pensjonsinformasjon {
        val fnr = if (prefillData.personNr.isNotBlank()) prefillData.personNr else throw IkkeGyldigKallException("Mangler Fnr")
        val pendata: Pensjonsinformasjon = pensjonsinformasjonService.hentAltPaaFnr(fnr)
        createRelasjonerBarnOgAvdod(prefillData, pendata)
        return pendata
    }

    //hjelpe metode for å hente ut valgt V1SAK på vetak/SAK fnr og sakid benyttes
    fun hentMedSak(prefillData: PrefillDataModel, pendata: Pensjonsinformasjon): V1Sak {
        val sakId = if (prefillData.penSaksnummer.isNotBlank()) prefillData.penSaksnummer else throw IkkeGyldigKallException("Mangler sakId")
        return pensjonsinformasjonService.hentAltPaaSak(sakId, pendata)
    }

    //henter ut nødvendige familie relasjoner
    fun createRelasjonerBarnOgAvdod(dataModel: PrefillDataModel, pendata: Pensjonsinformasjon): PrefillDataModel {
        logger.debug("Henter ut liste barn fra PESYS")

        val listbarmItem = mutableListOf<V1BrukersBarn>()
        if (pendata.brukersBarnListe != null) {
            pendata.brukersBarnListe.brukersBarnListe.forEach {
                listbarmItem.add(it)
            }
        }

        logger.debug("Henter ut liste ektefeller/partnere fra PESYS")
        val listEktefellePartnerFnrlist = mutableListOf<V1EktefellePartnerSamboer>()
        if (pendata.ektefellePartnerSamboerListe != null) {
            pendata.ektefellePartnerSamboerListe.ektefellePartnerSamboerListe.forEach {
                listEktefellePartnerFnrlist.add(it)
            }
        }

        dataModel.partnerFnr = listEktefellePartnerFnrlist
        dataModel.barnlist = listbarmItem

        logger.debug("Henter ut avdod relasjoner fra PESYS")
        dataModel.avdod = pendata.avdod?.avdod ?: ""
        dataModel.avdodMor = pendata.avdod?.avdodMor ?: ""
        dataModel.avdodFar = pendata.avdod?.avdodFar ?: ""

        return dataModel
    }

    fun createInstitusionReview(prefillData: PrefillDataModel) {
        logger.debug("Henter ut lokal kontakt, institusjon (NAV Utland)")

        prefillData.andreInstitusjon = AndreinstitusjonerItem(
                institusjonsid = institutionid,
                institusjonsnavn = institutionnavn,
                institusjonsadresse = institutionGate,
                postnummer = institutionPostnr,
                bygningsnr = null,
                land = institutionLand,
                region = null,
                poststed = institutionBy
        )

        logger.debug("Andreinstitusjoner: ${prefillData.andreInstitusjon} ")

    }

}