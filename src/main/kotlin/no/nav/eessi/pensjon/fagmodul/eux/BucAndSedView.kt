package no.nav.eessi.pensjon.fagmodul.eux

import no.nav.eessi.pensjon.fagmodul.eux.bucmodel.Buc
import no.nav.eessi.pensjon.fagmodul.eux.bucmodel.ShortDocumentItem
import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.utils.mapAnyToJson

data class BucAndSedView(
        val type: String,
        val caseId: String,
        val creator: InstitusjonItem? = null,
        val sakType: String? = null,
        val aktoerId: String,
        val status: String? = null,
        val startDate: Long? = null,
        val lastUpdate: Long? = null,
        val institusjon: List<InstitusjonItem>,
        val seds: List<ShortDocumentItem>
) {
    fun toJson(): String {
        return mapAnyToJson(this, false)
    }

    fun toJsonSkipEmpty(): String {
        return mapAnyToJson(this, true)
    }

    override fun toString(): String {
        return toJson()
    }

    companion object {
        fun from(buc: Buc, aktoerid: String): BucAndSedView {
            val bucUtil = BucUtils(buc)
            return BucAndSedView(
                    type = bucUtil.getProcessDefinitionName()!!,
                    creator = bucUtil.getCaseOwnerOrCreator(),
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


