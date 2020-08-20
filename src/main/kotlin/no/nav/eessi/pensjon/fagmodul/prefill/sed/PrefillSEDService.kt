package no.nav.eessi.pensjon.fagmodul.prefill.sed

import no.nav.eessi.pensjon.fagmodul.models.SEDType
import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.model.PersonData
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonService
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillGjenlevende
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillSed
import no.nav.eessi.pensjon.fagmodul.prefill.sed.krav.EPSaktype.ALDER
import no.nav.eessi.pensjon.fagmodul.prefill.sed.krav.EPSaktype.UFOREP
import no.nav.eessi.pensjon.fagmodul.prefill.sed.krav.PrefillP2000
import no.nav.eessi.pensjon.fagmodul.prefill.sed.krav.PrefillP2100
import no.nav.eessi.pensjon.fagmodul.prefill.sed.krav.PrefillP2200
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.PrefillP6000
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.personoppslag.personv3.PersonV3Service
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.sak.V1Sak
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.ResponseStatus

@Component
class PrefillSEDService(private val prefillNav: PrefillNav,
                        private val personV3Service: PersonV3Service,
                        private val eessiInformasjon: EessiInformasjon,
                        private val pensjonsinformasjonService: PensjonsinformasjonService) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillSEDService::class.java) }

    fun prefill(prefillData: PrefillDataModel): SED {

        val sedType = SEDType.valueOf(prefillData.getSEDType())

        logger.debug("mapping prefillClass to SED: $sedType")

        return when (sedType) {
            //krav
            SEDType.P2000 -> PrefillP2000(prefillNav, hentRelevantPensjonSak(prefillData, { pensakType -> pensakType == ALDER.name})).prefill(prefillData, hentPersoner(prefillData, true))
            SEDType.P2200 -> PrefillP2200(prefillNav, hentRelevantPensjonSak(prefillData, { pensakType -> pensakType == UFOREP.name})).prefill(prefillData, hentPersoner(prefillData, true))
            SEDType.P2100 -> PrefillP2100(prefillNav, hentRelevantPensjonSak(prefillData, { pensakType -> listOf("ALDER", "BARNEP", "GJENLEV", "UFOREP").contains(pensakType) })).prefill(prefillData, hentPersoner(prefillData, true))

            //vedtak
            SEDType.P6000 -> PrefillP6000(prefillNav, eessiInformasjon, hentVedtak(prefillData)).prefill(prefillData, hentPersoner(prefillData, true))

            SEDType.P4000 -> PrefillP4000(getPrefillSed(prefillData)).prefill(prefillData, hentPersoner(prefillData))
            SEDType.P7000 -> PrefillP7000(getPrefillSed(prefillData)).prefill(prefillData, hentPersoner(prefillData))
            SEDType.P8000 -> PrefillP8000(getPrefillSed(prefillData)).prefill(prefillData, hentPersoner(prefillData))

            SEDType.P10000 -> PrefillP10000(getPrefillSed(prefillData)).prefill(prefillData, hentPersoner(prefillData))

            SEDType.X005 -> PrefillX005(prefillNav).prefill(prefillData, hentPersoner(prefillData))
            SEDType.H020, SEDType.H021 -> PrefillH02X(getPrefillSed(prefillData)).prefill(prefillData, hentPersoner(prefillData))
            else ->
                //P3000_NO vil aldre gå dennee vei! men fra EU-SED->Nav-SED->PESYS
                //P3000_SE, PL, DK, DE, UK, ol vil gå denne veien.
                //P5000, - P9000, P14000 og og andre
                getPrefillSed(prefillData).prefill(prefillData, hentPersoner(prefillData))
        }
    }

    private fun getPrefillSed(prefillData: PrefillDataModel): PrefillSed {
        val pensjonGjenlevende = PrefillGjenlevende(personV3Service, prefillNav).prefill(prefillData)
        return PrefillSed(prefillNav, pensjonGjenlevende)
    }


    fun hentVedtak(prefillData: PrefillDataModel): Pensjonsinformasjon {
        val vedtakId = prefillData.vedtakId

        if (vedtakId.isBlank()) throw ManglendeVedtakIdException("Mangler vedtakID")

        logger.debug("----------------------------------------------------------")
        val starttime = System.nanoTime()

        logger.debug("Starter [vedtak] Preutfylling Utfylling Data")

        logger.debug("vedtakId: $vedtakId")
        val pensjonsinformasjon = pensjonsinformasjonService.hentMedVedtak(vedtakId)

        logger.debug("Henter pensjondata fra PESYS")

        val endtime = System.nanoTime()
        val tottime = endtime - starttime

        logger.debug("Metrics")
        logger.debug("Ferdig hentet pensjondata fra PESYS. Det tok ${(tottime / 1.0e9)} sekunder.")
        logger.debug("----------------------------------------------------------")

        return pensjonsinformasjon
    }

    fun hentRelevantPensjonSak(prefillData: PrefillDataModel, akseptabelSakstypeForSed: (String) -> Boolean): V1Sak? {

        val aktorId = prefillData.bruker.aktorId
        val penSaksnummer = prefillData.penSaksnummer
        val sedType = prefillData.getSEDType()

        return pensjonsinformasjonService.hentPensjonInformasjonNullHvisFeil(aktorId)?.let {
            val sak: V1Sak = PensjonsinformasjonService.finnSak(penSaksnummer, it)

            if (!akseptabelSakstypeForSed(sak.sakType)) {
                logger.warn("Du kan ikke opprette ${sedTypeAsText(sedType)} i en ${sakTypeAsText(sak.sakType)} (PESYS-saksnr: $penSaksnummer har sakstype ${sak.sakType})")
                throw FeilSakstypeForSedException("Du kan ikke opprette ${sedTypeAsText(sedType)} i en ${sakTypeAsText(sak.sakType)} (PESYS-saksnr: $penSaksnummer har sakstype ${sak.sakType})")
            }
            sak
        }
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

    private fun sedTypeAsText(sedType: String) =
            when (sedType) {
                "P2000" -> "alderspensjonskrav"
                "P2100" -> "gjenlevende-krav"
                "P2200" -> "uføretrygdkrav"
                else -> sedType
            }

    //Henter inn alle personer fra ep-personoppslag  først før preutfylling
    private fun hentPersoner(prefillData: PrefillDataModel, fyllUtBarnListe: Boolean = false): PersonData {
        // FIXME - det veksles mellom gjenlevende og bruker ... usikkert om dette er rett...
        logger.info("Henter hovedperson/gjenlevende eller avdød (avdød: ${prefillData.avdod == null})")
        val brukerEllerGjenlevende = personV3Service.hentBruker(prefillData.avdod?.norskIdent ?: prefillData.bruker.norskIdent)

        logger.info("Henter hovedperson/forsikret")
        val forsikretPerson = personV3Service.hentBruker(prefillData.bruker.norskIdent)

        val (ektepinid, ekteTypeValue) = filterEktefelleRelasjon(forsikretPerson)

        logger.info("Henter ektefelle/partner (ekteType: $ekteTypeValue)")
        val ektefelleBruker = if(ektepinid.isBlank()) null else personV3Service.hentBruker(ektepinid)

        val barnBrukereFraTPS = if (forsikretPerson == null || !fyllUtBarnListe ) emptyList() else hentBarnFraTps(forsikretPerson)

        return PersonData(brukerEllerGjenlevende = brukerEllerGjenlevende, forsikretPerson = forsikretPerson!!, ektefelleBruker = ektefelleBruker, ekteTypeValue = ekteTypeValue, barnBrukereFraTPS = barnBrukereFraTPS)
    }

    fun hentBarnFraTps(hovedPerson: no.nav.tjeneste.virksomhet.person.v3.informasjon.Person) =
            hovedPerson.harFraRolleI
                    .filter { relasjon -> PrefillNav.Companion.RelasjonEnum.BARN.erSamme(relasjon.tilRolle.value) }
                    .map { relasjon -> (relasjon.tilPerson.aktoer as PersonIdent).ident.ident }
                    .mapNotNull { barnPin ->
                        logger.info("Henter barn fra TPS")
                        personV3Service.hentBruker(barnPin)
                    }

    private fun filterEktefelleRelasjon(bruker: Bruker?): Pair<String, String> {
        val validRelasjoner = listOf("EKTE", "REPA", "SAMB")

        if (bruker == null) return Pair("", "")
        var ektepinid = ""
        var ekteTypeValue = ""

        bruker.harFraRolleI.forEach {
            val relasjon = it.tilRolle.value

            if (validRelasjoner.contains(relasjon)) {

                ekteTypeValue = it.tilRolle.value
                val tilperson = it.tilPerson
                val pident = tilperson.aktoer as PersonIdent

                ektepinid = pident.ident.ident
                if (ektepinid.isNotBlank()) {
                    return@forEach
                }
            }
        }
        return Pair(ektepinid, ekteTypeValue)
    }

}

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class ManglendeVedtakIdException(message: String) : IllegalArgumentException(message)

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class FeilSakstypeForSedException(override val message: String?, override val cause: Throwable? = null) : IllegalArgumentException()

