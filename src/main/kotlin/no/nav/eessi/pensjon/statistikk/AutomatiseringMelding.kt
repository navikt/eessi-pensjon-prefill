package no.nav.eessi.pensjon.statistikk

import no.nav.eessi.pensjon.eux.model.buc.BucType
import no.nav.eessi.pensjon.eux.model.sed.SedType
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHeaders
import java.time.LocalDateTime

data class PrefillAutomatiseringMelding (
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    val sedVersjon: String,
    val bucType: BucType,
    val sedType: SedType,
    val antallPreutfylteFelter: Int,
    val antallTommeFelter: Int,
    val antallFelter: Int
)

class KafkaAutomatiseringMessage(
    private val payload: PrefillAutomatiseringMelding
): Message<PrefillAutomatiseringMelding> {
    override fun getPayload(): PrefillAutomatiseringMelding = payload
    override fun getHeaders(): MessageHeaders = MessageHeaders(mapOf("hendelsetype" to "PREUTFYLLINGSTATISTIKK", "opprettetTidspunkt" to LocalDateTime.now()))
}

