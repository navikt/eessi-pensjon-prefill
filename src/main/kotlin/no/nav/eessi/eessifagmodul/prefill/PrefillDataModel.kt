package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.clients.aktoerid.AktoerIdClient
import no.nav.eessi.eessifagmodul.models.InstitusjonItem
import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.models.createSED
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository

/**
 * Data class to store different required data to build any given sed, auto or semiauto.
 *
 * sed, aktoerid,  psak-saknr, rinanr, institutions (mottaker eg. nav),
 *
 * servives:  aktoerid, tps, pen, maybe joark, eux-basis.
 *
 */
@Component
class PrefillDataModel(private val aktoerIdClient: AktoerIdClient) {

    private lateinit var sed: SED
    private var pin: String = ""

    //Rina Sector
    private lateinit var subject: String
    //PEN-saksnummer
    private lateinit var  caseId: String
    private lateinit var vedtakId: String
    private lateinit var karavId: String

    private lateinit var  buc: String
    private lateinit var institution: List<InstitusjonItem>
    private lateinit var aktoerID: String

    //aktorid-etterlatt - når gjenlevende fylles ut?
    private var etterlattAktoerID: String? = null
    private var etterlattPin: String? = null

    private var partSedasJson: MutableMap<String, String> = mutableMapOf()

    //euxCaseId (RINAcaseID)
    private lateinit var euxCaseID: String

    @Throws(RuntimeException::class)
    fun build(subject: String, caseId: String, buc: String, sedID: String, aktoerID: String, institutions: List<InstitusjonItem>, payload: String, euxcaseId: String): PrefillDataModel {
        this.subject =  subject
        this.caseId = caseId
        this.buc = buc
        this.aktoerID = aktoerID
        this.sed = createSED(sedID)
        this.institution = institutions
        this.partSedasJson.put(sedID, payload)
        this.euxCaseID = euxcaseId
        this.pin = hentAktoerIdPin(aktoerID)
        println(debug())
        return this
    }

    @Throws(RuntimeException::class)
    fun build(subject: String, caseId: String, buc: String, sedID: String, aktoerID: String, institutions: List<InstitusjonItem>): PrefillDataModel {
        this.subject =  subject
        this.caseId = caseId
        this.buc = buc
        this.aktoerID = aktoerID
        this.sed = createSED(sedID)
        this.institution = institutions
        this.pin = hentAktoerIdPin(aktoerID)
        println(debug())
        return this
    }

    @Throws(RuntimeException::class)
    fun build(subject: String, caseId: String, buc: String, sedID: String, aktoerID: String, institutions: List<InstitusjonItem>, dodaktorid: String): PrefillDataModel {
        this.subject =  subject
        this.caseId = caseId
        this.buc = buc
        this.aktoerID = aktoerID
        this.sed = createSED(sedID)
        this.institution = institutions
        this.pin = hentAktoerIdPin(aktoerID)
        this.etterlattAktoerID = dodaktorid
        this.etterlattPin = hentAktoerIdPin(dodaktorid)
        println(debug())
        return this
    }

    fun debug():String {
        return "Sektor: $subject, pen-saknr: $caseId, buc: $buc, sedid: ${sed.sed}, instirusjoner: $institution, aktorid: $aktoerID, norpin: $pin, dodaktorID: $etterlattAktoerID, dodpin: $etterlattPin haretterlatt: ${isValidEtterlatt()}"
    }

    @Throws(RuntimeException::class)
    fun hentAktoerIdPin(aktorid: String): String {
        if (aktorid.isBlank()) return ""
        return aktoerIdClient.hentPinIdentFraAktorid(aktorid)
    }

    //Pinid (FNR) aktorID
    fun getPinid(): String { return pin }

    fun getAktoerid(): String {
        return aktoerID
    }

    fun getSED(): SED {
        return sed
    }

    fun getSEDid(): String {
        return sed.sed!!
    }

    fun getBUC(): String {
        return buc
    }

    fun setPinID(pinid: String) {
        this.pin = pinid
    }

    fun getEtterlattPinid(): String { return etterlattPin ?: "" }
    fun setEtterlattPinID(etterlattPinid: String) {
        this.etterlattPin = etterlattPinid
    }
    fun getEtterlattAktoerID(): String { return etterlattAktoerID ?: "" }

    fun setEtterlattAktoerID(etterlattAktoerid: String) {
        this.etterlattAktoerID = etterlattAktoerid
    }

    fun getSaksnr(): String {
        return caseId
    }

    fun getPartSEDasJson(key: String): String {
        return partSedasJson[key].orEmpty()
    }

    fun getInstitutionsList(): List<InstitusjonItem> {
        return institution
    }

    fun isValidEtterlatt(): Boolean {
        return try {
            etterlattAktoerID?.isNotBlank() ?: false && etterlattPin?.isNotBlank() ?: false
        } catch (ex: Exception) {
            false
        }
    }

    fun validSED(sedid: String): Boolean {
        return getSEDid() == sedid
    }

    fun print(post: String): String {
        val sedid = getSEDid()
        return "$sedid\t\t$post"
    }

}
