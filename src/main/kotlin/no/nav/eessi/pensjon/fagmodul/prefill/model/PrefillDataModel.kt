package no.nav.eessi.pensjon.fagmodul.prefill.model

import no.nav.eessi.pensjon.fagmodul.sedmodel.AndreinstitusjonerItem
import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.sedmodel.InstitusjonX005
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.typeRefs
import no.nav.pensjon.v1.brukersbarn.V1BrukersBarn
import no.nav.pensjon.v1.ektefellepartnersamboer.V1EktefellePartnerSamboer

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

    //aktoearid og aktoerId for person
    lateinit var personNr: String
    lateinit var aktoerID: String

    //data fra pesys
    lateinit var saktype: String
    lateinit var barnlist: List<V1BrukersBarn>
    lateinit var partnerFnr: List<V1EktefellePartnerSamboer>

    //avdod rellasjon - gjennlevende
    lateinit var avdod: String
    lateinit var avdodAktorID: String
    lateinit var avdodFar: String
    lateinit var avdodMor: String

    //rina
    lateinit var rinaSubject: String
    lateinit var euxCaseID: String
    lateinit var buc: String
    lateinit var sed: SED
    lateinit var institution: List<InstitusjonItem>

    lateinit var skipSedkey: List<String>

    //hjelpe parametere for utfylling av institusjon
    var andreInstitusjon: AndreinstitusjonerItem? = null
    var institusjonX005: InstitusjonX005? = null

    //div payload seddata json
    var partSedAsJson: MutableMap<String, String> = mutableMapOf()

    fun getSEDid(): String {
        return sed.sed!!
    }

    fun getPartSEDasJson(key: String): String? {
        return partSedAsJson[key]
    }

    fun getPersonInfoFromRequestData(): BrukerInformasjon? {
        val personInfo = getPartSEDasJson("PersonInfo") ?: return null
        return mapJsonToAny(personInfo, typeRefs())
    }

    fun getInstitutionsList(): List<InstitusjonItem> {
        return institution
    }

    fun erGyldigEtterlatt(): Boolean {
        //TODO finne bedre metode?
        return try {
            return avdod.isNotBlank()
        } catch (ex: Exception) {
            false
        }
    }

    fun brukerEllerGjenlevendeHvisDod() = if (erGyldigEtterlatt()) avdod else personNr

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

    fun kanFeltSkippes(key: String): Boolean {
        return try {
            skipSedkey.contains(key)
        } catch (ex: Exception) {
            false
        }
    }

    fun clone() : String {
        return mapAnyToJson(this)
    }

    companion object {
        @JvmStatic
        fun fromJson(prefillData: String) : PrefillDataModel {
            return mapJsonToAny(prefillData, typeRefs(), true)
        }
    }

}
