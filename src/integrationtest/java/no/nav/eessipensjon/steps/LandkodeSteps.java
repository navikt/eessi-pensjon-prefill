package no.nav.eessipensjon.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import cucumber.api.java.no.Gitt;
import cucumber.api.java.no.Når;
import cucumber.api.java.no.Så;
import no.nav.eessipensjon.IntegrationTestBaseClass;
import org.apache.http.HttpHeaders;
import org.apache.http.client.fluent.Request;
import org.junit.Assert;

import java.util.List;

public class LandkodeSteps extends IntegrationTestBaseClass {

    private String oidcToken;
    private List landkoder;

    @Gitt("^en saksbehandler \"([^\"]*)\"$")
    public void enSaksbehandler(String saksbehandler) throws Throwable {
        oidcToken = getSaksbehandlerToken(saksbehandler);
    }

    @Når("^det bes om landkodelisten$")
    public void detBesOmLandkodeliste() throws Throwable {
        String response = Request
                .Post("http://localhost:" + basePort + "/api/landkoder")
                .setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + oidcToken)
                .execute()
                .returnContent()
                .asString();

        landkoder = new ObjectMapper().readValue(response, List.class);
    }

    @Så("^får vi en liste med (\\d+) landkoder$")
    public void fårViEnListeMedLandkoder(int antall) throws Throwable {
        Assert.assertEquals("Forventer " + antall + " landkoder", landkoder.size(), antall);
    }
}
