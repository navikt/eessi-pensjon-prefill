package no.nav.eessi.pensjon.prefill.sed.vedtak

import no.nav.eessi.pensjon.eux.model.sed.P6000
import no.nav.eessi.pensjon.prefill.EtterlatteService.EtterlatteVedtakResponseData
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.sed.vedtak.PrefillP6000Pensjon.prefillP6000Pensjon
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class PrefillP6000(
    private val prefillNav: PrefillPDLNav,
    private val eessiInfo: EessiInformasjon,
    private val pensjoninformasjon: Pensjonsinformasjon?,
) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP6000::class.java) }

    fun prefill(prefillData: PrefillDataModel, personData: PersonDataCollection, etterlatteRespData: EtterlatteVedtakResponseData?): P6000 {
        val sedType = prefillData.sedType

        logger.info(
            "----------------------------------------------------------"
                    + "\nPreutfylling Pensjon : P6000 "
                    + "\n------------------| Preutfylling [$sedType] START |------------------ "
        )

        logger.info("Henter ut lokal kontakt, institusjon (NAV Utland)")
        val andreInstitusjondetaljer = eessiInfo.asAndreinstitusjonerItem()
        logger.info("Andreinstitusjoner: $andreInstitusjondetaljer ")

        logger.debug("Henter opp Persondata/Gjenlevende fra TPS")
        val gjenlevende = prefillData.avdod?.let { prefillNav.createGjenlevende(personData.forsikretPerson, prefillData.bruker) }

        val p6000Pensjon = if(pensjoninformasjon != null) {
            logger.debug("Prefiller P6000 med Pensjonsdata fra PESYS")
            prefillP6000Pensjon(pensjoninformasjon, gjenlevende, andreInstitusjondetaljer)
        } else {
            logger.debug("Prefiller med Pensjonsdata fra Gjenny, med vedtak: $etterlatteRespData")
            PrefillP6000GjennyPensjon().prefillP6000GjennyPensjon(
                gjenlevende,
                etterlatteRespData
            )
        }

        logger.debug("Henter opp Persondata fra TPS")
        val nav = prefillNav.prefill(
            penSaksnummer = prefillData.penSaksnummer,
            bruker = prefillData.bruker,
            avdod = prefillData.avdod,
            personData = personData,
            bankOgArbeid = prefillData.getBankOgArbeidFromRequest(),
            krav = p6000Pensjon?.kravDato,
            annenPerson = null
        )

        logger.info("-------------------| Preutfylling [$sedType] END |------------------- ")

        return P6000(
            type = sedType,
            nav = nav,
            pensjon = p6000Pensjon
        )

    }

}

