package no.nav.eessi.pensjon.fagmodul.eux

import no.nav.eessi.pensjon.fagmodul.models.SEDType

class ValidBucAndSed {

    fun getAvailableSedOnBuc(bucType: String?): List<SEDType> {
        val map = initSedOnBuc()

        if (bucType.isNullOrEmpty()) {
            val set = mutableSetOf<SEDType>()
            map["P_BUC_01"]?.let { set.addAll(it) }
            map["P_BUC_02"]?.let { set.addAll(it) }
            map["P_BUC_03"]?.let { set.addAll(it) }
            map["P_BUC_05"]?.let { set.addAll(it) }
            map["P_BUC_06"]?.let { set.addAll(it) }
            map["P_BUC_09"]?.let { set.addAll(it) }
            map["P_BUC_10"]?.let { set.addAll(it) }
            return set.toList()
        }
        return map[bucType].orEmpty()
    }

    /**
     * Own impl. no list from eux that contains list of SED to a speific BUC
     */
    fun initSedOnBuc(): Map<String, List<SEDType>> {
        return mapOf(
                "P_BUC_01" to listOf(SEDType.P2000),
                "P_BUC_02" to listOf(SEDType.P2100),
                "P_BUC_03" to listOf(SEDType.P2200),
                "P_BUC_05" to listOf(SEDType.P8000),
                "P_BUC_06" to listOf(SEDType.P5000, SEDType.P6000, SEDType.P7000, SEDType.P10000),
                "P_BUC_09" to listOf(SEDType.P14000),
                "P_BUC_10" to listOf(SEDType.P15000),
                "P_BUC_04" to listOf(SEDType.P1000),
                "P_BUC_07" to listOf(SEDType.P11000),
                "P_BUC_08" to listOf(SEDType.P12000)
        )
    }

}