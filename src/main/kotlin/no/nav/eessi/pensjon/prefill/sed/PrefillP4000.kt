package no.nav.eessi.pensjon.prefill.sed

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.eessi.pensjon.eux.model.sed.P4000
import no.nav.eessi.pensjon.eux.model.sed.PersonArbeidogOppholdUtland
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModel
import no.nav.eessi.pensjon.prefill.models.person.PrefillSed
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.typeRefs
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillP4000(private val prefillSed: PrefillSed) {

    private val logger: Logger =  LoggerFactory.getLogger(PrefillP4000::class.java)
    private val mapper = jacksonObjectMapper()

    fun prefill(prefillData: PrefillDataModel, personData: PersonDataCollection): P4000 {
        return try {
            val sed = prefillSed.prefill(prefillData, personData)
            P4000(
                nav = sed.nav,
                trygdetid = perfillPersonTrygdetid(prefillData)
            )
        } catch (ex: Exception) {
            logger.error("Feiler ved prefill P4000 person", ex)
            P4000(prefillData.sedType)
        }
    }

    private fun perfillPersonTrygdetid(prefillData: PrefillDataModel) : PersonArbeidogOppholdUtland? {
            val p4000json = prefillData.getPartSEDasJson("P4000")

            return try {
                val personDataNode = mapper.readTree(p4000json)

                logger.debug("Prøver å preutfylle mappe om en p4000-innseningdata til P4000")
                val personDataJson =  personDataNode["periodeInfo"].toString()
                mapJsonToAny(personDataJson, typeRefs())

            } catch (ex: Exception) {
                logger.debug("Feiler ved mapping av P4000, fortsetter")
                null
            }
    }

}