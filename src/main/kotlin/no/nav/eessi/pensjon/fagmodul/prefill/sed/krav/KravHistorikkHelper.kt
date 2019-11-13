package no.nav.eessi.pensjon.fagmodul.prefill.sed.krav

import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.sedmodel.*
import no.nav.eessi.pensjon.utils.simpleFormat
import no.nav.pensjon.v1.kravhistorikk.V1KravHistorikk
import no.nav.pensjon.v1.kravhistorikkliste.V1KravHistorikkListe
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object KravHistorikkHelper {
    private val logger: Logger by lazy { LoggerFactory.getLogger(KravHistorikkHelper::class.java) }

    /**
     *  9.1
     *
     *  Setter kravdato på sed (denne kommer fra PESYS men opprettes i nav?!)
     */
    fun settKravdato(prefillData: PrefillDataModel, sed: SED) {
        if (prefillData.kanFeltSkippes("NAVSED")) {
            //sed.nav?.krav = Krav("")
            //pensjon.kravDato = null
        } else {
            logger.debug("9.1     legger til nav kravdato fra pensjon kravdato : ${sed.pensjon?.kravDato} ")
            sed.nav?.krav = sed.pensjon?.kravDato
        }
    }

    private fun sortertKravHistorikk(kravHistorikkListe: V1KravHistorikkListe): List<V1KravHistorikk> {
        val list = kravHistorikkListe.kravHistorikkListe.toList()
        return list.asSequence().sortedBy { it.mottattDato.toGregorianCalendar() }.toList()
    }

    fun hentKravHistorikkSisteRevurdering(kravHistorikkListe: V1KravHistorikkListe): V1KravHistorikk {
        val sortList = sortertKravHistorikk(kravHistorikkListe)

        for (i in sortList) {
            logger.debug("leter etter ${Kravtype.REVURD} i  ${i.kravType} med dato ${i.virkningstidspunkt}")
            if (i.kravType == Kravtype.REVURD.name) {
                logger.debug("Fant Kravhistorikk med $i.kravType")
                return i
            }
        }
        return V1KravHistorikk()
    }

    fun hentKravHistorikkForsteGangsBehandlingUtlandEllerForsteGang(kravHistorikkListe: V1KravHistorikkListe): V1KravHistorikk {
        return hentKravHistorikkMedKravType(listOf(Kravtype.F_BH_MED_UTL.name, Kravtype.FORSTEG_BH.name), kravHistorikkListe)
    }

    private fun hentKravHistorikkMedKravType(kravType: List<String>, kravHistorikkListe: V1KravHistorikkListe): V1KravHistorikk {
        val sortList = sortertKravHistorikk(kravHistorikkListe)
        sortList.forEach {
            logger.debug("leter etter Kravtype: $kravType, fant ${it.kravType} med dato i ${it.virkningstidspunkt}")
            if (kravType.contains(it.kravType)) {
                logger.debug("Fant Kravhistorikk med $kravType")
                return it
            }
        }
        logger.error("Fant ikke noe Kravhistorikk. med $kravType HVA GJØR VI NÅ?")
        return V1KravHistorikk()
    }

    fun hentKravHistorikkMedKravStatusTilBehandling(kravHistorikkListe: V1KravHistorikkListe): V1KravHistorikk {
        val sortList = sortertKravHistorikk(kravHistorikkListe)
        sortList.forEach {
            logger.debug("leter etter Krav status med ${Kravstatus.TIL_BEHANDLING}, fant ${it.kravType} med virkningstidspunkt dato : ${it.virkningstidspunkt}")
            if (Kravstatus.TIL_BEHANDLING.name == it.status) {
                logger.debug("Fant Kravhistorikk med ${it.status}")
                return it
            }
        }
        logger.error("Fant ikke noe Kravhistorikk..${Kravstatus.TIL_BEHANDLING} HVA GJØR VI NÅ?")
        return V1KravHistorikk()
    }

    fun hentKravHistorikkMedKravStatusAvslag(kravHistorikkListe: V1KravHistorikkListe): V1KravHistorikk {
        val sortList = sortertKravHistorikk(kravHistorikkListe)
        sortList.forEach {
            logger.debug("leter etter Krav status med ${Kravstatus.AVSL}, fant ${it.kravType} med virkningstidspunkt dato : ${it.virkningstidspunkt}")
            if (Kravstatus.AVSL.name == it.status) {
                logger.debug("Fant Kravhistorikk med ${it.status}")
                return it
            }
        }
        logger.error("Fant ikke noe Kravhistorikk..${Kravstatus.TIL_BEHANDLING} HVA GJØR VI NÅ?")
        return V1KravHistorikk()
    }


    /**
     * 9.1- 9.2
     *
     *  Fra PSAK, kravdato på alderspensjonskravet
     *  Fra PSELV eller manuell kravblankett:
     *  Fyller ut fra hvilket tidspunkt bruker ønsker å motta pensjon fra Norge.
     *  Det er et spørsmål i søknadsdialogen og på manuell kravblankett. Det er ikke nødvendigvis lik virkningstidspunktet på pensjonen.
     */
    fun createKravDato(valgtKrav: V1KravHistorikk): Krav? {
        logger.debug("9.1        Dato Krav (med korrekt data fra PESYS krav.virkningstidspunkt)")
        logger.debug("KravType   :  ${valgtKrav.kravType}")
        logger.debug("mottattDato:  ${valgtKrav.mottattDato}")
        logger.debug("--------------------------------------------------------------")

        logger.debug("Prøver å sette kravDato til Virkningstidpunkt: ${valgtKrav.kravType} og dato: ${valgtKrav.mottattDato}")
        return Krav(
                dato = valgtKrav.mottattDato?.simpleFormat() ?: ""
        )
    }

}
