package no.nav.eessi.pensjon.fagmodul.eux

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.eessi.pensjon.fagmodul.eux.basismodel.BucSedResponse
import no.nav.eessi.pensjon.fagmodul.eux.basismodel.Rinasak
import no.nav.eessi.pensjon.fagmodul.eux.bucmodel.Buc
import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.models.SEDType
import no.nav.eessi.pensjon.fagmodul.sedmodel.Krav
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import no.nav.eessi.pensjon.utils.typeRefs
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.client.RestTemplate

@Service
class EuxService (private val euxKlient: EuxKlient) {

    fun getAvailableSedOnBuc(bucType: String?): List<String> {
        val map = initSedOnBuc()

        if (bucType.isNullOrEmpty()) {
            val set = mutableSetOf<String>()
            map["P_BUC_01"]?.let { set.addAll(it) }
            map["P_BUC_02"]?.let { set.addAll(it) }
            map["P_BUC_03"]?.let { set.addAll(it) }
            map["P_BUC_05"]?.let { set.addAll(it) }
            map["P_BUC_06"]?.let { set.addAll(it) }
            map["P_BUC_09"]?.let { set.addAll(it) }
            map["P_BUC_10"]?.let { set.addAll(it) }
            return set.toList()
        }
        return map[bucType].orEmpty()
    }

    /**
     * Own impl. no list from eux that contains list of SED to a speific BUC
     */
    fun initSedOnBuc(): Map<String, List<String>> {
        return mapOf(
                "P_BUC_01" to listOf("P2000"),
                "P_BUC_02" to listOf("P2100"),
                "P_BUC_03" to listOf("P2200"),
                "P_BUC_05" to listOf("P8000"),
                "P_BUC_06" to listOf("P5000", "P6000", "P7000", "P10000"),
                "P_BUC_09" to listOf("P14000"),
                "P_BUC_10" to listOf("P15000"),
                "P_BUC_04" to listOf("P1000"),
                "P_BUC_07" to listOf("P11000"),
                "P_BUC_08" to listOf("P12000")
        )
    }

    // Vi trenger denne no arg konstruktøren for å kunne bruke @Spy med mockito
    constructor() : this(EuxKlient(RestTemplate(), MetricsHelper(SimpleMeterRegistry())))

    private val logger = LoggerFactory.getLogger(EuxService::class.java)

    @Throws(EuxGenericServerException::class, SedDokumentIkkeOpprettetException::class)
    fun opprettSvarSedOnBuc(navSED: SED, euxCaseId: String, parentDocumentId: String): BucSedResponse {
        val euxUrlpath = "/buc/{RinaSakId}/sed/{DokuemntId}/svar"
        logger.debug("prøver å kontakte eux-rina-api : $euxUrlpath")
        return euxKlient.opprettSed(euxUrlpath,
                navSED.toJsonSkipEmpty(),
                euxCaseId,
                MetricsHelper.MeterName.OpprettSvarSED,
                "Feil ved opprettSvarSed", parentDocumentId)
    }

    /**
     * Ny SED på ekisterende type
     */
    @Throws(EuxGenericServerException::class, SedDokumentIkkeOpprettetException::class)
    fun opprettSedOnBuc(navSED: SED, euxCaseId: String): BucSedResponse {
        val euxUrlpath = "/buc/{RinaSakId}/sed"
        return euxKlient.opprettSed(euxUrlpath, navSED.toJsonSkipEmpty(), euxCaseId, MetricsHelper.MeterName.OpprettSED, "Feil ved opprettSed", null)
    }

    /**
     * Henter ut sed fra rina med bucid og documentid
     */
    fun getSedOnBuc(euxCaseId: String, sedType: String?): List<SED> {
        logger.info("Prøver å hente ut en BucUtils for type $euxCaseId")
        val docid = getBuc(euxCaseId).documents ?: throw NoSuchFieldException("Fant ikke DocumentsItem")

        val sedlist = mutableListOf<SED>()
        docid.forEach {
            if (sedType != null && sedType == it.type) {
                it.id?.let { id ->
                    sedlist.add(getSedOnBucByDocumentId(euxCaseId, id))
                }
            } else {
                it.id?.let { id ->
                    sedlist.add(getSedOnBucByDocumentId(euxCaseId, id))
                }
            }
        }
        logger.info("return liste av SED for type: $sedType listSize: ${sedlist.size}")
        return sedlist
    }

