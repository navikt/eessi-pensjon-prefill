package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.Barn
import no.nav.eessi.eessifagmodul.models.InstitusjonItem
import no.nav.eessi.eessifagmodul.models.SED

/**
 * Data class to store different required data to build any given sed, auto or semiauto.
 *
 * sed, aktoerregister,  psak-saknr, rinanr, institutions (mottaker eg. nav),
 *
 * services:  aktoerregister, person, pen, maybe joark, eux-basis.
 *
 */
class PrefillDataModel {

    //pensjon
    lateinit var penSaksnummer: String
    lateinit var vedtakId: String
    lateinit var kravId: String

    //aktoearid og aktoerId for person
    lateinit var personNr: String
    lateinit var aktoerID: String

    //data fra pesys
    lateinit var saktype: String
    lateinit var barnlist: List<Barn>
    //aktoerid og aktoerId for avdod

    lateinit var avdod: String
    lateinit var avdodFar: String
    lateinit var avdodMor: String

    //rina
    lateinit var rinaSubject: String
    lateinit var euxCaseID: String
    lateinit var buc: String
    lateinit var sed: SED
    lateinit var institution: List<InstitusjonItem>

    //div payload seddata json
    val partSedAsJson: MutableMap<String, String> = mutableMapOf()

    fun getSEDid(): String {
        return sed.sed!!
    }

    fun getPartSEDasJson(key: String): String {
        return partSedAsJson[key].orEmpty()
    }

    fun getInstitutionsList(): List<InstitusjonItem> {
        return institution
    }

    fun erGyldigEtterlatt(): Boolean {
        //TODO finne bedre metode?
        return try {
            val state = checkNotNull(avdod)
            return state.isNotBlank()
        } catch (ex: Exception) {
            false
        }
    }

    fun erForeldreLos(): Boolean {
        //TODO finne bedre metode?
        return try {
            val stateOne = checkNotNull(avdodFar)
            val stateTwo = checkNotNull(avdodMor)
            stateOne.isNotBlank() && stateTwo.isNotBlank()
        } catch (ex: Exception) {
            false
        }
    }


    fun validSED(sedid: String): Boolean {
        return getSEDid() == sedid
    }
}
