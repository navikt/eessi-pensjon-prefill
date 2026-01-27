package no.nav.eessi.pensjon.prefill.sed.krav

import no.nav.eessi.pensjon.prefill.models.pensjon.EessiSakStatus
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiSakStatus.AVSL
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiSakStatus.INNV
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiSakType

/**
     *  [07] Førtidspensjon
     *  [08] Uførepensjon
     *  [10] Alderspensjon
     *  [11] Etterlattepensjon
     */
     fun mapSaktype(saktype: EessiSakType?): String {
            return when (saktype) {
                EessiSakType.UFOREP -> "08"
                EessiSakType.ALDER -> "10"
                else -> "11"
            }
    }

    /**
     *  [01] Søkt
     *  [02] Innvilget
     *  [03] Avslått
     */
    fun mapSakstatus(sakstatus: EessiSakStatus): String {
        return try {
            when (sakstatus) {
                INNV -> "02"
                AVSL -> "03"
                else -> "01"
            }
        } catch (ex: Exception) {
            "01"
        }
    }
