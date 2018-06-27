package no.nav.eessi.eessifagmodul.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.eessi.eessifagmodul.config.jaxws.client.AktoerIdClient
import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.services.EuxService
import no.nav.freg.security.oidc.common.OidcTokenAuthentication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/experiments")
class ExperimentController {

    val objectMapper = jacksonObjectMapper()

    @Autowired
    lateinit var aktoerIdClient: AktoerIdClient

    @Autowired
    lateinit var restTemplate: RestTemplate

    @Value("\${eessibasis.url}")
    lateinit var eessiBasisUrl: String

    @Autowired
    lateinit var euxService: EuxService

    @GetMapping("/testEuxOidc")
    fun testEuxOidc(): ResponseEntity<String> {
        val httpHeaders = HttpHeaders()
        httpHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer ")
        val requestEntity = RequestEntity<String>(httpHeaders, HttpMethod.GET, URI("$eessiBasisUrl/sample"))

        try {
            return restTemplate.exchange(requestEntity, String::class.java)
        } catch (ex: Exception) {
            ex.printStackTrace()
            println("message: ${ex.message}")
            throw ex
        }
    }

    @GetMapping("/testAktoer/{ident}") //, authentication: OAuth2AuthenticationToken
    fun testAktoer(@PathVariable("ident") ident: String): String? {
        val auth = SecurityContextHolder.getContext().authentication as OidcTokenAuthentication
//        val oidcUser = authentication.principal as OidcUser
        return aktoerIdClient.hentAktoerIdForIdent(ident, auth.idToken)?.aktoerId
    }


    @GetMapping("/opprett")
    fun createCaseAndDocument(): String? {
        val fagSaknr = "SAK-123456"
        val mottaker = "NO"
        val pensjon = genererMockData()
        val pensjonAsJson = objectMapper.writeValueAsString(pensjon)
        val bucType = "P6000"
        val korrid = UUID.randomUUID()
        val vedleggType = ""

        try {
            val data = euxService.createCaseAndDocument(pensjonAsJson, bucType, fagSaknr, mottaker, vedleggType, korrid.toString())
            println("Response: $data")
            println("Skal komme hit!!")
            return data
        } catch (ex: Exception) {
            println("Skal _IKKE_ komme hit!!")
            throw RuntimeException(ex.message)
        }
    }
}

fun genererMockData(): Pensjon {
    return PensjonMock().genererMockData()
//    return Pensjon(
//            kjoeringsdato = LocalDate.now().toString(),
//            sak = Sak(
//                    type = "AP",
//                    artikkel44 = "Artikkel44",
//                    kravtyper = listOf(
//                            Kravtype(datoFirst = LocalDate.now().toString()),
//                            Kravtype(datoFirst = LocalDate.now().toString())
//                    )
//            ),
//            vedtak = Vedtak(
//                    basertPaa = "A",
//                    avslag = Avslag(begrunnelse = "Ikke godkjent opphold", begrunnelseAnnen = "Ingen kake"),
//                    beregninger = listOf(
//                            Beregning(
//                                    artikkel = "Beregning artikkel abcdØåæ",
//                                    virkningsdato = LocalDate.now().toString(),
//                                    periode = Periode(LocalDate.now().toString(), LocalDate.now().toString()),
//                                    beloepNetto = BeloepNetto(beloep = "5432.50"),
//                                    beloepBrutto = BeloepBrutto(beloep = "9542.50", ytelseskomponentGrunnpensjon = "1240.50", ytelseskomponentTilleggspensjon = "3292.20", ytelseskomponentAnnen = "194.50"),
//                                    valuta = "SE",
//                                    utbetalingshyppighet = "1",
//                                    utbetalingshyppighetAnnen = "Annet")
//                            ,
//                            Beregning(
//                                    artikkel = "Beregning artikkel efghÆåø",
//                                    virkningsdato = LocalDate.now().toString(),
//                                    periode = Periode(LocalDate.now().toString(), LocalDate.now().toString()),
//                                    beloepNetto = BeloepNetto(beloep = "3432.50"),
//                                    beloepBrutto = BeloepBrutto(beloep = "9542.50", ytelseskomponentGrunnpensjon = "1240.50", ytelseskomponentTilleggspensjon = "3292.20", ytelseskomponentAnnen = "194.50"),
//                                    valuta = "EU",
//                                    utbetalingshyppighet = "3",
//                                    utbetalingshyppighetAnnen = "Skuddår")
//                    ),
//                    grunnlag = Grunnlag(meldlemskap = "Norge", opptjening = Opptjening(forsikredeAnnen = "Anders And"), framtidigtrygdetid = "Nåtid"),
//                    opphor = Opphor(
//                            verdi = "1000.50",
//                            begrunnelse = "Begrunnelse",
//                            annulleringdato = LocalDate.now().toString(),
//                            dato = LocalDate.now().toString(),
//                            utbetaling = Utbetaling(
//                                    beloepBrutto = "10000",
//                                    valuta = "NOK"
//                            )
//                    ),
//                    reduksjoner = listOf(
//                            Reduksjon(
//                                    type = "1",
//                                    arsak = Arsak(inntekt = "212323.50", inntektAnnen = "23223.50"),
//                                    artikkeltype = "2")
//                    ),
//                    dato = LocalDate.now().toString(),
//                    artikkel48 = "Vedtakk artikkel48"
//            )
//    )
}