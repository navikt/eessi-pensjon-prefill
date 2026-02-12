package no.nav.eessi.pensjon.integrationtest.sed

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
import no.nav.eessi.pensjon.prefill.models.pensjon.Ytelseskomponent
import java.io.File
import java.time.LocalDate

object XmlToP2xxxMapper {

    fun readP2000FromXml(path: String): P2xxxMeldingOmPensjonDto {
        val xmlMapper = XmlMapper()
        val resource = javaClass.getResource(path)
        val root: JsonNode = xmlMapper.readTree(File(resource!!.toURI()))

        val sakNode = root
            .path("brukersSakerListe")
            .path("brukersSakerListe")
            .firstOrNull() ?: throw IllegalArgumentException("No sak found")

        val sakType = EessiFellesDto.EessiSakType.valueOf(sakNode.path("sakType").asText())
        val status = EessiFellesDto.EessiSakStatus.valueOf(sakNode.path("status").asText())
        val forsteVirkningstidspunkt = LocalDate.parse(sakNode.path("forsteVirkningstidspunkt").asText().substring(0, 10))

        val kravHistorikk = sakNode
            .path("kravHistorikkListe")
            .path("kravHistorikkListe")
            .map { kravNode ->
                val kravStatusText = kravNode.path("status").asText()
                val kravStatus = if (kravStatusText == "Ingen status") {
                    EessiFellesDto.EessiSakStatus.INGEN_STATUS
                } else {
                    EessiFellesDto.EessiSakStatus.valueOf(kravStatusText)
                }
                P2xxxMeldingOmPensjonDto.KravHistorikk(
                    mottattDato = LocalDate.parse(kravNode.path("mottattDato").asText().substring(0, 10)),
                    kravType = EessiFellesDto.EessiKravGjelder.valueOf(kravNode.path("kravType").asText()),
                    virkningstidspunkt = kravNode.path("virkningstidspunkt")?.let {
                        if (it.isMissingNode) null else LocalDate.parse(it.asText().substring(0, 10))
                    },
                    kravStatus = kravStatus
                )
            }

        val ytelsePerMaaned = sakNode
            .path("ytelsePerMaanedListe")
            .path("ytelsePerMaanedListe")
            .map { ytelseNode ->
                P2xxxMeldingOmPensjonDto.YtelsePerMaaned(
                    fom = LocalDate.parse(ytelseNode.path("fom").asText().substring(0, 10)),
                    belop = ytelseNode.path("belop").asInt(),
                    ytelseskomponent = ytelseNode
                        .path("ytelseskomponentListe")
                        .map { kompNode ->
                            Ytelseskomponent(
                                ytelsesKomponentType = kompNode.path("ytelsesKomponentType").asText(),
                                belopTilUtbetaling = kompNode.path("belopTilUtbetaling").asInt()
                            )
                        }
                )
            }

        return P2xxxMeldingOmPensjonDto(
            sak = P2xxxMeldingOmPensjonDto.Sak(
                sakType = sakType,
                kravHistorikk = kravHistorikk,
                ytelsePerMaaned = ytelsePerMaaned,
                forsteVirkningstidspunkt = forsteVirkningstidspunkt,
                status = status
            ),
            vedtak = null
        )
    }


