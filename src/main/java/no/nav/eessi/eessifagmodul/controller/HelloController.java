package no.nav.eessi.eessifagmodul.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
public class HelloController {

    @RequestMapping()
    public ResponseEntity helloService() {
        return ResponseEntity.ok("Hello from HelloController");
    }


    @RequestMapping("/world")
    public ResponseEntity helloworld() {
        return ResponseEntity.ok("Hello from /hello/world");
    }
}
