package no.nav.eessi.eessifagmodul.preutfyll

import no.nav.eessi.eessifagmodul.models.InstitusjonItem
import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.models.createSED

class UtfyllingData {

    private lateinit var sed: SED
    private var pin: String? = ""

    //sector
    private lateinit var subject: String
    //PEN-saksnummer
    private lateinit var  caseId: String
    private lateinit var  buc: String
    //private var sedID : String? = null
    //mottakere
    private lateinit var institutions: List<InstitusjonItem>
    //aktoerid
    private lateinit var aktoerID: String

    fun build(subject: String, caseId: String, buc: String, sedID: String, aktoerID: String, data: List<InstitusjonItem>): UtfyllingData {
        println("mapRequest: $subject, $caseId, $buc, $sedID, $aktoerID")
        this.subject =  subject
        this.caseId = caseId
        this.buc = buc
        this.aktoerID = aktoerID
        this.sed = createSED(sedID)
        this.institutions = data
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

    fun hentInstitutionsList(): List<InstitusjonItem> {
        return institutions
    }

}
