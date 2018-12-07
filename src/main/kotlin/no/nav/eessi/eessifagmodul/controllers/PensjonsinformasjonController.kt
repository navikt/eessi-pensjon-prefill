package no.nav.eessi.eessifagmodul.controllers

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.annotations.ApiOperation
import no.nav.eessi.eessifagmodul.utils.createXMLCalendarFromString
import no.nav.eessi.eessifagmodul.utils.mapAnyToJson
import no.nav.pensjon.v1.brukerssakerliste.V1BrukersSakerListe
import no.nav.pensjon.v1.kravhistorikk.V1KravHistorikk
import no.nav.pensjon.v1.kravhistorikkliste.V1KravHistorikkListe
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import no.nav.pensjon.v1.sak.V1Sak
import no.nav.security.oidc.api.Unprotected
import org.springframework.web.bind.annotation.*

//@Protected
@Unprotected
@CrossOrigin
@RestController
@RequestMapping("/pesys")
class PensjonsinformasjonController {

    @ApiOperation(
            httpMethod = "GET",
            value = "Henter ut kravhode fra innkommende SEDER fra EU/EØS. Nødvendig data for å automatisk opprette et krav i Pesys",
            response = Pensjonsinformasjon::class)
    @GetMapping("/kravFraUtland/{sakId)")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    fun hentKravFraUtland(@PathVariable("sakId", required = false) sakId: Long? = 0L): Pensjonsinformasjon {
        val v1sak = V1Sak()
        v1sak.forsteVirkningstidspunkt = createXMLCalendarFromString("2016-01-01")
        v1sak.sakType = "ALDER"
        v1sak.sakId = sakId ?: 0L
        val v1krav = V1KravHistorikk()
        v1krav.kravType
        v1krav.mottattDato = createXMLCalendarFromString("2018-01-01") //pkt.9.1 i P2000
        v1krav.virkningstidspunkt = v1sak.forsteVirkningstidspunkt
        v1sak.kravHistorikkListe = V1KravHistorikkListe().apply {
            kravHistorikkListe.add(v1krav)
        }
        val peninfo = Pensjonsinformasjon()
        peninfo.brukersSakerListe = V1BrukersSakerListe().apply {
            brukersSakerListe.add(v1sak)
        }

        val json = mapAnyToJson(peninfo, true)
        println("-------------------------------------------------------------------------------------")
        println(json)
        println("-------------------------------------------------------------------------------------")

        return peninfo
    }

}