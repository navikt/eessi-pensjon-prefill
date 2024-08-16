package no.nav.eessi.pensjon.prefill.sed

import no.nav.eessi.pensjon.eux.model.sed.P5000
import no.nav.eessi.pensjon.eux.model.sed.P5000Pensjon
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection

import no.nav.eessi.pensjon.prefill.person.PrefillSed
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillP5000(private val prefillSed: PrefillSed) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP5000::class.java) }

    fun prefill(prefillData: PrefillDataModel, personDataCollection: PersonDataCollection): P5000 {

        logger.info("Preutlfyll P5000: ")
        val sed = prefillSed.prefill(prefillData, personDataCollection)

        return P5000(
            nav = sed.nav,
            pensjon = P5000Pensjon(
                gjenlevende = sed.pensjon?.gjenlevende
            )
        )
    }

}
