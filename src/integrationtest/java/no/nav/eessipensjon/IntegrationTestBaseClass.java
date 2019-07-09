package no.nav.eessipensjon;

import no.nav.eessi.pensjon.fagmodul.EessiFagmodulApplication;
import no.nav.security.oidc.test.support.spring.TokenGeneratorConfiguration;
import org.apache.http.client.fluent.Request;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;

@Import(TokenGeneratorConfiguration.class)
@SpringBootTest(classes = EessiFagmodulApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(name = "application-integrationtest.yml")
@ActiveProfiles("integrationtest")
public abstract class IntegrationTestBaseClass {

    @LocalServerPort
    public int basePort;

    public String getSaksbehandlerToken(String subject) throws IOException {
        String subjectParam;
        if (subject == null) {
            subjectParam = "";
        } else {
            subjectParam = "?subject=" + subject;
        }
        return Request.Get("http://localhost:" + basePort + "/local/jwt" + subjectParam)
                .execute()
                .returnContent()
                .asString();
    }

    public String getBorgerToken() throws IOException {
        return Request.Get("http://localhost:" + basePort + "/local/jwt")
                .execute()
                .returnContent()
                .asString();
    }
}
