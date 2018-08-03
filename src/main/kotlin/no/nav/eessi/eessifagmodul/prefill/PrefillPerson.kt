package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.clients.aktoerid.AktoerIdClient
import no.nav.eessi.eessifagmodul.models.PersonIkkeFunnetException
import no.nav.eessi.eessifagmodul.models.SED
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentIdentForAktoerIdPersonIkkeFunnet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PrefillPerson(private val aktoerIdClient: AktoerIdClient, private val prefillNav: PrefillNav, private val prefilliPensjon: PrefillPensjon) {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillPerson::class.java) }


    @Throws(RuntimeException::class)
    private fun hentPinIdentFraAktorid(pin: String = ""): String {
        logger.debug("henter pinid fra aktoerid $pin")
        return try {
            aktoerIdClient.hentIdentForAktoerId(aktoerId = pin).ident
        } catch(ikkefunnet: HentIdentForAktoerIdPersonIkkeFunnet) {
            throw PersonIkkeFunnetException("Fant ikke aktoer", ikkefunnet)
        } catch (ex: Exception) {
            logger.error(ex.message)
            throw Exception(ex.message, ex)
        }
    }

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

    //hva skal returnere sed? eller utfyllingData?
    //
    //Validering hva gjør vi med manglende data inn?
    fun preutfyll(utfyllingData: PrefillDataModel): SED {

        logger.debug("----------------- Preutfylling START ----------------- ")

        logger.debug("Preutfylling NAV     : ${prefillNav::class.java} ")
        logger.debug("Preutfylling Pensjon : ${prefilliPensjon::class.java} ")

        val sed = utfyllingData.hentSED()
        logger.debug("Preutfylling Utfylling Data")

        //har vi hentet ned fnr fra aktor?
        utfyllingData.putPinID( hentPinIdentFraAktorid(utfyllingData.hentAktoerid() ))

        logger.debug("Sjekker PinID : ${utfyllingData.hentPinid()}")
        logger.debug("Preutfylling hentet pinid fra aktoerIdClient.")

        sed.nav = prefillNav.utfyllNav(utfyllingData)

        logger.debug("Preutfylling Utfylling NAV")

        sed.pensjon = prefilliPensjon.pensjon(utfyllingData)
        logger.debug("Preutfylling Utfylling Pensjon")

        logger.debug("----------------- Preutfylling END ----------------- ")

        return utfyllingData.hentSED()
    }

}

