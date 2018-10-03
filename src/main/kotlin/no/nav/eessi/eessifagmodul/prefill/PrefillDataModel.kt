package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.InstitusjonItem
import no.nav.eessi.eessifagmodul.models.SED
import no.nav.eessi.eessifagmodul.models.createSED
import java.util.*

/**
 * Data class to store different required data to build any given sed, auto or semiauto.
 *
 * sed, aktoerregister,  psak-saknr, rinanr, institutions (mottaker eg. nav),
 *
 * servives:  aktoerregister, tps, pen, maybe joark, eux-basis.
 *
 */
class PrefillDataModel {

    //pensjon
    lateinit var penSaksnummer: String
    lateinit var vedtakId: String
    lateinit var karavId: String

    lateinit var virkningstidspunkt: Calendar

    //aktoearid og pinid for person
    lateinit var personNr: String
    lateinit var aktoerID: String

    //aktoerid og pinid for avdod
    lateinit var avdodAktoerID: String
    lateinit var avdodPersonnr: String

    //rina
    lateinit var rinaSubject: String
    lateinit var euxCaseID: String
    lateinit var  buc: String
    lateinit var sed: SED
    lateinit var institution: List<InstitusjonItem>

    //div payload seddata json
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
        return this
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
        //TODO finne bedre metode?
        return try {
            val state = checkNotNull(avdodPersonnr)
            state.isNotBlank()
        } catch (ex: Exception) {
            false
        }
    }

    fun validSED(sedid: String): Boolean {
        return getSEDid() == sedid
    }

}
