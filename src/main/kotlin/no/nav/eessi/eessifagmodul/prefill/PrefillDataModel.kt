package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.InstitusjonItem
import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.models.createSED

/**
 * Data class to store different required data to build any given sed, auto or semiauto.
 *
 * sed, aktoerid,  psak-saknr, rinanr, institutions (mottaker eg. nav),
 *
 * servives :  aktoerid, tps, pen, maybe joark, eux-basis.
 *
 */
class PrefillDataModel {

    private lateinit var sed: SED
    private var pin: String = ""

    //sector
    private lateinit var subject: String
    //PEN-saksnummer
    private lateinit var  caseId: String
    //Vedtak?
    private lateinit var vedtakId: String
    //Krav?
    private lateinit var karavId: String

    private lateinit var  buc: String
    //private var sedID : String? = null
    //mottakere
    private lateinit var institution: List<InstitusjonItem>
    //aktoerid
    private lateinit var aktoerID: String

    //partlysed
    private var partSedasJson: MutableMap<String, String> = mutableMapOf()

    fun build(subject: String, caseId: String, buc: String, sedID: String, aktoerID: String, institutions: List<InstitusjonItem>, payload: String): PrefillDataModel {
        this.subject =  subject
        this.caseId = caseId
        this.buc = buc
        this.aktoerID = aktoerID
        this.sed = createSED(sedID)
        this.institution = institutions
        this.partSedasJson.put(sedID, payload)
        return this
    }

    fun build(subject: String, caseId: String, buc: String, sedID: String, aktoerID: String, institutions: List<InstitusjonItem>): PrefillDataModel {
        this.subject =  subject
        this.caseId = caseId
        this.buc = buc
        this.aktoerID = aktoerID
        this.sed = createSED(sedID)
        this.institution = institutions
        return this
    }

    //Pinid (FNR) aktorID
    fun hentPinid(): String { return pin}

    fun hentAktoerid(): String {
        return aktoerID
    }

    fun hentSED(): SED {
        return sed
    }

    fun hentSEDid(): String {
        return sed.sed!!
    }

    fun putPinID(pinid: String) {
        this.pin = pinid
    }

    fun hentSaksnr(): String {
        return caseId
    }

    fun hentPartSedasJson(key: String): String {
        return partSedasJson[key].orEmpty()
    }

    fun hentInstitutionsList(): List<InstitusjonItem> {
        return institution
    }

}
