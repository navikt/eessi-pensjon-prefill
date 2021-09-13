package no.nav.eessi.pensjon.prefill.sed

import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.eux.model.sed.SedType
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.models.PensjonCollection
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModel
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.eessi.pensjon.prefill.person.PrefillSed
import no.nav.eessi.pensjon.prefill.sed.krav.PrefillP2000
import no.nav.eessi.pensjon.prefill.sed.krav.PrefillP2100
import no.nav.eessi.pensjon.prefill.sed.krav.PrefillP2200
import no.nav.eessi.pensjon.prefill.sed.vedtak.PrefillP6000
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

@Component
class PrefillSEDService(private val eessiInformasjon: EessiInformasjon, private val prefillPDLnav: PrefillPDLNav) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillSEDService::class.java) }

    fun prefill(prefillData: PrefillDataModel, personDataCollection: PersonDataCollection, pensjonCollection: PensjonCollection): SED {

        val sedType = prefillData.sedType

        logger.debug("mapping prefillClass to SED: $sedType")

        return when (sedType) {
            //krav
            SedType.P2000 -> {
                PrefillP2000(prefillPDLnav).prefillSed(
                    prefillData,
                    personDataCollection,
                    pensjonCollection.sak,
                    pensjonCollection.vedtak
                )
            }

            SedType.P2200 -> PrefillP2200(prefillPDLnav).prefill(
                prefillData,
                personDataCollection,
                pensjonCollection.sak,
                pensjonCollection.vedtak
            )
            SedType.P2100 -> {
                val sedpair = PrefillP2100(prefillPDLnav).prefillSed(
                    prefillData,
                    personDataCollection,
                    pensjonCollection.sak
                    )
                prefillData.melding = sedpair.first
                sedpair.second
            }

            //vedtak
            SedType.P6000 -> PrefillP6000(
                    prefillPDLnav,
                    eessiInformasjon,
                    pensjonCollection.pensjoninformasjon ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingen vedtak")
                ).prefill(
                prefillData,
                personDataCollection
            )
            SedType.P5000 -> PrefillP5000(PrefillSed(prefillPDLnav)).prefill(prefillData, personDataCollection)
            SedType.P4000 -> PrefillP4000(PrefillSed(prefillPDLnav)).prefill(prefillData, personDataCollection)

            SedType.P7000 -> {
                if (prefillData.partSedAsJson[SedType.P7000.name] != null && prefillData.partSedAsJson[SedType.P7000.name] != "{}") {
                    logger.info("P7000mk2 preutfylling med data fra P6000..")
                    PrefillP7000Mk2Turbo(PrefillSed(prefillPDLnav)).prefill(prefillData, personDataCollection)
                } else {
                    logger.info("P7000 med forenklet preutfylling")
                    PrefillP7000(PrefillSed(prefillPDLnav)).prefill(prefillData, personDataCollection)
                }
            }

            SedType.P8000 -> {
                if (prefillData.buc == "P_BUC_05") {
                    PrefillP8000(PrefillSed(prefillPDLnav)).prefill(prefillData, personDataCollection, pensjonCollection.sak)
//                    try {
//                    } catch (ex: Exception) {
//                        logger.error(ex.message)
//                        PrefillP8000(PrefillSed(prefillPDLnav)).prefill(prefillData, personDataCollection, null)
//                    }
                } else {
                    PrefillP8000(PrefillSed(prefillPDLnav)).prefill(prefillData, personDataCollection, null)
                }
            }

            SedType.P15000 -> PrefillP15000(PrefillSed(prefillPDLnav)).prefill(
                prefillData,
                personDataCollection,
                pensjonCollection.sak,
                pensjonCollection.pensjoninformasjon
            )

            SedType.P10000 -> PrefillP10000(prefillPDLnav).prefill(
                prefillData.penSaksnummer,
                prefillData.bruker,
                prefillData.avdod,
                prefillData.getPersonInfoFromRequestData(),
                personDataCollection
            )

            SedType.X005 -> PrefillX005(prefillPDLnav).prefill(
                prefillData.penSaksnummer,
                prefillData.bruker,
                prefillData.avdod,
                prefillData.getPersonInfoFromRequestData(),
                prefillData.institution.first(),
                personDataCollection)

            SedType.X010 -> PrefillX010(prefillPDLnav).prefill(
                prefillData.penSaksnummer,
                prefillData.bruker,
                prefillData.avdod,
                prefillData.getPersonInfoFromRequestData(),
                personDataCollection
            )

            SedType.H020, SedType.H021 -> PrefillH02X(prefillPDLnav).prefill(prefillData, personDataCollection)

            else ->
                //P3000_SE, PL, DK, DE, UK, med flere vil gå denne veien..
                //P9000, P14000, P15000.. med flere..
                PrefillSed(prefillPDLnav).prefill(prefillData, personDataCollection)
        }
    }

}
