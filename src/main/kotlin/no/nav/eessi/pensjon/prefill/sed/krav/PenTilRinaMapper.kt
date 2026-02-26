package no.nav.eessi.pensjon.prefill.sed.krav

import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto

/**
     *  [07] Førtidspensjon
     *  [08] Uførepensjon
     *  [10] Alderspensjon
     *  [11] Etterlattepensjon
     */
     fun mapSaktype(saktype: EessiFellesDto.EessiSakType?): String {
            return when (saktype) {
                EessiFellesDto.EessiSakType.UFOREP -> "08"
                EessiFellesDto.EessiSakType.ALDER -> "10"
                else -> "11"
            }
    }

    /**
     *  [01] Søkt
     *  [02] Innvilget
     *  [03] Avslått
     */
    fun mapSakstatus(sakstatus: EessiFellesDto.EessiSakStatus): String {
        return try {
            when (sakstatus) {
                EessiFellesDto.EessiSakStatus.INNV -> "02"
                EessiFellesDto.EessiSakStatus.AVSL -> "03"
                else -> "01"
            }
        } catch (ex: Exception) {
            "01"
        }
    }
