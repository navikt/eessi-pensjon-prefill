package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.utils.simpleFormat
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class PrefillNav(private val preutfyllingPersonFraTPS: PrefillPersonDataFromTPS) : Prefill<Nav> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillNav::class.java) }
    private val barnSEDlist = listOf<String>("P2000", "P2100", "P2200")

    //TODO hva vil avsender ID på RINA være for NAV-PEN?
    //vil dette hentes fra Fasit? eller Rina?
    private val institutionid = "NO:noinst002"
    private val institutionnavn = "NOINST002, NO INST002, NO"

    override fun prefill(prefillData: PrefillDataModel): Nav {
        logger.debug("perfill aktoerId: ${prefillData.aktoerID}")

        val nav = Nav(
                eessisak = createLokaltsaksnummer(prefillData),

                //skal denne kjøres hver gang? eller kun under P2000? P2100
                barn = createBarnlistefraTPS(prefillData),

                //createBrukerfraTPS død hvis etterlatt (etterlatt aktoerregister fylt ut)
                bruker = createBrukerfraTPS(prefillData),

                //benyttes i P2x000
                krav = createKrav()
        )

        logger.debug("[${prefillData.getSEDid()}] Utfylling av NAV data med lokalsaksnr: ${prefillData.penSaksnummer}")

        return nav
    }

    private fun createKrav(): Krav {
        logger.debug("9.1      Dato for krav")
        return Krav(Date().simpleFormat())
    }

    // kan denne utfylling benyttes på alle SED?
    // etterlatt pensjon da er dette den avdøde.(ikke levende)
    // etterlatt pensjon da er den levende i pk.3 sed (gjenlevende) (pensjon.gjenlevende)
    private fun createBrukerfraTPS(utfyllingData: PrefillDataModel): Bruker {
        if (utfyllingData.erGyldigEtterlatt()) {
            logger.debug("2.1           Avdod person")
            return preutfyllingPersonFraTPS.prefillBruker(utfyllingData.avdod)
        }

        logger.debug("2.1       Forsikret person")
        return preutfyllingPersonFraTPS.prefillBruker(utfyllingData.personNr)
    }

    private fun createBarnlistefraTPS(utfyllingData: PrefillDataModel): List<BarnItem> {
        if (barnSEDlist.contains(utfyllingData.getSEDid()).not()) {
            logger.debug("8.1           SKIP Preutfylling barn, ikke P2x00")
            return listOf()
        }
        logger.debug("8.1           Preutfylling barn")
        val barnaspin = preutfyllingPersonFraTPS.hentBarnaPinIdFraBruker(utfyllingData.personNr)
        val barna = mutableListOf<BarnItem>()
        barnaspin.forEach {
            val barnBruker = preutfyllingPersonFraTPS.prefillBruker(it)
            logger.debug("          Preutfylling barn")
            val barn = BarnItem(
                    person = barnBruker.person,
                    far = barnBruker.far,
                    mor = barnBruker.mor,
                    relasjontilbruker = "BARN"
            )
            barna.add(barn)
        }
        return barna.toList()
    }

    /**
     * TODO NAV lokal institusjon må hentes fra Rina? hva skal vi ta med?
     */
    private fun createLokaltsaksnummer(prefillData: PrefillDataModel): List<EessisakItem> {
        logger.debug("1.1           Lokalt saksnummer (hvor hentes disse verider ifra?")
        //oppretter sakid
        return listOf(EessisakItem(
                institusjonsid = institutionid,
                institusjonsnavn = institutionnavn,
                saksnummer = prefillData.penSaksnummer,
                land = "NO"
        ))
    }


}

