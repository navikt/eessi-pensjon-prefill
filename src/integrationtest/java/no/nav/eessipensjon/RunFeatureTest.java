package no.nav.eessipensjon;

import cucumber.api.CucumberOptions;
import cucumber.api.SnippetType;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features="src/integrationTest/resources/features",
        glue="no/nav/eessipensjon/steps",
        format={"pretty", "html:out/test/cucumber"},
        tags = {"~@Ignore", "~@WIP"},
        snippets=SnippetType.UNDERSCORE,
        strict=true)
public class RunFeatureTest {}