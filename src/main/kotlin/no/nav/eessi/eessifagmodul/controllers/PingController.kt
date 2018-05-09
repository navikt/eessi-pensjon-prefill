package no.nav.eessi.eessifagmodul.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Deprecated("utgår flyttet til InternalController")
@CrossOrigin
@RestController
@RequestMapping("/ping")
class PingController {

    @GetMapping("/")
    fun getPing(): ResponseEntity<String> {
        return ResponseEntity.ok("")
    }

}

