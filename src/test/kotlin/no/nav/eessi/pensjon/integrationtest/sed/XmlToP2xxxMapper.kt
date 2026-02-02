package no.nav.eessi.pensjon.integrationtest.sed

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import no.nav.eessi.pensjon.prefill.models.pensjon.EessiFellesDto
import no.nav.eessi.pensjon.prefill.models.pensjon.P2xxxMeldingOmPensjonDto
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
                    ytelseskomponentListe = ytelseNode
                        .path("ytelseskomponentListe")
                        .map { kompNode ->
                            EessiFellesDto.Ytelseskomponent(
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
}