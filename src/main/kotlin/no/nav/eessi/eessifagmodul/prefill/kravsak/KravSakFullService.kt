package no.nav.eessi.eessifagmodul.prefill.kravsak

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader

@Component
class KravSakFullService {

    private val logger: Logger by lazy { LoggerFactory.getLogger(KravSakFullService::class.java) }
    private val kravSakFull: MutableMap<String?, KravSak>

    init {
        val FILENAME = "/kodeverk/k-krav-sak-full.txt"
        val `in` = this.javaClass.getResourceAsStream(FILENAME)
        val br = BufferedReader(InputStreamReader(`in`, "UTF-8"))
        var line: String? = ""
        val csvSplitBy = ";"
        kravSakFull = mutableMapOf()

        while (line  != null) {
            line = br.readLine()
            if (line != null) {
                val sakArray = line.split(csvSplitBy.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                val data = KravSak(sakArray[0], sakArray[1], sakArray[2], sakArray[3])

                kravSakFull[data.kKravGjelder] = data
                kravSakFull[data.kKravSakFull] = data
                kravSakFull[data.kSakT] = data
            }
        }
        logger.debug("Har importert kodeverk fra $FILENAME")
//        hentListeKravSak().forEach {
//            logger.debug("$it")
//        }
    }

    fun finnKravSakFull(kKravSakFull: String): KravSak? {
        return if (kravSakFull[kKravSakFull] == null) {
            logger.debug("Finner ikke KravSak for $kKravSakFull, sjekk om ny KravSak må lastes.")
            null
        } else {
            kravSakFull[kKravSakFull]
        }
    }

    fun finnKravGjelder(kKravGjelder: String): KravSak? {
        return if (kravSakFull[kKravGjelder] == null) {
            logger.debug("Finner ikke KravSak for $kKravGjelder, sjekk om ny KravSak må lastes.")
            null
        } else {
            kravSakFull[kKravGjelder]
        }
    }

    fun hentListeKravSak(): List<KravSak> {
        val kravset = mutableSetOf<KravSak>()
        kravSakFull.keys.forEach {
            val value = kravSakFull[it]
            if (value != null) {
                kravset.add(value)
            }
        }
        return kravset.toList()
    }

    fun hentKravSakMap(): Map<String?, KravSak> {
        return kravSakFull
    }

}