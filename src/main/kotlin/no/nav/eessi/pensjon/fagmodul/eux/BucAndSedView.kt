package no.nav.eessi.pensjon.fagmodul.eux

import no.nav.eessi.pensjon.fagmodul.eux.bucmodel.Buc
import no.nav.eessi.pensjon.fagmodul.eux.bucmodel.ShortDocumentItem
import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.utils.toJson

data class BucAndSedView(
        val type: String,
        val caseId: String,
        val creator: InstitusjonItem? = null,
        val sakType: String? = null,
        val status: String? = null,
        val startDate: Long? = null,
        val lastUpdate: Long? = null,
        val institusjon: List<InstitusjonItem>? = null,
        val seds: List<ShortDocumentItem>? = null,
        val error: String? = null
) {
    override fun toString(): String {
        return toJson()
    }

    companion object {
        fun fromErr(error: String?): BucAndSedView {
            return BucAndSedView(
                    type = "",
                    caseId = "",
                    error = error
            )
        }
        fun from(buc: Buc): BucAndSedView {
            val bucUtil = BucUtils(buc)
            return BucAndSedView(
                    type = bucUtil.getProcessDefinitionName()!!,
                    creator = bucUtil.getCaseOwnerOrCreator(),
                    caseId = buc.id?: "n/a",
                    startDate = bucUtil.getStartDateLong(),
                    lastUpdate = bucUtil.getLastDateLong(),
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