    fun getBuc(euxCaseId: String): Buc {
        val body = euxKlient.getBucJson(euxCaseId)
        logger.debug("mapper buc om til BUC objekt-model")
        return mapJsonToAny(body, typeRefs())
    }

    @Throws(EuxServerException::class, SedDokumentIkkeLestException::class)
    fun getSedOnBucByDocumentId(euxCaseId: String, documentId: String): SED {
        val json = euxKlient.getSedOnBucByDocumentIdAsJson(euxCaseId, documentId)
        return SED.fromJson(json)
    }

    /**
     * Henter ut Kravtype og Fnr fra P2100 og P15000
     */
    fun hentFnrOgYtelseKravtype(euxCaseId: String, documentId: String): PinOgKrav {
        val sed = getSedOnBucByDocumentId(euxCaseId, documentId)

        //validere om SED er virkelig en P2100 eller P15000
        if (SEDType.P2100.name == sed.sed) {
            return PinOgKrav(
                    fnr = euxKlient.getFnrMedLandkodeNO(sed.pensjon?.gjenlevende?.person?.pin),
                    krav = sed.nav?.krav ?: Krav()
            )
        }
        //P15000 sjekke om det er 02 Gjenlevende eller ikke
        if (SEDType.P15000.name == sed.sed) {
            val krav = sed.nav?.krav ?: Krav()
            return if ("02" == krav.type) {
                PinOgKrav(
                        fnr = euxKlient.getFnrMedLandkodeNO(sed.pensjon?.gjenlevende?.person?.pin),
                        krav = krav
                )
            } else {
                PinOgKrav(
                        fnr = euxKlient.getFnrMedLandkodeNO(sed.nav?.bruker?.person?.pin),
                        krav = sed.nav?.krav ?: Krav()
                )
            }
        }
        throw SedDokumentIkkeGyldigException("SED gyldig SED av type P2100 eller P15000")
    }

    fun getSingleBucAndSedView(euxCaseId: String): BucAndSedView {
        return try {
            BucAndSedView.from(getBuc(euxCaseId))
        } catch (ex: Exception) {
            logger.error("Feiler ved utlevering av enkel bucandsedview ${ex.message}", ex)
            BucAndSedView.fromErr(ex.message)
        }
    }

    fun getBucAndSedView(rinasaker: List<String>): List<BucAndSedView> {
        val startTime = System.currentTimeMillis()
        val list = rinasaker
                .map { rinaid ->
                    try {
                        BucAndSedView.from(getBuc(rinaid))
                    } catch (ex: Exception) {
                        logger.error(ex.message, ex)
                        BucAndSedView.fromErr(ex.message)
                    }
                }
                .toList()

        logger.debug(" ferdig returnerer list av BucAndSedView. Antall BUC: ${list.size}")

        logger.debug(" sortert listen på startDate nyeste dato først")
        val sortlist = list.asSequence().sortedByDescending { it.startDate }.toList()

        logger.debug(" tiden tok ${System.currentTimeMillis() - startTime} ms.")
        return sortlist
    }

    fun addInstitution(euxCaseID: String, nyeInstitusjoner: List<String>) {
        logger.debug("Prøver å legge til Deltaker/Institusions på buc samt prefillSed og sende inn til Rina ")
        logger.info("X005 finnes ikke på buc, legger til Deltakere/Institusjon på vanlig måte")
        euxKlient.putBucMottakere(euxCaseID, nyeInstitusjoner)
    }

    fun getInstitutions(bucType: String, landkode: String? = ""): List<InstitusjonItem> {
        return euxKlient.getInstitutions(bucType, landkode)
    }

    fun getFilteredArchivedaRinasaker(list: List<Rinasak>): List<String> {
        val gyldigBucs = mutableListOf("H_BUC_07", "R_BUC_01", "R_BUC_02", "M_BUC_02", "M_BUC_03a", "M_BUC_03b")
        gyldigBucs.addAll(initSedOnBuc().keys.map { it }.toList())

        return list.asSequence()
                .filterNot { rinasak -> rinasak.status == "archived" }
                .filter { rinasak -> gyldigBucs.contains(rinasak.processDefinitionId) }
                .sortedBy { rinasak -> rinasak.id }
                .map { rinasak -> rinasak.id!! }
                .toList()
    }


    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    class SedDokumentIkkeSendtException(message: String?) : Exception(message)

}