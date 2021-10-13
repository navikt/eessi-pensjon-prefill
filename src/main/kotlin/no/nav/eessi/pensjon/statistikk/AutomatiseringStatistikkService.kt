package no.nav.eessi.pensjon.statistikk

import com.github.wnameless.json.flattener.JsonFlattener
import net.minidev.json.JSONObject
import net.minidev.json.parser.JSONParser
import no.nav.eessi.pensjon.eux.model.buc.BucType
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.utils.toJson
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class AutomatiseringStatistikkService(private val aivenKafkaTemplate: KafkaTemplate<String, String>) {

    private val logger = LoggerFactory.getLogger(AutomatiseringStatistikkService::class.java)

    private fun publiserAutomatiseringStatistikk(automatiseringMelding: PrefillAutomatiseringMelding) {
        logger.info("Produserer melding på kafka: ${aivenKafkaTemplate.defaultTopic}  melding: $automatiseringMelding")

        aivenKafkaTemplate.send(KafkaAutomatiseringMessage(automatiseringMelding)).get()
    }

    fun genererAutomatiseringStatistikk(sed: SED, bucType: String) {
        val parser = JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE)
        val obj = parser.parse(sed.toJson())
        val jsonObject = obj as JSONObject
        val flattenedJsonMap = JsonFlattener.flattenAsMap(jsonObject.toString())

//        if (logger.isDebugEnabled) {
//            flattenedJsonMap.forEach { (k: String, v: Any) -> logger.debug("$k : $v") }
//        }

        val listOfValues = flattenedJsonMap.filter { it.value != null }
        val listOfEmptyValues = flattenedJsonMap.filter { it.value == null }

        val antallFelter = flattenedJsonMap.size
        val antallPreutfylteFelter = listOfValues.size
        val antallTommeFelter = listOfEmptyValues.size
        logger.info("Buctype: $bucType, SedType: ${sed.type}, antall utfylt felt: $antallPreutfylteFelter, antall tomme felt: $antallTommeFelter, Total: $antallFelter")

        publiserAutomatiseringStatistikk(
            PrefillAutomatiseringMelding(
                sedVersjon = "1",
                bucType = BucType.valueOf(bucType),
                sedType = sed.type,
                antallPreutfylteFelter = antallPreutfylteFelter,
                antallTommeFelter = antallTommeFelter,
                antallFelter = antallFelter)
        )
    }
}