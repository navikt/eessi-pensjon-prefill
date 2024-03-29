package no.nav.eessi.pensjon.prefill.sed

import no.nav.eessi.pensjon.eux.model.BucType.P_BUC_05
import no.nav.eessi.pensjon.eux.model.SedType.H020
import no.nav.eessi.pensjon.eux.model.SedType.H021
import no.nav.eessi.pensjon.eux.model.SedType.P10000
import no.nav.eessi.pensjon.eux.model.SedType.P15000
import no.nav.eessi.pensjon.eux.model.SedType.P2000
import no.nav.eessi.pensjon.eux.model.SedType.P2100
import no.nav.eessi.pensjon.eux.model.SedType.P2200
import no.nav.eessi.pensjon.eux.model.SedType.P4000
import no.nav.eessi.pensjon.eux.model.SedType.P5000
import no.nav.eessi.pensjon.eux.model.SedType.P6000
import no.nav.eessi.pensjon.eux.model.SedType.P7000
import no.nav.eessi.pensjon.eux.model.SedType.P8000
import no.nav.eessi.pensjon.eux.model.SedType.X005
import no.nav.eessi.pensjon.eux.model.SedType.X010
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.person.PrefillSed
import no.nav.eessi.pensjon.prefill.sed.krav.PrefillP2000
import no.nav.eessi.pensjon.prefill.sed.krav.PrefillP2100
import no.nav.eessi.pensjon.prefill.sed.krav.PrefillP2200
import no.nav.eessi.pensjon.prefill.sed.vedtak.PrefillP6000
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.utils.mapJsonToAny
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

@Component
class PrefillSEDService(private val eessiInformasjon: EessiInformasjon, private val prefillPDLnav: PrefillPDLNav) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillSEDService::class.java) }

    fun prefill(prefillData: PrefillDataModel, personDataCollection: PersonDataCollection): SED {
        return when (prefillData.sedType) {
            P6000 -> {
                PrefillP6000(
                    prefillPDLnav,
                    eessiInformasjon,
                    null
                ).prefill(
                    prefillData,
                    personDataCollection
                )
            }
            P2100 -> {
                val sedpair = PrefillP2100(prefillPDLnav).prefillSed(
                    prefillData,
                    personDataCollection,
                    null
                )
                prefillData.melding = sedpair.first
                sedpair.second
            }
            else -> {
                logger.warn("Benytter ordinær preutfylling for Gjenny for ${prefillData.sedType}")
                prefill(prefillData, personDataCollection, null)
            }
        }
    }
    fun prefill(prefillData: PrefillDataModel, personDataCollection: PersonDataCollection, pensjonCollection: PensjonCollection?): SED {

        logger.debug("mapping prefillClass to SED: ${prefillData.sedType}")

        return when (prefillData.sedType) {
            //krav
            P2000 -> {
                PrefillP2000(prefillPDLnav).prefillSed(
                    prefillData,
                    personDataCollection,
                    pensjonCollection?.sak,
                    pensjonCollection?.vedtak
                )
            }

            P2200 -> PrefillP2200(prefillPDLnav).prefill(
                prefillData,
                personDataCollection,
                pensjonCollection?.sak,
                pensjonCollection?.vedtak
            )
            P2100 -> {
                val sedpair = PrefillP2100(prefillPDLnav).prefillSed(
                    prefillData,
                    personDataCollection,
                    pensjonCollection?.sak
                )
                prefillData.melding = sedpair.first
                sedpair.second
            }

            //vedtak
            P6000 -> PrefillP6000(
                prefillPDLnav,
                eessiInformasjon,
                pensjonCollection?.pensjoninformasjon ?: throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ingen vedtak"
                )
            ).prefill(
                prefillData,
                personDataCollection
            )
            P5000 -> PrefillP5000(PrefillSed(prefillPDLnav)).prefill(prefillData, personDataCollection)
            P4000 -> PrefillP4000(PrefillSed(prefillPDLnav)).prefill(prefillData, personDataCollection)

            P7000 -> {
                if (prefillData.partSedAsJson[P7000.name] != null && prefillData.partSedAsJson[P7000.name] != "{}") {
                    logger.info("P7000mk2 preutfylling med data fra P6000..")
                    PrefillP7000Mk2Turbo(PrefillSed(prefillPDLnav)).prefill(prefillData, personDataCollection)
                } else {
                    logger.info("P7000 med forenklet preutfylling")
                    PrefillP7000(PrefillSed(prefillPDLnav)).prefill(prefillData, personDataCollection)
                }
            }

            P8000 -> {
                if (prefillData.buc == P_BUC_05) {
                    PrefillP8000(PrefillSed(prefillPDLnav)).prefill(
                        prefillData,
                        personDataCollection,
                        pensjonCollection?.sak
                    )
                } else {
                    PrefillP8000(PrefillSed(prefillPDLnav)).prefill(prefillData, personDataCollection, null)
                }
            }

            P15000 -> PrefillP15000(PrefillSed(prefillPDLnav)).prefill(
                prefillData,
                personDataCollection,
                pensjonCollection?.sak,
                pensjonCollection?.pensjoninformasjon
            )

            P10000 -> PrefillP10000(prefillPDLnav).prefill(
                prefillData.penSaksnummer,
                prefillData.bruker,
                prefillData.avdod,
                prefillData.getBankOgArbeidFromRequest(),
                personDataCollection
            )

            X005 -> PrefillX005(prefillPDLnav).prefill(
                prefillData.penSaksnummer,
                prefillData.bruker,
                prefillData.avdod,
                prefillData.getBankOgArbeidFromRequest(),
                prefillData.institution.first(),
                personDataCollection
            )

            X010 -> PrefillX010(prefillPDLnav).prefill(
                prefillData.penSaksnummer,
                prefillData.bruker,
                prefillData.avdod,
                prefillData.getBankOgArbeidFromRequest(),
                personDataCollection,
                prefillData.partSedAsJson[X010.name]?.let { payload -> mapJsonToAny(payload) }
            )

            H020, H021 -> PrefillH02X(prefillPDLnav).prefill(prefillData, personDataCollection)

            else ->
                //P3000_SE, PL, DK, DE, UK, med flere vil gå denne veien..
                //P9000, P14000, P15000.. med flere..
                PrefillSed(prefillPDLnav).prefill(prefillData, personDataCollection)
        }
    }

}
