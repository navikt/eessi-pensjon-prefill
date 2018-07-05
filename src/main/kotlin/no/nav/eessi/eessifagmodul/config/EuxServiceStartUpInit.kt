package no.nav.eessi.eessifagmodul.config

import no.nav.eessi.eessifagmodul.services.EuxService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class EuxServiceStartUpInit(val service: EuxService) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(EuxServiceStartUpInit::class.java) }

    @PostConstruct
    fun init() {
        logger.debug("Initilize Cache for Bucs and Institusjoner")
        //service.getCachedBuCTypePerSekor()
        try {
            service.refreshAll()
        } catch (ex: Exception) {
            logger.debug("Initilize Cache for Bucs and Institusjoner Exception")
        }
    }

}
