package no.nav.eessi.eessifagmodul.prefill.kravsak

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader

@Component
class KravSakFullService {

    private val logger: Logger by lazy { LoggerFactory.getLogger(KravSakFullService::class.java) }
    private val kravSakMap: MutableMap<String?, List<KravSak>>

    init {
        val FILENAME = "/kodeverk/k-krav-sak-full.txt"
        val `in` = this.javaClass.getResourceAsStream(FILENAME)
        val br = BufferedReader(InputStreamReader(`in`, "UTF-8"))
        var line: String? = ""
        val csvSplitBy = ";"
        kravSakMap = mutableMapOf()

        while (line  != null) {
            line = br.readLine()
            if (line != null) {
                val sakArray = line.split(csvSplitBy.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                val keyword = sakArray[1]
                val data = KravSak(sakArray[1], sakArray[0], sakArray[2], sakArray[3])

                addKravSakToMap(keyword, data)
                //logger.debug("kravSakMap:  ${kravSakMap[keyword]}")
            }
        }
        logger.debug("Har importert kodeverk fra $FILENAME")
    }

    private final fun addKravSakToMap(keyword: String, model: KravSak) {
            if (kravSakMap[keyword] == null) {
                kravSakMap.put(keyword, mutableListOf(model))
            }  else {
                val list = kravSakMap.get(keyword) as MutableList<KravSak>
                list.add(model)
            }
    }


    fun finnKravSakFull(kravSakFull: String): KravSak? {
        kravSakMap.keys.forEach {
            kravSakMap[it]?.forEach {
                if (kravSakFull == it.kravSakFull) {
                    return it
                }
            }
        }
        return null
    }

    fun finnKravGjelder(kravGjelder: String): List<KravSak>? {
        kravSakMap.keys.forEach {
            if (kravGjelder == it) {
                return kravSakMap[it]
            }
        }
        return null
    }

}