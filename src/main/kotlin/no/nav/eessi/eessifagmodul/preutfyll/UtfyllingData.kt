package no.nav.eessi.eessifagmodul.preutfyll

import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.models.createSED

class UtfyllingData {

    private lateinit var sed: SED
    private var pin: String? = ""

    //sector
    private var subjectArea: String? = null
    //PEN-saksnummer
    private var caseId: String? = null
    private var buc: String? = null
    //private var sedID : String? = null
    //mottakere
    private var institutions: MutableList<InstitusjonItem> = mutableListOf()
    //aktoerid
    private var aktoerID: String? = null

    fun mapFromRequest(subject: String, caseId: String, buc: String, sedID: String, aktoerID: String): UtfyllingData {
        println("mapRequest: $subject, $caseId, $buc, $sedID, $aktoerID")
        this.subjectArea =  subject
        this.caseId = caseId
        this.buc = buc
        this.aktoerID = aktoerID
        this.sed = createSED(sedID)
        return this
    }

    //Pinid (FNR) aktorID
    fun hentPinid(): String? { return pin }

    fun hentAktoerid(): String? {
        return aktoerID
    }

    fun hentSED(): SED {
        return sed
    }

    fun putPinID(pinid: String?) {
        this.pin = pinid
    }

    fun hentSaksnr(): String? {
        return caseId
    }

    fun addInstitutions(item: InstitusjonItem) {
        institutions.add(item)
    }

    fun setInstitutions(list: List<InstitusjonItem>) {
        institutions = list as MutableList<InstitusjonItem>
    }

    fun hentInstitutionsList():List<InstitusjonItem> {
        return institutions.toList()
    }

}

data class InstitusjonItem(
        val country: String? = null,
        val institution: String? = null
)

