package no.nav.eessi.pensjon.prefill.sed.krav

import no.nav.eessi.pensjon.prefill.models.pensjon.EessiKravArsak
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiKravGjelder
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiSakStatus
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto.KravHistorikk
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object KravHistorikkHelper {
    private val logger: Logger by lazy { LoggerFactory.getLogger(KravHistorikkHelper::class.java) }

    private fun sortertKravHistorikk(kravHistorikkListe:List<KravHistorikk>?): List<KravHistorikk>? {
        return kravHistorikkListe?.sortedBy { it.mottattDato}
    }

    fun hentKravHistorikkForsteGangsBehandlingUtlandEllerForsteGang(kravHistorikkListe: List<KravHistorikk>?): KravHistorikk =
        hentKravHistorikkMedKravType(listOf(EessiKravGjelder.F_BH_MED_UTL, EessiKravGjelder.F_BH_KUN_UTL, EessiKravGjelder.REVURD, EessiKravGjelder.F_BH_BO_UTL, EessiKravGjelder.SLUTT_BH_UTL), kravHistorikkListe)

    fun finnKravHistorikk(kravType: EessiKravGjelder, kravHistorikkListe: List<KravHistorikk>?): List<KravHistorikk>? {
        return sortertKravHistorikk(kravHistorikkListe)?.filter { it.kravType == kravType.name }
    }

    private fun hentKravHistorikkMedKravType(kravType: List<EessiKravGjelder>, kravHistorikkListe: List<KravHistorikk>?): KravHistorikk {
        val sortList = sortertKravHistorikk(kravHistorikkListe)
        val sortListFraKravIndex = sortList?.sortedBy { kravType.indexOfFirst { type -> type.name == it.kravType } }

        if (sortListFraKravIndex != null && sortListFraKravIndex.size > 1) {
            logger.warn("Listen med krav er større enn én. Krav: {${sortList.size}")
        }

        sortListFraKravIndex?.forEach { kravHistorikk ->
            if (kravHistorikk.kravType in kravType.map { it.name } ) {
                logger.info("Fant ${kravHistorikk.kravType} med virkningstidspunkt: ${kravHistorikk.virkningstidspunkt}")
                return kravHistorikk
            }
        }
        logger.warn("Fant ikke noe Kravhistorikk. med $kravType. Grunnet utsending kun utland mangler vilkårprøving/vedtak. følger ikke normal behandling")
        return KravHistorikk()
    }

    fun hentKravhistorikkForGjenlevende(kravHistorikkListe: List<KravHistorikk>?): KravHistorikk? {
            val kravHistorikk = kravHistorikkListe?.filter { krav -> krav.kravArsak == EessiKravArsak.GJNL_SKAL_VURD.name || krav.kravArsak == EessiKravArsak.TILST_DOD.name }
            if (kravHistorikk?.isNotEmpty() == true) {
                return kravHistorikk.first()
            }
            logger.warn("Fant ikke Kravhistorikk med bruk av kravårsak: ${EessiKravArsak.GJNL_SKAL_VURD.name} eller ${EessiKravArsak.TILST_DOD.name} ")
            return null
    }

    fun hentKravhistorikkForGjenlevendeOgNySoknad(kravHistorikkListe: List<KravHistorikk>?): KravHistorikk? {
        val kravHistorikk = kravHistorikkListe?.filter { krav -> krav.kravArsak == EessiKravArsak.GJNL_SKAL_VURD.name || krav.kravArsak == EessiKravArsak.TILST_DOD.name || krav.kravArsak == EessiKravArsak.NY_SOKNAD.name }
        if (kravHistorikk?.isNotEmpty() == true) {
            return kravHistorikk.first()
        }
        logger.warn("Fant ikke Kravhistorikk med bruk av kravårsak: ${EessiKravArsak.GJNL_SKAL_VURD.name} , ${EessiKravArsak.TILST_DOD.name} eller ${EessiKravArsak.NY_SOKNAD.name} fra kravliste: \n${kravHistorikkListe.toString()}")
        return null
    }

    fun hentKravHistorikkMedKravStatusTilBehandling(kravHistorikkListe: List<KravHistorikk>?): KravHistorikk {
        val sortList = sortertKravHistorikk(kravHistorikkListe)
        sortList?.forEach {
            logger.debug("leter etter Krav status med ${EessiSakStatus.TIL_BEHANDLING}, fant ${it.kravType} med virkningstidspunkt dato : ${it.virkningstidspunkt}")
            if (EessiSakStatus.TIL_BEHANDLING.name == it.kravStatus?.name) {
                logger.debug("Fant Kravhistorikk med ${it.kravStatus.name}")
                return it
            }
        }
        logger.error("Fant ikke noe Kravhistorikk..${EessiSakStatus.TIL_BEHANDLING}. Mangler vilkårsprlving/vedtak. følger ikke normal behandling")
        return KravHistorikk()
    }

    fun hentKravHistorikkMedKravStatusAvslag(kravHistorikkListe: List<KravHistorikk>?): KravHistorikk {
        val sortList = sortertKravHistorikk(kravHistorikkListe)
        sortList?.forEach {
            logger.debug("leter etter Krav status med ${EessiSakStatus.AVSL}, fant ${it.kravType} med virkningstidspunkt dato : ${it.virkningstidspunkt}")
            if (EessiSakStatus.AVSL.name == it.kravStatus?.name) {
                logger.debug("Fant Kravhistorikk med ${it.kravStatus.name}")
                return it
            }
        }
        logger.error("Fant ikke noe Kravhistorikk..${EessiSakStatus.AVSL}. Mangler vilkårsprøving. følger ikke normal behandling")
        return KravHistorikk()
    }

    fun hentKravHistorikkMedValgtKravType(kravHistorikkListe: List<KravHistorikk>?, penKravtype: EessiKravGjelder): KravHistorikk? {
        val sortList = sortertKravHistorikk(kravHistorikkListe)
        if (sortList == null || sortList.size > 1) return null
        logger.debug("leter etter kravtype: $penKravtype")
        return sortList.firstOrNull { kravhist -> kravhist.kravType == penKravtype.name}
            .also { logger.debug("fant ${it?.kravType} med kravÅrsak: ${it?.kravArsak} med virkningstidspunkt dato : ${it?.virkningstidspunkt}") }

    }

    fun finnKravHistorikkForDato(pensak: P2xxxMeldingOmPensjonDto.Sak?): KravHistorikk {
        try {
            val gjenLevKravarsak = hentKravhistorikkForGjenlevende(pensak?.kravHistorikkListe)
            if (gjenLevKravarsak != null) return gjenLevKravarsak

            val kravKunUtland = hentKravHistorikkMedValgtKravType(pensak?.kravHistorikkListe, F_BH_KUN_UTL)
            if (kravKunUtland != null) return  kravKunUtland

            logger.info("Sakstatus: ${pensak?.status},sakstype: ${pensak?.sakType}")
            return when (Sakstatus.byValue(pensak?.status!!)) {
                Sakstatus.TIL_BEHANDLING -> hentKravHistorikkMedKravStatusTilBehandling(pensak.kravHistorikkListe)
                Sakstatus.AVSL -> hentKravHistorikkMedKravStatusAvslag(pensak.kravHistorikkListe)
                else -> hentKravHistorikkForsteGangsBehandlingUtlandEllerForsteGang(pensak.kravHistorikkListe)
            }

        } catch (ex: Exception) {
            logger.warn("Fant ingen gyldig kravdato: $ex")
            return KravHistorikk()
        }
    }

}
