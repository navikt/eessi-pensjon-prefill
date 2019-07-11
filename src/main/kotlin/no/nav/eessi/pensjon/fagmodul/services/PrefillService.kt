package no.nav.eessi.pensjon.fagmodul.services

import no.nav.eessi.pensjon.fagmodul.models.*
import no.nav.eessi.pensjon.fagmodul.prefill.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.PrefillSED
import no.nav.eessi.pensjon.fagmodul.services.eux.EuxGenericServerException
import no.nav.eessi.pensjon.fagmodul.services.eux.EuxService
import no.nav.eessi.pensjon.fagmodul.services.eux.SedDokumentIkkeOpprettetException
import no.nav.eessi.pensjon.fagmodul.services.eux.bucmodel.ShortDocumentItem
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PrefillService(private val euxService: EuxService, private val prefillSED: PrefillSED) {

    private val logger = LoggerFactory.getLogger(PrefillService::class.java)

    private val validator = SedValidator()

    //preutfylling av sed fra TPS, PESYS, AAREG o.l skjer her..
    @Throws(SedValidatorException::class)
    fun prefillSed(dataModel: PrefillDataModel): PrefillDataModel {

        val startTime = System.currentTimeMillis()
        val data = prefillSED.prefill(dataModel)
        val endTime = System.currentTimeMillis()
        logger.debug("PrefillSED tok ${endTime - startTime} ms.")

        if (SEDType.P2000.name == data.getSEDid()) {
            validator.validateP2000(data.sed)
        }

        return data
    }


    /**
    service function to prefill sed and call eux to put sed on existing type
     will also add any new institutions on existing case eg add or send a X005 sed.
     */
    @Throws(EuxGenericServerException::class, SedDokumentIkkeOpprettetException::class)
    fun prefillAndAddInstitusionAndSedOnExistingCase(dataModel: PrefillDataModel): ShortDocumentItem {
        logger.debug("Prøver å legge til Deltaker/Institusions på buc samt prefillSed og sende inn til Rina ")

        val bucUtil = euxService.getBucUtils(dataModel.euxCaseID)

        val nyeDeltakere = bucUtil.findNewParticipants(dataModel.getInstitutionsList())

        if (nyeDeltakere.isNotEmpty()) {
            logger.debug("DeltakerListe (InstitusjonItem) size: ${nyeDeltakere.size}")
            val bucX005 = bucUtil.findFirstDocumentItemByType("X005")
                if (bucX005 == null) {
                    logger.debug("X005 finnes ikke på buc, legger til Deltakere/Institusjon på vanlig måte")
                euxService.addDeltagerInstitutions(dataModel.euxCaseID, nyeDeltakere)
            } else {
                logger.debug("X005 finnes på buc, Sed X005 prefills og sendes inn")
                addX005(dataModel, nyeDeltakere)
            }
        }

        val data = prefillSed(dataModel)

        logger.debug("Prøver å sende SED:${dataModel.getSEDid()} inn på buc: ${dataModel.euxCaseID}")
        val docresult = euxService.opprettSedOnBuc(data.sed, data.euxCaseID)
        return euxService.getBucUtils(docresult.caseId).findDocument(docresult.documentId)
    }

    //Oppretter NavSedX005 og sender til Rina
    fun addX005(data: PrefillDataModel, nyeDeltakere: List<InstitusjonItem>): Boolean {

        val resultX005 = mutableListOf<String>()

        val datax005 = PrefillDataModel().apply {
            sed = SED.create(SEDType.X005.name)
            penSaksnummer = data.penSaksnummer
            personNr = data.personNr
            euxCaseID = data.euxCaseID
        }
        logger.debug("Prefill X005")
        val x005 = prefillSED.prefill(datax005)
        val sedX005 = x005.sed

        //opprette en X005 pr unik institusjon i filtrerteInstitusjon
        nyeDeltakere.forEach {
            try {
                logger.debug("Legger til Institusjon på X005 ${it.institution}")
                //ID og Navn på X005 er påkrevd må hente innn navn fra UI.
                val institusjonX005 = InstitusjonX005(
                        id = it.checkAndConvertInstituion(),
                        navn = it.name ?: it.checkAndConvertInstituion()
                )
                sedX005.nav?.sak?.leggtilinstitusjon?.institusjon = institusjonX005
            } catch (sx: SedDokumentIkkeOpprettetException) {
                logger.warn("Feiler ved innsending av X005 til Rina.")
                throw sx
            } catch (ex: Exception) {
                logger.warn("Feiler ved å legge til en X005 til rina på caseid: ${datax005.euxCaseID}")
                //??? bør vi kaste en exc?
            }

            try {
                val docresult = euxService.opprettSedOnBuc(sedX005, datax005.euxCaseID)
                //sjekk på vellykket X005
                if (docresult.caseId == data.euxCaseID) {
                    logger.debug("X005 Sed response fra Rina: $docresult")
                    resultX005.add(docresult.documentId)
                }
            } catch (sx: SedDokumentIkkeOpprettetException) {
                logger.warn("Feiler ved innsending av X005 til Rina.")
                throw sx
            } catch (ex: Exception) {
                logger.warn("Feiler ved å legge til en X005 til rina på caseid: ${datax005.euxCaseID}")
                //??? bør vi kaste en exc?
            }
        }
        return nyeDeltakere.size == resultX005.size
    }
}