package no.nav.eessi.eessifagmodul.prefill

import no.nav.eessi.eessifagmodul.models.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.text.SimpleDateFormat
import java.util.*

@Component
class PrefillNav(private val preutfyllingPersonFraTPS: PrefillPersonDataFromTPS) : Prefill<Nav> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillNav::class.java) }

    //TODO hva vil avsender ID på RINA være for NAV-PEN?
    //vil dette hentes fra Fasit? eller Rina?
    private val institutionid = "NO:noinst002"
    private val institutionnavn = "NOINST002, NO INST002, NO"

    override fun prefill(prefillData: PrefillDataModel): Nav {
        logger.debug("perfill aktoerId: ${prefillData.aktoerID}")

        val nav = Nav(
                eessisak = opprettLokalSaknr(prefillData),

                //skal denne kjøres hver gang? eller kun under P2000? P2100
                barn = hentBarnaFraTPS(prefillData),

                //bruker død hvis etterlatt (etterlatt aktoerregister fylt ut)
                bruker = bruker(prefillData),

                //benyttes i P2x000
                krav = Krav(SimpleDateFormat("yyyy-MM-dd").format(Date()))
        )

        logger.debug("[${prefillData.getSEDid()}] Utfylling av NAV data med lokalsaksnr: ${prefillData.penSaksnummer}")

        return nav
    }


    // kan denne utfylling benyttes på alle SED?
    // etterlatt pensjon da er dette den avdøde.(ikke levende)
    // etterlatt pensjon da er den levende i pk.3 sed (gjenlevende) (pensjon.gjenlevende)
    private fun bruker(utfyllingData: PrefillDataModel): Bruker {
        logger.debug("2.1       Forsikret person")
        if (utfyllingData.erGyldigEtterlatt()) {
            logger.debug("2.1           Avdod person")
            val pinid = utfyllingData.avdod
            val bruker = preutfyllingPersonFraTPS.prefillBruker(pinid)
            logger.debug("Preutfylling Utfylling (avdød) Nav END")
            return bruker
        }

        val bruker = preutfyllingPersonFraTPS.prefillBruker(utfyllingData.personNr)

        //logger.debug("Preutfylling Utfylling Nav END")
        return bruker
    }

    private fun hentBarnaFraTPS(utfyllingData: PrefillDataModel): List<BarnItem> {
        if (utfyllingData.getSEDid() != "P2100") {
            logger.debug("Preutfylling barn SKIP not valid SED?")
            return listOf()
        }
        logger.debug("8.1           Preutfylling barn")
        val barnaspin = preutfyllingPersonFraTPS.hentBarnaPinIdFraBruker(utfyllingData.personNr)
        val barna = mutableListOf<BarnItem>()
        barnaspin.forEach {
            val barnBruker = preutfyllingPersonFraTPS.prefillBruker(it)
            logger.debug("Preutfylling barn x..")
            val barn = BarnItem(
                    person = barnBruker.person,
                    far = barnBruker.far,
                    mor = barnBruker.mor,
                    relasjontilbruker = "BARN"
            )
            barna.add(barn)
        }
        logger.debug("Preutfylling barn END")
        return barna.toList()
    }

    /**
     * TODO NAV lokal institusjon må hentes fra Fasit? Rina?
     */
    //korrekt bruk av eessisak? skal pen-saknr legges ved?
    //eller peker denne til en ekisterende rina-casenr?
    private fun opprettLokalSaknr(prefillData: PrefillDataModel): List<EessisakItem> {
        logger.debug("1.1           Lokalt saksnummer (hvor hentes disse verider ifra?")

        //lokalsak
        val lokalsaksnr = EessisakItem(
                institusjonsid = institutionid,
                institusjonsnavn = institutionnavn,
                saksnummer = prefillData.penSaksnummer,
                land = "NO"
        )
        //lokalvedtak
        val lokalvedtaksnr = EessisakItem(
                institusjonsid = institutionid,
                institusjonsnavn = institutionnavn,
                saksnummer = prefillData.vedtakId,
                land = "NO"
        )
        val eessisak = mutableListOf<EessisakItem>()

        eessisak.add(lokalsaksnr)
        try {
            if (prefillData.vedtakId.isNotBlank()) {
                eessisak.add(lokalvedtaksnr)
            }
        } catch (ex: Exception) {
            logger.debug("      Ingen vedtak")
        }

        return eessisak
    }


}

