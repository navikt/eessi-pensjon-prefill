package no.nav.eessi.pensjon.listeners

import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import org.slf4j.MDC
import java.util.*

@Service
class SedListener {

    private val logger = LoggerFactory.getLogger(SedListener::class.java)

    //@KafkaListener(topics = ["\${kafka.sedSendt.topic}"], groupId = "\${kafka.sedSendt.groupid}")
    fun consumeSedSendt(hendelse: String, acknowledgment: Acknowledgment) {
        MDC.putCloseable("x_request_id", UUID.randomUUID().toString()).use {
            logger.info("Innkommet sedSendt hendelse")
            logger.debug(hendelse)
            try {
                acknowledgment.acknowledge()
            } catch (ex: Exception) {
                logger.error(
                        "Noe gikk galt under behandling av SED-hendelse:\n $hendelse \n" +
                                "${ex.message}", ex)
                throw RuntimeException(ex.message)
            }
        }
    }

    //@KafkaListener(topics = ["\${kafka.sedMottatt.topic}"], groupId = "\${kafka.sedMottatt.groupid}")
    fun consumeSedMottatt(hendelse: String, acknowledgment: Acknowledgment) {
        MDC.putCloseable("x_request_id", UUID.randomUUID().toString()).use {
            logger.info("Innkommet sedMottatt hendelse")
            logger.debug(hendelse)
            try {
                acknowledgment.acknowledge()
            } catch (ex: Exception) {
                logger.error(
                        "Noe gikk galt under behandling av SED-hendelse:\n $hendelse \n" +
                                "${ex.message}", ex)
                throw RuntimeException(ex.message)
            }
        }
    }
}

