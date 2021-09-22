package no.nav.eessi.pensjon.automatisering

import no.nav.eessi.pensjon.eux.model.buc.BucType
import no.nav.eessi.pensjon.eux.model.sed.SedType
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHeaders
import java.time.LocalDateTime

data class PrefillAutomatiseringMelding (
    val opprettetTidspunkt: LocalDateTime,
    val sedVersjon: String,
    val bucType: BucType,
    val sedType: SedType,
    val prefiltfelt: Int,
    val tommefelt: Int,
    val totalfelt: Int
)

class KafkaAutomatiseringMessage(
    private val payload: PrefillAutomatiseringMelding
): Message<PrefillAutomatiseringMelding> {
    override fun getPayload(): PrefillAutomatiseringMelding = payload
    override fun getHeaders(): MessageHeaders = MessageHeaders(mapOf("hendelsetype" to "PREFILL", "opprettetTidspunkt" to LocalDateTime.now()))
}

