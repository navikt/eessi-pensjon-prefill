package no.nav.eessi.pensjon.prefill

import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.pensjonsinformasjon.FinnSak
import no.nav.eessi.pensjon.pensjonsinformasjon.PensjoninformasjonException
import no.nav.eessi.pensjon.pensjonsinformasjon.PensjonsinformasjonClient
import no.nav.eessi.pensjon.prefill.models.PrefillDataModel
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.sak.V1Sak
import no.nav.pensjon.v1.vedtak.V1Vedtak
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

/**
 * hjelpe klass for utfylling av alle SED med pensjondata fra PESYS.
 * sakid eller vedtakid.
 */
@Component
class PensjonsinformasjonService(private val pensjonsinformasjonClient: PensjonsinformasjonClient) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PensjonsinformasjonService::class.java) }

    companion object {
        //hjelpe metode for å hente ut valgt V1SAK på vetak/SAK fnr og sakid benyttes
        fun finnSak(sakId: String, pendata: Pensjonsinformasjon): V1Sak? {
            if (sakId.isBlank()) throw ManglendeSakIdException("Mangler sakId")
            return FinnSak.finnSak(sakId, pendata)
        }
    }

    //hjelemetode for Vedtak P6000 P5000
    fun hentMedVedtak(vedtakId: String): Pensjonsinformasjon {
        if (vedtakId.isBlank()) throw IkkeGyldigKallException("Mangler vedtakID")
        return pensjonsinformasjonClient.hentAltPaaVedtak(vedtakId)
    }

    //hjelpe metode for å hente ut date for SAK/krav P2x00 fnr benyttes
    fun hentPensjonInformasjon(fnr: String, aktoerId: String): Pensjonsinformasjon {
        if (aktoerId.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Mangler AktoerId")
        if (fnr.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Mangler FNR")

        //**********************************************
        //skal det gjøre en sjekk med en gang på tilgang av data? sjekke person? sjekke pensjon?
        //Nå er vi dypt inne i prefill SED også sjekker vi om vi får hentet ut noe Pensjonsinformasjon
        //hvis det inne inneholder noe data så feiler vi!
        //**********************************************

        //val pendata: Pensjonsinformasjon = pensjonsinformasjonClient.hentAltPaaAktoerId(aktoerId)
        val pendata: Pensjonsinformasjon = pensjonsinformasjonClient.hentAltPaaFNR(fnr, aktoerId)

        if (pendata.brukersSakerListe == null) {
            throw PensjoninformasjonException("Ingen gyldig brukerSakerListe")
        }
        return pendata
    }

    fun hentVedtak(vedtakId: String): Pensjonsinformasjon {
        logger.debug("----------------------------------------------------------")
        val starttime = System.nanoTime()

        logger.debug("Starter [vedtak] Preutfylling Utfylling Data")

        logger.debug("vedtakId: $vedtakId")
        val pensjonsinformasjon = hentMedVedtak(vedtakId)

        logger.debug("Henter pensjondata fra PESYS")

        val endtime = System.nanoTime()
        val tottime = endtime - starttime

        logger.debug("Ferdig hentet pensjondata fra PESYS. Det tok ${(tottime / 1.0e9)} sekunder.")
        logger.debug("----------------------------------------------------------")

        return pensjonsinformasjon
    }

    fun hentRelevantPensjonSak(prefillData: PrefillDataModel, akseptabelSakstypeForSed: (String) -> Boolean): V1Sak? {
        val fnr = prefillData.bruker.norskIdent
        val aktorId = prefillData.bruker.aktorId
        val penSaksnummer = prefillData.penSaksnummer
        val sedType = prefillData.sedType

        logger.debug("penSaksnummer: $penSaksnummer")

        if (penSaksnummer.isBlank()) throw ManglendeSakIdException("Mangler sakId")
        if (fnr.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Mangler norskident")

        val peninfo = hentPensjonInformasjon(fnr, aktorId)
        // ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ingen pensjoninformasjon funnet")

        return peninfo.let {
            val sak = finnSak(penSaksnummer, it) ?: return null

            if (!akseptabelSakstypeForSed(sak.sakType)) {
                logger.warn("Du kan ikke opprette ${sedTypeAsText(sedType)} i en ${sakTypeAsText(sak.sakType)} (PESYS-saksnr: $penSaksnummer har sakstype ${sak.sakType})")
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Du kan ikke opprette ${sedTypeAsText(sedType)} i en ${sakTypeAsText(sak.sakType)} (PESYS-saksnr: $penSaksnummer har sakstype ${sak.sakType})")
            }
            sak
        }
    }

    fun hentRelevantVedtakHvisFunnet(vedtakId : String): V1Vedtak? {
        if (vedtakId.isBlank()) return null
        return  hentMedVedtak(vedtakId).vedtak
    }

    private fun sakTypeAsText(sakType: String?) =
            when (sakType) {
                "UFOREP" -> "uføretrygdsak"
                "ALDER" -> "alderspensjonssak"
                "GJENLEV" -> "gjenlevendesak"
                "BARNEP" -> "barnepensjonssak"
                null -> "[NULL]"
                else -> "$sakType-sak"
            }

    private fun sedTypeAsText(sedType: SedType) =
            when (sedType) {
                SedType.P2000 -> "alderspensjonskrav"
                SedType.P2100 -> "gjenlevende-krav"
                SedType.P2200 -> "uføretrygdkrav"
                else -> sedType.name
            }

}

class IkkeGyldigKallException(reason: String): ResponseStatusException(HttpStatus.BAD_REQUEST, reason)

class ManglendeSakIdException(reason: String): ResponseStatusException(HttpStatus.BAD_REQUEST, reason)

