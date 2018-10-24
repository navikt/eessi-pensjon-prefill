package no.nav.eessi.eessifagmodul.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*


@Component
class PostnummerService {
    private val logger: Logger by lazy { LoggerFactory.getLogger(PostnummerService::class.java) }
    private val postalCodeTable: MutableMap<String?, PostData>

    init {
        postalCodeTable = HashMap()
        val resource = this.javaClass.getResourceAsStream(FILENAME)
        val br = BufferedReader(InputStreamReader(resource, "UTF-8"))
        var line: String? = ""
        val csvSplitBy = "\t"

        while (line != null) {
            line = br.readLine()
            if (line != null) {
                val postArray = line.split(csvSplitBy.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val data = PostData(postArray[0], postArray[1])
                postalCodeTable[data.postnmmer] = data
            }
        }
        logger.debug("Har importert kodeverk fra $FILENAME")
    }

    private data class PostData(
            val postnmmer: String? = null,
            val poststed: String? = null
    )

    fun finnPoststed(postnr: String): String? {
        return if (postalCodeTable[postnr] == null) {
            logger.debug("Finner ikke poststed for postnummer: $postnr, sjekk om ny postnummer.txt må lastes ned.")
            null
        } else {
            postalCodeTable[postnr]?.poststed
        }
    }

    private companion object {
        private const val FILENAME = "/kodeverk/postnummerregister.txt"
    }
}