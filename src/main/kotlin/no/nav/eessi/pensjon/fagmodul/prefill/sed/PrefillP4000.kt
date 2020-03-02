package no.nav.eessi.pensjon.fagmodul.prefill.sed

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillPerson
import no.nav.eessi.pensjon.fagmodul.sedmodel.PersonArbeidogOppholdUtland
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.typeRefs
import org.bouncycastle.asn1.x500.style.RFC4519Style.l
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillP4000(private val prefillPerson: PrefillPerson) : Prefill<SED> {

    private val logger: Logger =  LoggerFactory.getLogger(PrefillP4000::class.java)
    private val mapper = jacksonObjectMapper()

    override fun prefill(prefillData: PrefillDataModel): SED {
        return try {
            val sed = prefillPerson.prefill(prefillData)
            sed.trygdetid = perfillPersonTrygdetid(sed, prefillData)
            prefillData.sed

        } catch (ex: Exception) {
            logger.error("Feiler ved prefill P4000 person", ex)
            prefillData.sed
        }
    }

    private fun perfillPersonTrygdetid(sed: SED, prefillData: PrefillDataModel) : PersonArbeidogOppholdUtland? {
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