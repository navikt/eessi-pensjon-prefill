package no.nav.eessi.eessifagmodul.services

import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.prefill.PrefillDataModel
import no.nav.eessi.eessifagmodul.prefill.PrefillSED
import no.nav.eessi.eessifagmodul.services.eux.EuxService
import no.nav.eessi.eessifagmodul.services.eux.RinaActions
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import org.springframework.stereotype.Service
import java.util.*

@Service
class PrefillService(private val euxService: EuxService, private val prefillSED: PrefillSED, private val rinaActions: RinaActions) {

    fun prefillSed(dataModel: PrefillDataModel): PrefillDataModel {
        return prefillSED.prefill(dataModel)
    }

    fun prefillAndAddSedOnExistingCase(dataModel: PrefillDataModel): PrefillDataModel {

        val data = prefillSed(dataModel)
        val korrid = UUID.randomUUID().toString()
        val sedAsJson = mapAnyToJson(data.sed, true)

        if (checkForCreateStatus(data.euxCaseID, data.getSEDid())) {

            euxService.createSEDonExistingRinaCase(sedAsJson, data.euxCaseID, korrid)
            //ingen ting tilbake.. sjekke om alt er ok?
            //val aksjon = euxService.getPossibleActions(rinanr)
            dataModel.euxCaseID = checkForUpdateStatus(data.euxCaseID, data.getSEDid())
            return dataModel
        }
        throw SedDokumentIkkeGyldigException("Kan ikke opprette følgende  SED: ${{ data.getSEDid() }} på RINANR: ${data.euxCaseID}")

    }

    fun prefillAndCreateSedOnNewCase(dataModel: PrefillDataModel): PrefillDataModel {

        val data = prefillSed(dataModel)
        val payload = mapAnyToJson(data.sed, true)
        val korrid = UUID.randomUUID().toString()

        val euxCaseId = euxService.createCaseAndDocument(
                jsonPayload = payload,
                bucType = data.buc,
                fagSaknr = data.penSaksnummer,
                mottaker = getFirstInstitution(data.institution),
                korrelasjonID = korrid
        )

        dataModel.euxCaseID = checkForUpdateStatus(euxCaseId, data.getSEDid())

        return dataModel
    }

    private fun checkForCreateStatus(euxCaseId: String, sedName: String): Boolean {
        return rinaActions.canCreate(sedName, euxCaseId)
    }

    private fun checkForUpdateStatus(euxCaseId: String, sedName: String): String {
        if (rinaActions.canUpdate(sedName, euxCaseId)) {
            return "{\"euxcaseid\":\"$euxCaseId\"}"
        }
        throw SedDokumentIkkeOpprettetException("SED dokument feilet ved opprettelse ved RINANR: $euxCaseId")
    }

    //muligens midlertidig metode for å sende kun en mottaker til EUX.
    //TODO: funksjon for å legge til flere mottaker (InstitusjonItem) til Rina/SED etter oppretting.
    private fun getFirstInstitution(institutions: List<InstitusjonItem>): String {
        institutions.forEach {
            return it.institution ?: throw IkkeGyldigKallException("institujson kan ikke være tom")
        }
        throw IkkeGyldigKallException("Mangler mottaker register (InstitusjonItem)")
    }


}