package no.nav.eessi.eessifagmodul.pesys

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.annotations.ApiOperation
import no.nav.eessi.eessifagmodul.models.*
import no.nav.security.oidc.api.Unprotected
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@Unprotected
@CrossOrigin
@RestController
@RequestMapping("/pesys")
class PensjonsinformasjonMottakController {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PensjonsinformasjonMottakController::class.java) }

    //TODO: vil trenge en innhentSedFraRinaService..
    //TODO: vil trenge en navSED->PESYS regel.

    @ApiOperation(httpMethod = "GET",
            value = "Henter ut kravhode fra innkommende SEDER fra EU/EØS. Nødvendig data for å automatisk opprette et krav i Pesys",
            response = KravUtland::class)
    @GetMapping("/hentKravFraUtland/{bucId}")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    fun hentKravFraUtland(@PathVariable("bucId", required = false) buckId: Long? = 0L): KravUtland {

        logger.info("Starter prosess for henting av krav fra utloand (P2000...)")

        val p2000 = SED("P2000")
        p2000.nav = Nav(
                bruker = Bruker(
                        person = Person(
                                pin = listOf(PinItem(
                                        sektor = "Pensjon",
                                        identifikator = "410155012341",
                                        land = "SE"
                                )),
                                statsborgerskap = listOf(
                                        StatsborgerskapItem(
                                                land = "SE"
                                        )
                                )
                        )
                ),
                krav = Krav(dato = "2020-05-30")
        )
        val p3000no = SED("P3000_NO")
        p3000no.pensjon = Pensjon(

                landspesifikk = Landspesifikk(
                        norge = Norge(
                                alderspensjon = Alderspensjon(
                                        pensjonsgrad = "03"
                                )
                        )
                )

        )

        return KravUtland(
                //P2000 9.1
                mottattDato = LocalDate.parse(p2000.nav?.krav?.dato) ?: null,
                //P3000_NO 4.6.1. Forsikredes anmodede prosentdel av full pensjon
                uttaksgrad = parsePensjonsgrad(p3000no.pensjon?.landspesifikk?.norge?.alderspensjon?.pensjonsgrad),
                //P2000 2.2.1.1
                statsborgerskap = StatsborgerskapItem("SE"),
                //hentes fra P4000?
                utlandsopphold = listOf(Utlandsoppholditem(
                        land = "SE",
                        fom = LocalDate.parse("2010-01-01"),
                        tom = LocalDate.parse("2012-01-01"),
                        bodd = true,
                        arbeidet = true,
                        utlandPin = "410155012341"
                ))
        )

    }


    fun parsePensjonsgrad(pensjonsgrad: String?): String {
        return when (pensjonsgrad) {
            "01" -> "20"
            "02" -> "40"
            "03" -> "50"
            "04" -> "60"
            "05" -> "80"
            else -> "100"
        }
    }

}