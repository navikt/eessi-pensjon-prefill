package no.nav.eessi.pensjon.fagmodul.eux

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.eessi.pensjon.eux.model.buc.Buc
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.fagmodul.eux.basismodel.BucSedResponse
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.services.statistikk.StatistikkHandler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import javax.annotation.PostConstruct

@Service
class EuxPrefillService (private val euxKlient: EuxKlient,
                         private val statistikk: StatistikkHandler,
                         @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper(SimpleMeterRegistry())) {

    private val logger = LoggerFactory.getLogger(EuxPrefillService::class.java)

    // Vi trenger denne no arg konstruktøren for å kunne bruke @Spy med mockito
    constructor() : this(EuxKlient(RestTemplate()),
        StatistikkHandler("Q2", KafkaTemplate(DefaultKafkaProducerFactory(emptyMap())), ""))

    private lateinit var opprettSvarSED: MetricsHelper.Metric
    private lateinit var opprettSED: MetricsHelper.Metric

    @PostConstruct
    fun initMetrics() {
        opprettSvarSED = metricsHelper.init("OpprettSvarSED")
        opprettSED = metricsHelper.init("OpprettSED")
    }

    @Throws(EuxGenericServerException::class, SedDokumentIkkeOpprettetException::class)
    fun opprettSvarJsonSedOnBuc(jsonSed: String, euxCaseId: String, parentDocumentId: String, vedtakId: String?): BucSedResponse {
        val bucSedResponse = euxKlient.opprettSvarSed(
            jsonSed,
            euxCaseId,
            parentDocumentId,
            "Feil ved opprettSvarSed", opprettSvarSED
        )

        statistikk.produserSedOpprettetHendelse(euxCaseId, bucSedResponse.documentId, vedtakId)
        return bucSedResponse
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

}


data class BucOgDocumentAvdod(
    val rinaidAvdod: String,
    val buc: Buc,
    var dokumentJson: String = ""
)