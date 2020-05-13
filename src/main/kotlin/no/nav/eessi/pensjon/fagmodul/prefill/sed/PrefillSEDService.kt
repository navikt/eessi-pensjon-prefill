package no.nav.eessi.pensjon.fagmodul.prefill.sed

import no.nav.eessi.pensjon.fagmodul.models.SEDType
import no.nav.eessi.pensjon.fagmodul.prefill.eessi.EessiInformasjon
import no.nav.eessi.pensjon.fagmodul.prefill.model.PersonData
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.pen.PensjonsinformasjonService
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillGjenlevende
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillNav
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillSed
import no.nav.eessi.pensjon.fagmodul.prefill.sed.krav.PrefillP2000
import no.nav.eessi.pensjon.fagmodul.prefill.sed.krav.PrefillP2100
import no.nav.eessi.pensjon.fagmodul.prefill.sed.krav.PrefillP2200
import no.nav.eessi.pensjon.fagmodul.prefill.sed.vedtak.PrefillP6000
import no.nav.eessi.pensjon.fagmodul.prefill.tps.NavFodselsnummer
import no.nav.eessi.pensjon.fagmodul.prefill.tps.TpsPersonService
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PrefillSEDService(private val prefillNav: PrefillNav,
                        private val tpsPersonService: TpsPersonService,
                        private val eessiInformasjon: EessiInformasjon,
                        private val pensjonsinformasjonService: PensjonsinformasjonService) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillSEDService::class.java) }

    fun prefill(prefillData: PrefillDataModel): SED {

        val sedValue = SEDType.valueOf(prefillData.getSEDid())

        logger.debug("mapping prefillClass to SED: $sedValue")

        return when (sedValue) {
            SEDType.P6000 -> {
                PrefillP6000(prefillNav, eessiInformasjon, pensjonsinformasjonService, tpsPersonService).prefill(prefillData, hentPersoner(prefillData, true))
            }
            SEDType.P2000 -> {
                PrefillP2000(prefillNav, pensjonsinformasjonService, tpsPersonService).prefill(prefillData, hentPersoner(prefillData, true))
            }
            SEDType.P2200 -> {
                PrefillP2200(prefillNav, pensjonsinformasjonService, tpsPersonService).prefill(prefillData, hentPersoner(prefillData, true))
            }
            SEDType.P2100 -> {
                PrefillP2100(prefillNav, pensjonsinformasjonService, tpsPersonService).prefill(prefillData, hentPersoner(prefillData, true))
            }
            SEDType.P4000 -> {
                PrefillP4000(getPrefillSed(prefillData)).prefill(prefillData, hentPersoner(prefillData))
            }
            SEDType.P7000 -> {
                PrefillP7000(getPrefillSed(prefillData)).prefill(prefillData, hentPersoner(prefillData))
            }
            SEDType.P8000 -> {
                PrefillP8000(getPrefillSed(prefillData)).prefill(prefillData, hentPersoner(prefillData))
            }
            SEDType.P10000 -> {
                PrefillP10000(getPrefillSed(prefillData)).prefill(prefillData, hentPersoner(prefillData))
            }
            SEDType.X005 -> {
                PrefillX005(prefillNav).prefill(prefillData, hentPersoner(prefillData))
            }
            SEDType.H020, SEDType.H021 -> {
                PrefillH02X(getPrefillSed(prefillData)).prefill(prefillData, hentPersoner(prefillData))
            }
            else -> {
                //P3000_NO vil aldre gå dennee vei! men fra EU-SED->Nav-SED->PESYS
                //P3000_SE, PL, DK, DE, UK, ol vil gå denne veien.
                //P5000, - P9000, P14000 og og andre
                getPrefillSed(prefillData).prefill(prefillData, hentPersoner(prefillData))
            }
        }
    }

    private fun getPrefillSed(prefillData: PrefillDataModel) : PrefillSed {
        val pensjonGjenlevende = PrefillGjenlevende(tpsPersonService, prefillNav).prefill(prefillData)
        return PrefillSed(prefillNav, pensjonGjenlevende)
    }

    private fun hentPersoner(prefillData: PrefillDataModel, fyllUtBarnListe: Boolean = false): PersonData {
        // FIXME - det veksles mellom gjenlevende og bruker ... usikkert om dette er rett...
        val brukerEllerGjenlevende = tpsPersonService.hentBrukerFraTPS(prefillData.avdod?.norskIdent ?: prefillData.bruker.norskIdent)

        val brukerFraTps = tpsPersonService.hentBrukerFraTPS(prefillData.bruker.norskIdent)
        val (ektepinid, ekteTypeValue) = filterEktefelleRelasjon(brukerFraTps)

        val ektefelleBruker = if(ektepinid.isBlank()) null else tpsPersonService.hentBrukerFraTPS(ektepinid)

        val barnBrukereFraTPS =
                if (fyllUtBarnListe) {
                    barnsPinId(tpsPersonService.hentBrukerFraTPS(prefillData.bruker.norskIdent))
                            .mapNotNull { barn -> tpsPersonService.hentBrukerFraTPS(barn) }
                } else listOf()

        return PersonData(brukerEllerGjenlevende = brukerEllerGjenlevende, person = brukerFraTps, ektefelleBruker = ektefelleBruker, ekteTypeValue = ekteTypeValue, barnBrukereFraTPS = barnBrukereFraTPS)
    }

    private fun barnsPinId(brukerTPS: no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker?): List<String> {
        if (brukerTPS == null) return listOf()
            val person = brukerTPS as no.nav.tjeneste.virksomhet.person.v3.informasjon.Person

        val resultat = mutableListOf<String>()
        person.harFraRolleI.forEach {
            val tpsvalue = it.tilRolle.value   //mulig nullpoint? kan tilRolle være null?
            if (PrefillNav.Companion.RelasjonEnum.BARN.erSamme(tpsvalue)) {
                val persident = person.aktoer as PersonIdent
                val pinid: NorskIdent = persident.ident
                val norIdent = pinid.ident
                if (NavFodselsnummer(norIdent).validate()) {
                    resultat.add(norIdent)
                } else {
                    logger.error("følgende ident funnet ikke gyldig: $norIdent")
                }
            }
        }
        return resultat.toList()
    }

    private fun filterEktefelleRelasjon(bruker: no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker?): Pair<String, String> {
        val validRelasjoner = listOf("EKTE","REPA","SAMB")

        if (bruker == null) return Pair("","")
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