    fun readP2100FromXml(path: String): P2xxxMeldingOmPensjonDto {

        val xmlMapper = XmlMapper()
        val resource = javaClass.getResource(path)
        val root: JsonNode = xmlMapper.readTree(File(resource!!.toURI()))
        root.properties().forEach { println(it.key) }

        val sakNode = root
            .unwrapBrukersSakerListe()
            ?.firstOrNull() ?: throw IllegalArgumentException("No sak found")
//        val sakNode = root
//            .findFieldByLocalName("brukersSakerListe")
//            ?.findFieldByLocalName("brukersSakerListe")
//            ?.firstOrNull() ?: throw IllegalArgumentException("No sak found")
//
//        val sakType = EessiFellesDto.EessiSakType.valueOf(sakNode.path("sakType").asText())

        val sakType = enumOrDefault(
            sakNode.path("sakType").textNormalizedOrNull(),
            EessiFellesDto.EessiSakType.ALDER
        )

        val status = enumOrDefault(
            sakNode.path("status").textNormalizedOrNull(),
            EessiFellesDto.EessiSakStatus.INGEN_STATUS
        )
//        val forsteVirkningstidspunkt = LocalDate.parse(sakNode.path("forsteVirkningstidspunkt").textNormalized().asText().substring(0, 10))
        val forsteVirkningstidspunkt =
            sakNode.path("forsteVirkningstidspunkt").textNormalizedOrNull()
                ?.let { LocalDate.parse(it.substring(0, 10)) }

        val kravHistorikk = sakNode
            .path("kravHistorikkListe")
            .map { kravNode ->
                val kravStatusText = kravNode.path("status").asText()
                val kravStatus = if (kravStatusText == "Ingen status") {
                    EessiFellesDto.EessiSakStatus.INGEN_STATUS
                } else {
                    EessiFellesDto.EessiSakStatus.valueOf(kravStatusText)
                }
                P2xxxMeldingOmPensjonDto.KravHistorikk(
                    kravType = EessiFellesDto.EessiKravGjelder.valueOf(kravNode.path("kravType").asText()),
                    virkningstidspunkt = kravNode.path("virkningstidspunkt")?.let {
                        if (it.isMissingNode) null else LocalDate.parse(it.asText().substring(0, 10))
                    },
                    kravStatus = kravStatus
                )
            }

        val ytelsePerMaaned = sakNode
            .path("ytelsePerMaanedListe")
            .map { ytelseNode ->
                P2xxxMeldingOmPensjonDto.YtelsePerMaaned(
                    fom = LocalDate.parse(ytelseNode.path("fom").asText().substring(0, 10)),
                    belop = ytelseNode.path("belop").asInt(),
                    ytelseskomponent = ytelseNode
                        .path("ytelseskomponentListe")
                        .map { kompNode ->
                            Ytelseskomponent(
                                ytelsesKomponentType = kompNode.path("ytelsesKomponentType").asText(),
                                belopTilUtbetaling = kompNode.path("belopTilUtbetaling").asInt()
                            )
                        }
                )
            }

        return P2xxxMeldingOmPensjonDto(
            sak = P2xxxMeldingOmPensjonDto.Sak(
                sakType = sakType,
                kravHistorikk = kravHistorikk,
                ytelsePerMaaned = ytelsePerMaaned,
                forsteVirkningstidspunkt = forsteVirkningstidspunkt,
                status = status
            ),
            vedtak = null
        )
    }

    private fun JsonNode.unwrapBrukersSakerListe(): JsonNode? {
        val node = findFieldByLocalName("brukersSakerListe") ?: return null
        if (node is ObjectNode) {
            node.remove("type")
        }
        return node
    }
    private fun JsonNode.findFieldByLocalName(localName: String): JsonNode? =
        properties().asSequence().firstOrNull { it.key.substringAfter(':') == localName }?.value

    private inline fun <reified E : Enum<E>> enumOrNull(raw: String?): E? =
        raw?.let { runCatching { enumValueOf<E>(it) }.getOrNull() }

    private inline fun <reified E : Enum<E>> enumOrDefault(raw: String?, default: E): E =
        enumOrNull<E>(raw) ?: default

    private fun JsonNode.textNormalizedOrNull(): String? {
        if (isMissingNode || isNull) return null

        var s = asText()
            .trim()
            .takeIf { it.isNotEmpty() } ?: return null

        while (s.length >= 2 && s.first() == '"' && s.last() == '"') {
            s = s.substring(1, s.length - 1).trim()
        }

        s = s.replace("\\\"", "\"").trim()

        return s.takeIf { it.isNotEmpty() && it.lowercase() != "null" }
    }
}