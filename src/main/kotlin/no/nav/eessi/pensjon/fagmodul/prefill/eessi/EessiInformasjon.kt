package no.nav.eessi.pensjon.fagmodul.prefill.eessi

import no.nav.eessi.pensjon.fagmodul.sedmodel.AndreinstitusjonerItem
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class EessiInformasjon {

    private val logger: Logger by lazy { LoggerFactory.getLogger(EessiInformasjon::class.java) }

    @Value("\${eessi.pensjon_lokalid}")
    lateinit var institutionid: String

    @Value("\${eessi.pensjon_lokalnavn}")
    lateinit var institutionnavn: String

    @Value("\${eessi.pensjon_adresse_gate}")
    lateinit var institutionGate: String

    @Value("\${eessi.pensjon_adresse_by}")
    lateinit var institutionBy: String

    @Value("\${eessi.pensjon_adresse_postnummer}")
    lateinit var institutionPostnr: String

    @Value("\${eessi.pensjon_adresse_land}")
    lateinit var institutionLand: String


    fun mapEssiInformasjonTilPrefillDataModel(prefillDataModel: PrefillDataModel): PrefillDataModel {
        prefillDataModel.andreInstitusjon = AndreinstitusjonerItem(
                institusjonsid = institutionid,
                institusjonsnavn = institutionnavn,
                institusjonsadresse = institutionGate,
                postnummer = institutionPostnr,
                bygningsnr = null,
                land = institutionLand,
                region = null,
                poststed = institutionBy
        )

        return prefillDataModel
    }

}