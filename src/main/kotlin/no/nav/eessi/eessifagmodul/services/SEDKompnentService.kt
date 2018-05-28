package no.nav.eessi.eessifagmodul.services

import com.google.common.collect.Lists
import no.nav.eessi.eessifagmodul.models.NavPerson
import no.nav.eessi.eessifagmodul.models.SED
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SEDKompnentService {

    private val logger: Logger = LoggerFactory.getLogger(SEDKompnentService::class.java)

    fun opprettSEDmedSak(fnr: String, saksnr: String) : SED {
        val navperson = NavPerson(fnr)
        val sed = SED("SEDtype", "SAKnr: $saksnr", navperson,null, null )
        println("opprettSED Controller : $sed")
        return sed
    }

    fun opprettSED(fnr: String) : SED {
        println("FNR = $fnr")
        val navperson = NavPerson("FNR=$fnr")
        val sed = SED("SEDtype", "SAKnr: 123456", navperson,null, null )
        println("opprettSED Controller : $sed")
        return sed
    }


    // Rute for hent gyldige SED-typer for en gitt BUC
    fun getSedsForBuc(bucId : String) : List<SED> {
        val navperson = NavPerson(null)
        return Lists.newArrayList(SED("SEDtype", "SAKBUC1 :$bucId", navperson,null, null ), SED("SEDtype", "SAKBUC2 :$bucId", navperson,null, null ), SED("SEDtype", "SAKBUC3 :$bucId", navperson,null, null ))
    }


}