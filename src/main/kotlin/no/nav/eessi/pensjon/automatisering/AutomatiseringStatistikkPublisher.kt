package no.nav.eessi.pensjon.automatisering

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class AutomatiseringStatistikkPublisher(private val aivenKafkaTemplate: KafkaTemplate<String, String>) {

    private val logger = LoggerFactory.getLogger(AutomatiseringStatistikkPublisher::class.java)

    fun publiserAutomatiseringStatistikk(automatiseringMelding: PrefillAutomatiseringMelding) {
        logger.info("Produserer melding på kafka: ${aivenKafkaTemplate.defaultTopic}  melding: $automatiseringMelding")

        aivenKafkaTemplate.send(KafkaAutomatiseringMessage(automatiseringMelding))
    }
}