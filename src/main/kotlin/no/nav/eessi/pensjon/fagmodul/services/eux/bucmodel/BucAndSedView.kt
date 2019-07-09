package no.nav.eessi.pensjon.fagmodul.services.eux.bucmodel

import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import java.time.LocalDate

data class BucAndSedView(
        val type: String,
        val creator: InstitusjonItem,
        val caseId: String,
        val sakType: String? = null,
        val aktoerId: String,
        val status: String? = null,
        val startDate: Long? = null,
        val lastUpdate: Long? = null,
        val institusjon: List<InstitusjonItem>,
        val seds: List<ShortDocumentItem>
)


