package no.nav.eessi.pensjon.fagmodul.prefill

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.eessi.pensjon.fagmodul.eux.BucUtils
import no.nav.eessi.pensjon.fagmodul.eux.EuxConflictException
import no.nav.eessi.pensjon.fagmodul.eux.EuxRinaServerException
import no.nav.eessi.pensjon.fagmodul.eux.EuxService
import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.models.PersonDataCollection
import no.nav.eessi.pensjon.fagmodul.models.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.models.SEDType
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentType
import no.nav.eessi.pensjon.personoppslag.pdl.model.NorskIdent
import no.nav.eessi.pensjon.utils.toJson
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import javax.annotation.PostConstruct

@Service
class InnhentingService(private val personDataService: PersonDataService,
                        private val prefillService: PrefillService,
                        private val euxService: EuxService,
                        @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper(SimpleMeterRegistry())) {

    private val logger: Logger = LoggerFactory.getLogger(InnhentingService::class.java)

    private lateinit var HentPerson: MetricsHelper.Metric
    private lateinit var addInstutionAndDocumentBucUtils: MetricsHelper.Metric


    @PostConstruct
    fun initMetrics() {
        HentPerson = metricsHelper.init("HentPerson", ignoreHttpCodes = listOf(HttpStatus.BAD_REQUEST))
        addInstutionAndDocumentBucUtils = metricsHelper.init(
            "AddInstutionAndDocumentBucUtils",
            ignoreHttpCodes = listOf(HttpStatus.BAD_REQUEST)
        )
    }

    fun kanSedOpprettes(dataModel: PrefillDataModel): BucUtils {
        return addInstutionAndDocumentBucUtils.measure {
            logger.info("******* Hent BUC sjekk om sed kan opprettes *******")
            BucUtils(euxService.getBuc(dataModel.euxCaseID)).also { bucUtil ->
                //sjekk for om deltakere alt er fjernet med x007 eller x100 sed
                bucUtil.checkForParticipantsNoLongerActiveFromXSEDAsInstitusjonItem(dataModel.getInstitutionsList())
                //gyldig sed kan opprettes
                bucUtil.checkIfSedCanBeCreated(dataModel.sedType, dataModel.penSaksnummer)
            }
        }
    }

    fun checkAndAddInstitution(dataModel: PrefillDataModel, bucUtil: BucUtils, personcollection: PersonDataCollection) {
        logger.info(
            "Hvem er caseOwner: ${
                bucUtil.getCaseOwner()?.toJson()
            } på buc: ${bucUtil.getProcessDefinitionName()}"
        )
        val navCaseOwner = bucUtil.getCaseOwner()?.country == "NO"

        val nyeInstitusjoner = bucUtil.findNewParticipants(dataModel.getInstitutionsList())

        if (nyeInstitusjoner.isNotEmpty()) {
            if (bucUtil.findFirstDocumentItemByType(SEDType.X005) == null) {
                euxService.addInstitution(dataModel.euxCaseID, nyeInstitusjoner.map { it.institution })
            } else {

                //--gjort noe. ..
                nyeInstitusjoner.forEach {
                    if (!navCaseOwner && it.country != "NO") {
                        logger.error("NAV er ikke sakseier. Du kan ikke legge til deltakere utenfor Norge")
                        throw ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "NAV er ikke sakseier. Du kan ikke legge til deltakere utenfor Norge"
                        )
                    }
                }
                addInstitutionMedX005(
                    dataModel,
                    nyeInstitusjoner,
                    bucUtil.getProcessDefinitionVersion(),
                    personcollection
                )
            }
        }
    }

    private fun addInstitutionMedX005(
        dataModel: PrefillDataModel,
        nyeInstitusjoner: List<InstitusjonItem>,
        bucVersion: String,
        personcollection: PersonDataCollection
    ) {
        logger.info("X005 finnes på buc, Sed X005 prefills og sendes inn: ${nyeInstitusjoner.toJsonSkipEmpty()}")

        var execptionError: Exception? = null
        val x005Liste = prefillService.prefillEnX005ForHverInstitusjon(nyeInstitusjoner, dataModel, personcollection)

        x005Liste.forEach { x005 ->
            try {
                updateSEDVersion(x005, bucVersion)
                euxService.opprettJsonSedOnBuc(x005.toJson(), x005.type, dataModel.euxCaseID, dataModel.vedtakId)
            } catch (eux: EuxRinaServerException) {
                execptionError = eux
            } catch (exx: EuxConflictException) {
                execptionError = exx
            } catch (ex: Exception) {
                execptionError = ex
            }
        }
        if (execptionError != null) {
            logger.error(
                "Feiler ved oppretting av X005  (ny institusjon), euxCaseid: ${dataModel.euxCaseID}, sed: ${dataModel.sedType}",
                execptionError
            )
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Feiler ved oppretting av X005 (ny institusjon) for euxCaseId: ${dataModel.euxCaseID}"
            )
        }

    }

    //flyttes til prefill / en eller annen service?
    fun updateSEDVersion(sed: SED, bucVersion: String) {
        when (bucVersion) {
            "v4.2" -> {
                sed.sedVer = "2"
            }
            else -> {
                sed.sedVer = "1"
            }
        }
    }

    fun hentPersonData(prefillData: PrefillDataModel) : PersonDataCollection = personDataService.hentPersonData(prefillData)

    fun hentFnrfraAktoerService(aktoerid: String?): String = personDataService.hentFnrfraAktoerService(aktoerid)

    fun hentIdent(aktoerId: IdentType.AktoerId, norskIdent: NorskIdent): String = personDataService.hentIdent(aktoerId, norskIdent).id

}