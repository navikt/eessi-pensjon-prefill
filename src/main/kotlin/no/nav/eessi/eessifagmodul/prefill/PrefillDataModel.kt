package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.InstitusjonItem
import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.models.createSED

/**
 * Data class to store different required data to build any given sed, auto or semiauto.
 *
 * sed, aktoerid,  psak-saknr, rinanr, institutions (mottaker eg. nav),
 *
 * servives:  aktoerid, tps, pen, maybe joark, eux-basis.
 *
 */
class PrefillDataModel {

    lateinit var penSaksnummer: String
    lateinit var vedtakId: String
    lateinit var karavId: String

    //aktoearid og pinid for person
    lateinit var personNr: String
    lateinit var aktoerID: String

    //aktoerid og pinid for avdod
    lateinit var avdodAktoerID: String
    lateinit var avdodPersonnr: String


    lateinit var rinaSubject: String
    lateinit var euxCaseID: String
    lateinit var  buc: String
    lateinit var sed: SED
    lateinit var institution: List<InstitusjonItem>

    val partSedasJson: MutableMap<String, String> = mutableMapOf()

    fun build(subject: String, caseId: String, buc: String, sedID: String, aktoerID: String, pinID: String, institutions: List<InstitusjonItem>, payload: String, euxcaseId: String): PrefillDataModel {
        this.rinaSubject =  subject
        this.penSaksnummer = caseId
        this.buc = buc
        this.aktoerID = aktoerID
        this.sed = createSED(sedID)
        this.institution = institutions
        this.partSedasJson.put(sedID, payload)
        this.euxCaseID = euxcaseId
        this.personNr = pinID
        println(debug())
        return this
    }

    fun build(subject: String, caseId: String, buc: String, sedID: String, aktoerID: String,pinID: String, institutions: List<InstitusjonItem>): PrefillDataModel {
        this.rinaSubject =  subject
        this.penSaksnummer = caseId
        this.buc = buc
        this.aktoerID = aktoerID
        this.sed = createSED(sedID)
        this.institution = institutions
        this.personNr = pinID
        println(debug())
        return this
    }

    fun debug():String {
        return "Sektor: $rinaSubject, pen-saknr: $penSaksnummer, buc: $buc, sedid: ${sed.sed}, instirusjoner: $institution, aktorid: $aktoerID, norpin: $personNr, haretterlatt: ${isValidEtterlatt()} payload: ${partSedasJson.size}  payload: $partSedasJson "
    }


    fun getSEDid(): String {
        return sed.sed!!
    }
    fun getPartSEDasJson(key: String): String {
        return partSedasJson[key].orEmpty()
    }

    fun getInstitutionsList(): List<InstitusjonItem> {
        return institution
    }

    fun isValidEtterlatt(): Boolean {
        return try {
            avdodAktoerID.isNotBlank() && avdodPersonnr.isNotBlank()
        } catch (ex: Exception) {
            print(ex.message)
            false
        }
    }

    fun validSED(sedid: String): Boolean {
        return getSEDid() == sedid
    }

}
