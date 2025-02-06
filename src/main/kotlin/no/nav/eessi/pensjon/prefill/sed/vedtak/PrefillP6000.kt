package no.nav.eessi.pensjon.prefill.sed.vedtak

import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.AndreinstitusjonerItem
import no.nav.eessi.pensjon.eux.model.sed.Bruker
import no.nav.eessi.pensjon.eux.model.sed.P6000
import no.nav.eessi.pensjon.eux.model.sed.P6000Pensjon
import no.nav.eessi.pensjon.prefill.etterlatte.EtterlatteService
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.sed.vedtak.PrefillP6000Pensjon.prefillP6000Pensjon
import no.nav.eessi.pensjon.prefill.sed.vedtak.PrefillP6000Pensjon.prefillP6000PensjonVedtak
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class PrefillP6000(
    private val prefillNav: PrefillPDLNav,
    private val eessiInfo: EessiInformasjon,
    private val etterlatteService: EtterlatteService) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP6000::class.java) }

    fun prefill(prefillData: PrefillDataModel, personData: PersonDataCollection, pensjoninformasjon: Pensjonsinformasjon?): P6000 {
        val (sedType, andreInstitusjondetaljer, gjenlevende) = prefill6000Basis(prefillData, personData)

        logger.debug("Henter opp Pensjonsdata fra PESYS")
        val p6000Pensjon = if(pensjoninformasjon != null) prefillP6000Pensjon(pensjoninformasjon, gjenlevende, andreInstitusjondetaljer) else P6000Pensjon(gjenlevende)

        logger.debug("Starter prefill for P6000")
        val nav = prefillNav.prefill(
            penSaksnummer = prefillData.penSaksnummer,
            bruker = prefillData.bruker,
            avdod = prefillData.avdod,
            personData = personData,
            bankOgArbeid = prefillData.getBankOgArbeidFromRequest(),
            krav = p6000Pensjon.kravDato,
            annenPerson = null
        )

        logger.info("-------------------| Preutfylling [$sedType] END |------------------- ")

        return P6000(
            type = sedType,
            nav = nav,
            pensjon = p6000Pensjon
        )

    }

    fun prefillMedVedtak(prefillData: PrefillDataModel, personData: PersonDataCollection): P6000 {
        val (sedType, andreInstitusjondetaljer, gjenlevende) = prefill6000Basis(prefillData, personData)
        val vedtakResponse = etterlatteService.hentGjennySak(prefillData.bruker.norskIdent)

        if(vedtakResponse.isFailure){
            logger.error("Kunne ikke hente vedtak fra Gjenny: ${vedtakResponse.exceptionOrNull()}")
        }
        val vedtak = vedtakResponse.getOrNull()
        logger.debug("Lager pensjondata for P6000 fra gjenny")
        val p6000Pensjon =  prefillP6000PensjonVedtak(gjenlevende,vedtak, andreInstitusjondetaljer)

        logger.debug("Henter opp Persondata fra PDL")
        val nav = prefillNav.prefill(
            penSaksnummer = prefillData.penSaksnummer,
            bruker = prefillData.bruker,
            avdod = prefillData.avdod,
            personData = personData,
            bankOgArbeid = prefillData.getBankOgArbeidFromRequest(),
            krav = p6000Pensjon.kravDato,
            annenPerson = null
        )

        logger.info("-------------------| Preutfylling [$sedType] med gjenny vedtak END |------------------- ")

        return P6000(
            type = sedType,
            nav = nav,
            pensjon = p6000Pensjon
        )

    }

    private fun prefill6000Basis(
        prefillData: PrefillDataModel,
        personData: PersonDataCollection
    ): Triple<SedType, AndreinstitusjonerItem, Bruker?> {
        val sedType = prefillData.sedType

        logger.info("----------------------------------------------------------"
                    + "\nPreutfylling Pensjon : P6000 "
                    + "\n------------------| Preutfylling [$sedType] START |------------------ "
        )

        logger.info("Henter ut lokal kontakt, institusjon (NAV Utland)")
        val andreInstitusjondetaljer = eessiInfo.asAndreinstitusjonerItem()
        logger.info("Andreinstitusjoner: $andreInstitusjondetaljer ")

        logger.debug("Henter gjenlevende info for P6000")
        val gjenlevende = prefillData.avdod?.let { prefillNav.createGjenlevende(personData.forsikretPerson, prefillData.bruker) }
        return Triple(sedType, andreInstitusjondetaljer, gjenlevende)
    }

}

