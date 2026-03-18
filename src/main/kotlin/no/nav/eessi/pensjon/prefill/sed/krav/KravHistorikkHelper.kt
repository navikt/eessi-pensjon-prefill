package no.nav.eessi.pensjon.prefill.sed.krav

import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakStatus
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
        hentKravHistorikkMedKravType(listOf(EessiFellesDto.EessiKravGjelder.F_BH_MED_UTL, EessiFellesDto.EessiKravGjelder.F_BH_KUN_UTL, EessiFellesDto.EessiKravGjelder.REVURD, EessiFellesDto.EessiKravGjelder.F_BH_BO_UTL, EessiFellesDto.EessiKravGjelder.SLUTT_BH_UTL), kravHistorikkListe)

    private fun hentKravHistorikkMedKravType(kravType: List<EessiFellesDto.EessiKravGjelder>, kravHistorikkListe: List<KravHistorikk>?): KravHistorikk {
        val sortList = sortertKravHistorikk(kravHistorikkListe)
        val sortListFraKravIndex = sortList?.sortedBy { kravType.indexOfFirst { type -> type == it.kravType } }

        if (sortListFraKravIndex != null && sortListFraKravIndex.size > 1) {
            logger.warn("Listen med krav er større enn én. Krav: {${sortList.size}")
        }

        sortListFraKravIndex?.forEach { kravHistorikk ->
            if (kravHistorikk.kravType in kravType) {
                logger.info("Fant ${kravHistorikk.kravType} med virkningstidspunkt: ${kravHistorikk.virkningstidspunkt}")
                return kravHistorikk
            }
        }
        logger.warn("Fant ikke noe Kravhistorikk. med $kravType. Grunnet utsending kun utland mangler vilkårprøving/vedtak. følger ikke normal behandling")
        return KravHistorikk()
    }

    fun hentKravhistorikkForGjenlevende(kravHistorikkListe: List<KravHistorikk>?): KravHistorikk? {
            val kravHistorikk = kravHistorikkListe?.filter { krav -> krav.kravAarsak == EessiFellesDto.EessiKravAarsak.GJNL_SKAL_VURD || krav.kravAarsak == EessiFellesDto.EessiKravAarsak.TILST_DOD }
            if (kravHistorikk?.isNotEmpty() == true) {
                return kravHistorikk.first()
            }
            logger.warn("Fant ikke Kravhistorikk med bruk av kravårsak: ${EessiFellesDto.EessiKravAarsak.GJNL_SKAL_VURD} eller ${EessiFellesDto.EessiKravAarsak.TILST_DOD} ")
            return null
    }

    fun hentKravhistorikkForGjenlevendeOgNySoknad(kravHistorikkListe: List<KravHistorikk>?): KravHistorikk? {
        val kravHistorikk = kravHistorikkListe?.filter { krav -> krav.kravAarsak == EessiFellesDto.EessiKravAarsak.GJNL_SKAL_VURD || krav.kravAarsak == EessiFellesDto.EessiKravAarsak.TILST_DOD || krav.kravAarsak == EessiFellesDto.EessiKravAarsak.NY_SOKNAD }
        if (kravHistorikk?.isNotEmpty() == true) {
            return kravHistorikk.first()
        }
        logger.warn("Fant ikke Kravhistorikk med bruk av kravårsak: ${EessiFellesDto.EessiKravAarsak.GJNL_SKAL_VURD} , ${EessiFellesDto.EessiKravAarsak.TILST_DOD} eller ${EessiFellesDto.EessiKravAarsak.NY_SOKNAD} fra kravliste: \n${kravHistorikkListe.toString()}")
        return null
    }

    fun hentKravHistorikkMedKravStatusInnvilget(sak: P2xxxMeldingOmPensjonDto.Sak?): KravHistorikk? {
        val sortList = sortertKravHistorikk(sak?.kravHistorikk)
        sortList?.forEach {
            logger.info("leter etter Krav status med ${EessiSakStatus.INNV}, fant ${it.kravType} med virkningstidspunkt dato : ${it.virkningstidspunkt}")
            if (EessiSakStatus.INNV == it.kravStatus) {
                logger.info("Fant Kravhistorikk med ${it.kravStatus}")
                return it
            } else if (EessiSakStatus.TIL_BEHANDLING == it.kravStatus && sak?.ytelsePerMaaned?.firstOrNull()?.belop != null) {
            logger.info("Fant Krav med status ${EessiSakStatus.TIL_BEHANDLING}, med kravtype: ${it.kravType} og virkningstidspunkt dato: ${it.virkningstidspunkt}")
                return it
            }
        }
        logger.error("Fant ikke noe Kravhistorikk..${EessiSakStatus.TIL_BEHANDLING}. Mangler vilkårsprlving/vedtak. følger ikke normal behandling")
        return null
    }

    fun hentKravHistorikkMedKravStatusAvslag(sak: P2xxxMeldingOmPensjonDto.Sak?): KravHistorikk? {
        val sortList = sortertKravHistorikk(sak?.kravHistorikk)
        sortList?.forEach {
            logger.info("leter etter Krav status med ${EessiSakStatus.AVSL}, fant ${it.kravType} med virkningstidspunkt dato : ${it.virkningstidspunkt}")
            if (EessiSakStatus.AVSL == it.kravStatus) {
                logger.info("Fant Kravhistorikk med ${it.kravStatus}")
                return it
            } else if (sak?.status == EessiSakStatus.AVSL) {
                return it
            }
        }
        logger.error("Fant ikke noe Kravhistorikk..${EessiSakStatus.AVSL}. Mangler vilkårsprøving. følger ikke normal behandling")
        return null
    }

    fun hentKravHistorikkMedValgtKravType(kravHistorikkListe: List<KravHistorikk>?, penKravtype: EessiFellesDto.EessiKravGjelder): KravHistorikk? {
        val sortList = sortertKravHistorikk(kravHistorikkListe)
        if (sortList == null || sortList.size > 1) return null
        logger.info("leter etter kravtype: $penKravtype")
        return sortList.firstOrNull { kravhist -> kravhist.kravType == penKravtype}
            .also { logger.info("fant ${it?.kravType} med kravÅrsak: ${it?.kravAarsak} med virkningstidspunkt dato : ${it?.virkningstidspunkt}") }

    }

    fun finnKravHistorikkForDato(pensak: P2xxxMeldingOmPensjonDto.Sak?): KravHistorikk {
        try {
            val gjenLevKravarsak = hentKravhistorikkForGjenlevende(pensak?.kravHistorikk)
            if (gjenLevKravarsak != null) return gjenLevKravarsak

            val kravKunUtland = hentKravHistorikkMedValgtKravType(pensak?.kravHistorikk, EessiFellesDto.EessiKravGjelder.F_BH_KUN_UTL)
            if (kravKunUtland != null) return  kravKunUtland

            logger.info("Sakstatus: ${pensak?.status},sakstype: ${pensak?.sakType}")
            val innvilgetKrav = hentKravHistorikkMedKravStatusInnvilget(pensak)
            val avslaattKrav = hentKravHistorikkMedKravStatusAvslag(pensak)
            return innvilgetKrav ?: avslaattKrav ?: hentKravHistorikkForsteGangsBehandlingUtlandEllerForsteGang(pensak?.kravHistorikk)

        } catch (ex: Exception) {
            logger.warn("Fant ingen gyldig kravdato: $ex")
            return KravHistorikk()
        }
    }
}
