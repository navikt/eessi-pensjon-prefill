package no.nav.eessi.eessifagmodul.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*


@Component
class LandkodeService {

    private val logger: Logger by lazy { LoggerFactory.getLogger(LandkodeService::class.java) }
    private val landKodeTable: MutableMap<String?, Landkode>

    init {
        landKodeTable = HashMap()
        val `in` = this.javaClass.getResourceAsStream(FILENAME)
        val br = BufferedReader(InputStreamReader(`in`, "UTF-8"))
        var line: String? = ""
        val csvSplitBy = ";"

        while (line  != null) {
            line = br.readLine()
            if (line != null) {
                val landArray = line.split(csvSplitBy.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (landArray[0].length != 2 || landArray[1].length != 3 || landArray[2].length < 2) {
                    continue
                }
                val data = Landkode(landArray[0], landArray[1], landArray[2], landArray[3])
                landKodeTable.put(data.alpha3, data)
                landKodeTable.put(data.alpha2, data)
            }
        }
        logger.debug("Har importert kodeverk fra $FILENAME")
    }

    fun finnLandkode3(alpha2: String): String? {
        return if (landKodeTable[alpha2] == null) {
            println("Finner ikke landkode for $alpha2, sjekk om ny landkoder.txt må lastes ned.")
            null
        } else {
            landKodeTable[alpha2]?.alpha3
        }
    }

    fun hentLandkoer2(): List<String> {
        val landlist: MutableList<Landkode> = mutableListOf()
        println("Map landKodeTable : $landKodeTable")
        landKodeTable.keys.forEach {
            if (it?.length == 2) {
                landlist.add( landKodeTable[it]!! )
            }
        }
        val listsort: List<Landkode> = landlist.sortedBy { (_,_,_, sorting) -> sorting}.toList()
        //println("Sortertlist : $listsort")
        val list : MutableList<String> = mutableListOf()
        listsort.forEach {
            list.add(it?.alpha2!!)
        }
        //println("Filtrert Sortedlist : $list")
        return list
    }

    fun finnLandkode2(alpha3: String): String? {
        return if (landKodeTable[alpha3] == null) {
            logger.debug("Finner ikke landkode for $alpha3, sjekk om ny landkoder.txt må lastes ned.")
            null
        } else {
            landKodeTable[alpha3]?.alpha2
        }
    }

    private companion object {
        private val FILENAME = "/kodeverk/landkoder.txt"
    }

    private data class Landkode(
        val alpha2: String? = null,
        val alpha3: String? = null,
        val land: String? = null,
        val sorting: String? = null
       )


}