package no.nav.eessi.pensjon.prefill

import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.pensjonsinformasjon.FinnSak
import no.nav.eessi.pensjon.pensjonsinformasjon.clients.PensjoninformasjonException
import no.nav.eessi.pensjon.pensjonsinformasjon.clients.PensjonsinformasjonClient
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.sak.V1Sak
import no.nav.pensjon.v1.vedtak.V1Vedtak
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.RetryListener
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

/**
 * hjelpe klass for utfylling av alle SED med pensjondata fra PESYS.
 * sakid eller vedtakid.
 */
@Component
class PensjonsinformasjonService(private val pensjonsinformasjonClient: PensjonsinformasjonClient,
                                 @Value("\${ENV}") private val environment: String
) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PensjonsinformasjonService::class.java) }

    companion object {
        //hjelpe metode for å hente ut valgt V1SAK på vetak/SAK fnr og sakid benyttes
        fun finnSak(sakId: String?, pendata: Pensjonsinformasjon): V1Sak? {
            if (sakId.isNullOrBlank()) throw ManglendeSakIdException("Mangler sakId")
            return FinnSak.finnSak(sakId, pendata)
        }
    }

    //hjelemetode for Vedtak P6000 P5000
    fun hentMedVedtak(vedtakId: String): Pensjonsinformasjon {
        if (vedtakId.isBlank()) throw IkkeGyldigKallException("Mangler vedtakID")
        return pensjonsinformasjonClient.hentAltPaaVedtak(vedtakId)
    }

    //hjelpe metode for å hente ut date for SAK/krav P2x00 fnr benyttes

    @Retryable(
        backoff = Backoff(delayExpression = "@pensjonsInfoRetryConfig.initialRetryMillis", maxDelay = 200000L, multiplier = 3.0),
        listeners  = ["pensjonsInfoRetryLogger"])
    fun hentPensjonInformasjon(fnr: String, aktoerId: String?): Pensjonsinformasjon {
        if (aktoerId.isNullOrBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Mangler AktoerId")
        if (fnr.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Mangler FNR")

        //**********************************************
        //skal det gjøre en sjekk med en gang på tilgang av data? sjekke person? sjekke pensjon?
        //Nå er vi dypt inne i prefill SED også sjekker vi om vi får hentet ut noe Pensjonsinformasjon
        //hvis det inne inneholder noe data så feiler vi!
        //**********************************************
        logger.info("Hent pensjonInformasjon $environment")

        val pendata = if( environment in listOf("test", "q1")) {
            logger.debug("Henter ikke vedtak i q1")
            Pensjonsinformasjon()
        } else {
            pensjonsinformasjonClient.hentAltPaaFNR(fnr)
        }

        if (pendata.brukersSakerListe == null) {
            return Pensjonsinformasjon()
        }
        return pendata
    }

    fun hentVedtak(vedtakId: String): Pensjonsinformasjon {
        logger.debug("----------------------------------------------------------")
        val starttime = System.nanoTime()

        logger.debug("Starter [vedtak] Preutfylling Utfylling Data for $environment")
        logger.debug("vedtakId: $vedtakId")

        val pensjonsinformasjon = if( environment in listOf("test", "q1")) {
            logger.debug("Henter ikke vedtak i q1")
            return Pensjonsinformasjon()
        } else {
            hentMedVedtak(vedtakId)
        }
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

        if (penSaksnummer.isNullOrBlank()) throw ManglendeSakIdException("Mangler sakId")
        if (fnr.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Mangler norskident")

        val peninfo = aktorId?.let { hentPensjonInformasjon(fnr, it) }

        return peninfo?.let {
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


@Profile("!retryConfigOverride")
@Component
data class PensjonsInfoRetryConfig(val initialRetryMillis: Long = 20000L)

@Component
class PensjonsInfoRetryLogger :  RetryListener {
    private val logger = LoggerFactory.getLogger(PensjonsInfoRetryLogger::class.java)
    override fun <T : Any?, E : Throwable?> onError(context: RetryContext?, callback: RetryCallback<T, E>?, throwable: Throwable?) {
        logger.info("Feil under henting fra EUX - try #${context?.retryCount } - ${throwable?.toString()}", throwable)
    }
}

