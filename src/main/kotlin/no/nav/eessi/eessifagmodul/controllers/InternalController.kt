package no.nav.eessi.eessifagmodul.controllers

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.StringWriter
import java.time.Instant
import java.util.*
import kotlin.collections.HashSet

@CrossOrigin
@RestController
@RequestMapping("/internal")
class InternalController {

    private val logger: Logger by lazy { LoggerFactory.getLogger(InternalController::class.java) }
    private val registry: CollectorRegistry by lazy { CollectorRegistry.defaultRegistry }

    data class SelftestResult(
            val name: String = "eessi-fagmodul",
            val version: String = "0.0.1-SNAPSHOT",
            val timestamp: Instant = Instant.now(),
            val aggregateResult: Int,
            val checks: List<Check>?
    )

    data class Check(
            val endpoint: String,
            val description: String,
            val errorMessage: String,
            val result: Int
    )

    init {
        DefaultExports.initialize()
    }

    @GetMapping("/selftest")
    fun selftest(): SelftestResult {
        logger.debug("selftest passed")
        return SelftestResult(aggregateResult = 0, checks = null)
    }

    @GetMapping("/isalive")
    fun isalive(): ResponseEntity<String> {
        return ResponseEntity.ok("Is alive")
    }

    @GetMapping("/isready")
    fun isready(): ResponseEntity<String> {
        return ResponseEntity.ok("Is ready")
    }

    @GetMapping("/metrics")
    fun metrics(@RequestParam(name = "name[]", required = false) nameParams: Array<String>?): ResponseEntity<String> {

        fun arrayToSet(nameParams: Array<String>?): Set<String> = if (nameParams == null) emptySet() else HashSet(Arrays.asList(*nameParams))

        val body = StringWriter()
        body.use {
            TextFormat.write004(body, registry.filteredMetricFamilySamples(arrayToSet(nameParams)))
        }

        val headers = HttpHeaders()
        headers.add(HttpHeaders.CONTENT_TYPE, TextFormat.CONTENT_TYPE_004)

        return ResponseEntity(body.toString(), headers, HttpStatus.OK)
    }
}