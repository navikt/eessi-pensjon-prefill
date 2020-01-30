package no.nav.eessi.pensjon.services.kodeverk

import org.springframework.web.client.RestTemplate
import java.nio.file.Files
import java.nio.file.Paths

class KodeverkServiceMock : KodeverkService(RestTemplate(), "DummyTest") {

    override fun hentHierarki(hierarki: String): String {
        val mockJson = "src/test/resources/json/kodeverk/landkoderSammensattIso2.json"
        val mockResponseString = String(Files.readAllBytes(Paths.get(mockJson)))
        return mockResponseString
    }

//    override fun finnLandkode2(alpha3: String): String? {
//        return super.finnLandkode2(alpha3)
//    }




}