package no.nav.eessi.eessifagmodul.controllers

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import no.nav.eessi.eessifagmodul.services.AktoerIdClient
import no.nav.eessi.eessifagmodul.services.EESSIKomponentenService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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

    @Autowired
    lateinit var aktoerIdClient: AktoerIdClient

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

    @GetMapping("/ping")
    fun getPing(): ResponseEntity<String> {
        return ResponseEntity.ok("")
    }


    @GetMapping("/selftest")
    fun selftest(): SelftestResult {
        aktoerIdClient.ping()
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
    fun metrics(@PathVariable(name = "name[]", required = false) nameParams: Array<String>?): ResponseEntity<String> {

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