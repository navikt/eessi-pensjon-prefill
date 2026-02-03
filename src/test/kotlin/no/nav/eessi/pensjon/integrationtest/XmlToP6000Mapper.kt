package no.nav.eessi.pensjon.integrationtest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import no.nav.eessi.pensjon.prefill.models.pensjon.P6000MeldingOmVedtakDto
import no.nav.eessi.pensjon.prefill.models.pensjon.Ytelseskomponent
import java.io.File
import java.time.LocalDate

object XmlToP6000Mapper {

    fun readP6000FromXml(path: String): P6000MeldingOmVedtakDto {
        val xmlMapper = XmlMapper()
        val resource = javaClass.getResource(path)
        val root: JsonNode = xmlMapper.readTree(File(resource!!.toURI()))

        fun JsonNode.findFieldByLocalName(localName: String): JsonNode? =
            properties().asSequence().firstOrNull { it.key.substringAfter(':') == localName }?.value

        fun JsonNode.textNormalizedOrNull(): String? {
            if (isMissingNode || isNull) return null
            var s = asText().trim().takeIf { it.isNotEmpty() } ?: return null
            while (s.length >= 2 && s.first() == '"' && s.last() == '"') {
                s = s.substring(1, s.length - 1).trim()
            }
            s = s.replace("\\\"", "\"").trim()
            return s.takeIf { it.isNotEmpty() && it.lowercase() != "null" }
        }

        fun JsonNode.localDateOrNull(field: String): LocalDate? =
            findFieldByLocalName(field)?.textNormalizedOrNull()?.substring(0, 10)?.let { LocalDate.parse(it) }

        // Avdod
        val avdodNode = root.findFieldByLocalName("avdod") ?: root.path("avdod")
        val avdod = P6000MeldingOmVedtakDto.Avdod(
            avdod = avdodNode.findFieldByLocalName("avdod")?.asText(),
            avdodBoddArbeidetUtland = avdodNode.findFieldByLocalName("avdodBoddArbeidetUtland")?.asBoolean(),
            avdodFarBoddArbeidetUtland = avdodNode.findFieldByLocalName("avdodFarBoddArbeidetUtland")?.asBoolean(),
            avdodMorBoddArbeidetUtland = avdodNode.findFieldByLocalName("avdodMorBoddArbeidetUtland")?.asBoolean()
        )

        // SakAlder
        val sakAlderNode = root.findFieldByLocalName("sakAlder") ?: root.path("sakAlder")
        val sakAlder = P6000MeldingOmVedtakDto.SakAlder(
            sakType = sakAlderNode.findFieldByLocalName("sakType")?.asText()?.let { no.nav.eessi.pensjon.prefill.sed.vedtak.helper.KSAK.valueOf(it) }
                ?: no.nav.eessi.pensjon.prefill.sed.vedtak.helper.KSAK.ALDER
        )

        // Trygdeavtale
        val trygdeavtaleNode = root.findFieldByLocalName("trygdeavtale") ?: root.path("trygdeavtale")
        val trygdeavtale = P6000MeldingOmVedtakDto.Trygdeavtale(
            erArt10BruktGP = trygdeavtaleNode.findFieldByLocalName("erArt10BruktGP")?.asBoolean(),
            erArt10BruktTP = trygdeavtaleNode.findFieldByLocalName("erArt10BruktTP")?.asBoolean()
        )

        // TrygdetidListe
        val trygdetidListeNode = root.findFieldByLocalName("trygdetidListe") ?: root.path("trygdetidListe")
        val trygdetidList = trygdetidListeNode
            .findFieldByLocalName("trygdetidListe")?.let { it } ?: trygdetidListeNode
        val trygdetidListe = trygdetidList.mapNotNull { ttNode ->
            val fom = ttNode.localDateOrNull("fom")
            val tom = ttNode.localDateOrNull("tom")
            if (fom != null && tom != null) P6000MeldingOmVedtakDto.Trygdetid(fom, tom) else null
        }

        // Vedtak
        val vedtakNode = root.findFieldByLocalName("vedtak") ?: root.path("vedtak")
        val vedtak = vedtakNode.localDateOrNull("virkningstidspunkt")?.let {
            P6000MeldingOmVedtakDto.Vedtak(
                virkningstidspunkt = it,
                kravGjelder = vedtakNode.findFieldByLocalName("kravGjelder")?.asText() ?: "",
                hovedytelseTrukket = vedtakNode.findFieldByLocalName("hovedytelseTrukket")?.asBoolean() ?: false,
                boddArbeidetUtland = vedtakNode.findFieldByLocalName("boddArbeidetUtland")?.asBoolean(),
                datoFattetVedtak = vedtakNode.localDateOrNull("datoFattetVedtak")
            )
        }

        // VilkarsvurderingListe
        val vilkarsvurderingListeNode = root.findFieldByLocalName("vilkarsvurderingListe") ?: root.path("vilkarsvurderingListe")
//        val vilkarsvurderingList = vilkarsvurderingListeNode
//            .findFieldByLocalName("vilkarsvurderingListe")?.let { it } ?: vilkarsvurderingListeNode
        val vilkarsvurderingList = vilkarsvurderingListeNode
            .findFieldByLocalName("vilkarsvurderingListe")
            ?.asList() ?: emptyList()

        val vilkarsvurderingListe = vilkarsvurderingList.map { vvNode ->
            P6000MeldingOmVedtakDto.Vilkarsvurdering(
                fom = vvNode.localDateOrNull("fom") ?: LocalDate.MIN,
                vilkarsvurderingUforetrygd = vvNode.findFieldByLocalName("vilkarsvurderingUforetrygd")?.let { uNode ->
                    P6000MeldingOmVedtakDto.VilkarsvurderingUforetrygd(
                        alder = uNode.findFieldByLocalName("alder")?.asText(),
                        hensiktsmessigBehandling = uNode.findFieldByLocalName("hensiktsmessigBehandling")?.asText(),
                        hensiktsmessigArbeidsrettedeTiltak = uNode.findFieldByLocalName("hensiktsmessigArbeidsrettedeTiltak")?.asText(),
                        nedsattInntektsevne = uNode.findFieldByLocalName("nedsattInntektsevne")?.asText(),
                        unntakForutgaendeMedlemskap = uNode.findFieldByLocalName("unntakForutgaendeMedlemskap")?.asText()
                    )
                },
                resultatHovedytelse = vvNode.findFieldByLocalName("resultatHovedytelse")?.asText() ?: "",
                harResultatGjenlevendetillegg = vvNode.findFieldByLocalName("harResultatGjenlevendetillegg")?.asBoolean() ?: false,
                avslagHovedytelse = vvNode.findFieldByLocalName("avslagHovedytelse")?.asText()
            )
        }

        // YtelsePerMaanedListe
        val ytelsePerMaanedListeNode = root.findFieldByLocalName("ytelsePerMaanedListe") ?: root.path("ytelsePerMaanedListe")
        val ytelsePerMaanedList = ytelsePerMaanedListeNode
            .findFieldByLocalName("ytelsePerMaanedListe")?.let { it } ?: ytelsePerMaanedListeNode
        val ytelsePerMaanedListe = ytelsePerMaanedList.map { yNode ->
            P6000MeldingOmVedtakDto.YtelsePerMaaned(
                fom = yNode.localDateOrNull("fom") ?: LocalDate.MIN,
                tom = yNode.localDateOrNull("tom"),
                mottarMinstePensjonsniva = yNode.findFieldByLocalName("mottarMinstePensjonsniva")?.asBoolean() ?: false,
                vinnendeBeregningsmetode = yNode.findFieldByLocalName("vinnendeBeregningsmetode")?.asText() ?: "",
                belop = yNode.findFieldByLocalName("belop")?.asInt() ?: 0,
                ytelseskomponentListe = yNode.findFieldByLocalName("ytelseskomponentListe")?.map { kompNode ->
                    Ytelseskomponent(
                        ytelsesKomponentType = kompNode.findFieldByLocalName("ytelsesKomponentType")?.asText() ?: "",
                        belopTilUtbetaling = kompNode.findFieldByLocalName("belopTilUtbetaling")?.asInt() ?: 0
                    )
                } ?: emptyList()
            )
        }

        return P6000MeldingOmVedtakDto(
            avdod = avdod,
            sakAlder = sakAlder,
            trygdeavtale = trygdeavtale,
            trygdetidListe = trygdetidListe,
            vedtak = vedtak!!,
            vilkarsvurderingListe = vilkarsvurderingListe,
            ytelsePerMaanedListe = ytelsePerMaanedListe
        )
    }

    private fun JsonNode.asList(): List<JsonNode> =
        when {
            isMissingNode || isNull -> emptyList()
            isArray -> toList()
            else -> listOf(this)
        }
}