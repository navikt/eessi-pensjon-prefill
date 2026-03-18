package no.nav.eessi.pensjon.prefill.sed.krav

import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakStatus
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakStatus.AVSL
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakStatus.INNV
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto.EessiSakType.*

/**
     *  [07] Førtidspensjon
     *  [08] Uførepensjon
     *  [10] Alderspensjon
     *  [11] Etterlattepensjon
     */
     fun mapSaktype(saktype: EessiFellesDto.EessiSakType?): String {
            return when (saktype) {
                UFOREP -> "08"
                ALDER -> "10"
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
