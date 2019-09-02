package no.nav.eessi.pensjon.fagmodul.eux

import no.nav.eessi.pensjon.fagmodul.eux.bucmodel.Buc
import no.nav.eessi.pensjon.fagmodul.eux.bucmodel.ShortDocumentItem
import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem

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
) {
    companion object {
        fun from(buc: Buc, aktoerid: String): BucAndSedView {
            val bucUtil = BucUtils(buc)
            return BucAndSedView(
                    type = bucUtil.getProcessDefinitionName()!!,
                    creator = bucUtil.getCaseOwner(),
                    caseId = buc.id?: "n/a",
                    sakType = "",
                    startDate = bucUtil.getStartDateLong(),
                    lastUpdate = bucUtil.getLastDateLong(),
                    aktoerId = aktoerid,
                    status = bucUtil.getStatus(),
                    institusjon = bucUtil.getParticipants().map {
                        InstitusjonItem(
                                country = it.organisation?.countryCode ?: "",
                                institution = it.organisation?.id ?: "",
                                name = it.organisation?.name
                        )
                    },
                    seds = bucUtil.getAllDocuments()
            )
        }
    }
}


