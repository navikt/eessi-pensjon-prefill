package no.nav.eessi.pensjon.fagmodul.prefill

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.eessi.pensjon.fagmodul.eux.BucUtils
import no.nav.eessi.pensjon.fagmodul.eux.EuxService
import no.nav.eessi.pensjon.fagmodul.models.PersonDataCollection
import no.nav.eessi.pensjon.fagmodul.models.PrefillDataModel
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentType
import no.nav.eessi.pensjon.personoppslag.pdl.model.NorskIdent
import no.nav.eessi.pensjon.vedlegg.VedleggService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class InnhentingService(private val personDataService: PersonDataService,
                        private val euxService: EuxService,
                        private val vedleggService: VedleggService,
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

    fun hentPersonData(prefillData: PrefillDataModel) : PersonDataCollection = personDataService.hentPersonData(prefillData)

    fun hentFnrfraAktoerService(aktoerid: String?): String = personDataService.hentFnrfraAktoerService(aktoerid)

    fun hentIdent(aktoerId: IdentType.AktoerId, norskIdent: NorskIdent): String = personDataService.hentIdent(aktoerId, norskIdent).id

    fun hentRinaSakIderFraMetaData(aktoerid: String): List<String> = vedleggService.hentRinaSakIderFraMetaData(aktoerid)

}