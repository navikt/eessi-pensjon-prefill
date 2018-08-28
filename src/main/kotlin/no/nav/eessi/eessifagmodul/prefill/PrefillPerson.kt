package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.SED
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PrefillPerson(private val prefillNav: PrefillNav, private val prefilliPensjon: PrefillPensjon) : Prefill<SED> {

    //private val aktoerIdClient: AktoerIdClient,
    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillPerson::class.java) }

    /*
    hva trenger vi å hente ut fra TPS
	person {
		pinid ->
		sivilstatus ->
		personstatus -> leve/død?

		//P2000
		barn -> [

			pinid ->
			sivilstatus ->

			//alle sed?
			foreldre -> [
				fara -> pinid
					personstatus ->
						doedsdato ->
				mora -> pinid
					personstatus ->
						doedsdato ->
			]
		]

	}
	pensjon {
	    //hvis data finnes i pensjon.gjenlevende -> da er nav.person død?
        //hvis data ikke finnes? da er det nav.perosn som er levende?
		gjenlevende
			person ->
	}
	*/

    override fun prefill(prefillData: PrefillDataModel): SED {

        logger.debug("----------------------------------------------------------")

        logger.debug("Preutfylling NAV     : ${prefillNav::class.java} ")
        logger.debug("Preutfylling Pensjon : ${prefilliPensjon::class.java} ")

        logger.debug("------------------| Preutfylling START |------------------ ")

        logger.debug(prefillData.print("Preutfylling Utfylling Data"))

        val sed = prefillData.getSED()

        sed.nav = prefillNav.utfyllNav(prefillData)

        logger.debug(prefillData.print("Preutfylling Utfylling NAV"))

        sed.pensjon = prefilliPensjon.pensjon(prefillData)

        logger.debug(prefillData.print("Preutfylling Utfylling Pensjon"))

        logger.debug("-------------------| Preutfylling END |------------------- ")
        return prefillData.getSED()

    }


}

