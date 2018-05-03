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
class InternalController(val eessiKomponentenService: EESSIKomponentenService) {

    @Autowired
    lateinit var aktoerIdClient: AktoerIdClient

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
/*
    @RequestMapping("/testmethod")
    fun testMethod() {
        eessiKomponentenService.opprettEUFlyt("K1234", "Testeren", "12345612345")
    }

    @PostMapping("/testmethod")
    fun eessiMock(@RequestBody body: EESSIKomponentenService.OpprettEUFlytRequest) {
        println(body)
    }
*/

    @RequestMapping("/testmethod2")
    fun testMethodBucogSED() {
        eessiKomponentenService.opprettBuCogSED ("1234", "Testeren", "12345612345")
    }

    @PostMapping("/testmethod")
    fun opprettBuCogSEDRequest(@RequestBody body: EESSIKomponentenService.OpprettBuCogSEDRequest) {
        println(body)
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