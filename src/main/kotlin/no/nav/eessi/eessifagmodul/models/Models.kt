package no.nav.eessi.eessifagmodul.models

import java.util.*

data class OpprettEUFlytRequest(
        val KorrelasjonsID: UUID,
        val BUC: BUC,
        val SED: SED?,
        val Vedlegg: List<Any>? = null
)

data class OpprettEUFlytResponse(
        val KorrelasjonsID: UUID
)

data class BUC(
        val flytType: String,
        val saksbehandler: String,
        val saksnummerPensjon: String,
        val Parter: SenderReceiver
)

data class SED(
        val SEDType: String,
        val NAVSaksnummer: String?,
        val ForsikretPerson: NavPerson,
        val Barn: List<NavPerson>? = null,
        val Samboer: NavPerson? = null
)

data class NavPerson(
        val fnr: String
)

data class SenderReceiver(
        val sender: Institusjon,
        val receiver: List<Institusjon>
)

data class Institusjon(
        val landkode: String,
        val navn: String
)
