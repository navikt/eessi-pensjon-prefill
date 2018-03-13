package no.nav.eessi.eessifagmodul.controller;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@CrossOrigin
@RestController
@RequestMapping("/internal")
public class InternalController {

    private static Logger log = LoggerFactory.getLogger(InternalController.class);
    private CollectorRegistry registry;

    public InternalController() {
        registry = CollectorRegistry.defaultRegistry;
    }

    @RequestMapping("/selftest")
    public ResponseEntity selftest() {
        return ResponseEntity.ok("Passed");
    }

    @RequestMapping("/isalive")
    public ResponseEntity isAlive() {
        return ResponseEntity.ok("Is alive");
    }

    @RequestMapping("/isready")
    public ResponseEntity isReady() {
        return ResponseEntity.ok("Is ready");
    }

    @RequestMapping("/ping")
    public ResponseEntity ping() {
        return ResponseEntity.ok("pong");
    }

    @RequestMapping(path = "/metrics", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> metrics(@RequestParam(name = "name[]", required = false) String[] nameParams) throws IOException {

        log.debug("RequestParams: {}", Arrays.toString(nameParams));
        String body;
        try (Writer writer = new StringWriter()) {
            TextFormat.write004(writer, registry.filteredMetricFamilySamples(parse(nameParams)));
            body = writer.toString();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, TextFormat.CONTENT_TYPE_004);

        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }

    private Set<String> parse(@RequestParam(name = "name[]", required = false) String[] nameParams) {
        if (nameParams == null)
            return Collections.emptySet();
        return new HashSet<>(Arrays.asList(nameParams));
    }
}
