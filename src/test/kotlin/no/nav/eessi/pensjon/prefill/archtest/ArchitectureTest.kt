package no.nav.eessi.pensjon.prefill.archtest

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaAnnotation
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import no.nav.eessi.pensjon.EessiFagmodulApplication
import no.nav.eessi.pensjon.metrics.MetricsHelper
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Scope
import org.springframework.web.bind.annotation.RestController


class ArchitectureTest {

    companion object {

        @JvmStatic
        private val root = EessiFagmodulApplication::class.qualifiedName!!.replace("." + EessiFagmodulApplication::class.simpleName, "")

        @JvmStatic
        lateinit var allClasses: JavaClasses

        @JvmStatic
        lateinit var productionClasses: JavaClasses

        @JvmStatic
        lateinit var testClasses: JavaClasses

        @BeforeAll @JvmStatic
        fun `extract classes`() {
            allClasses = ClassFileImporter().importPackages(root)

            assertTrue(allClasses.size > 250, "Sanity check on no. of classes to analyze")
            assertTrue(allClasses.size < 1100, "Sanity check on no. of classes to analyze")

            productionClasses = ClassFileImporter()
                .withImportOption(ImportOption.DoNotIncludeTests())
                .withImportOption(ImportOption.DoNotIncludeJars()) //eux skaper problemer, vurderer om denne skal fjernes på et senere tidspunkt
                .importPackages(root)

            println("Validating size of productionClass, currently: ${productionClasses.size}")
            assertTrue(productionClasses.size > 100, "Sanity check on no. of classes to analyze")
            assertTrue(productionClasses.size < 850, "Sanity check on no. of classes to analyze")

            testClasses = ClassFileImporter()
                .withImportOption{ !ImportOption.DoNotIncludeTests().includes(it) }
                .importPackages(root)

            assertTrue(testClasses.size > 100, "Sanity check on no. of classes to analyze")
            assertTrue(testClasses.size < 600, "Sanity check on no. of classes to analyze")
        }
    }

    @Test
    fun `check architecture in detail`() {

        // components
        val health = "health"
        val bucSedApi = "prefill.api"
        val prefill = "prefill"
        val models = "prefill.models"
        val config = "config"
        val kodeverkService = "services.kodeverk"
        val geoService = "services.geo"
        val pensjonService = "services.pensjon"
        val integrationtest = "integrationtest"
        val utils = "utils"
        val vedlegg = "vedlegg"
        val security = "security"
        val personService = "personoppslag.personv3"
        val personDataLosning = "personoppslag.pdl"
        val innhentingService = "fagmodul.prefill"

        val packages: Map<String, String> = mapOf(
            "$root.health.." to health,
            "$root.prefill.api.." to bucSedApi,
            "$root.prefill.." to prefill,
            "$root.prefill.models.." to models,
            "$root.fagmodul.config.." to config,
            "$root.config.." to config,
            "$root.services.kodeverk" to kodeverkService,
            "$root.services.geo" to geoService,
            "$root.services.pensjonsinformasjon" to pensjonService,
            "$root.security.." to security,
            "$root.integrationtest.." to integrationtest,
            "$root.metrics.." to utils,
            "$root.utils.." to utils,
            "$root.logging.." to utils,
            "$root.vedlegg.." to vedlegg,
            "$root.personoppslag.pdl" to personDataLosning,
            "$root.personoppslag.personv3" to personService,
            "$root.fagmodul.prefill.." to innhentingService
        )

        // packages in each component - default is the package with the component name
        fun packagesFor(layer: String) = packages.entries.filter { it.value == layer }.map { it.key }.toTypedArray()

        // mentally replace the word "layer" with "component":
        layeredArchitecture()
            .layer(health).definedBy(*packagesFor(health))

            .layer(bucSedApi).definedBy(*packagesFor(bucSedApi))
            .layer(prefill).definedBy(*packagesFor(prefill))
            .layer(models).definedBy(*packagesFor(models))
            .layer(personDataLosning).definedBy(*packagesFor(personDataLosning))
            .layer(personService).definedBy(*packagesFor(personService))
            .layer(kodeverkService).definedBy(*packagesFor(kodeverkService))
            .layer(geoService).definedBy(*packagesFor(geoService))
            .layer(pensjonService).definedBy(*packagesFor(pensjonService))
            .layer(security).definedBy(*packagesFor(security))
            .layer(config).definedBy(*packagesFor(config))
            .layer(utils).definedBy(*packagesFor(utils))
            .layer(integrationtest).definedBy(*packagesFor(integrationtest))
            .layer(vedlegg).definedBy(*packagesFor(vedlegg))
            .layer(innhentingService).definedBy(*packagesFor(innhentingService))

            .whereLayer(health).mayNotBeAccessedByAnyLayer()
            .whereLayer(bucSedApi).mayNotBeAccessedByAnyLayer()
            .whereLayer(prefill).mayOnlyBeAccessedByLayers(bucSedApi, integrationtest)
            .whereLayer(models).mayOnlyBeAccessedByLayers(prefill, bucSedApi, integrationtest)
            .whereLayer(personDataLosning).mayOnlyBeAccessedByLayers(config, health, bucSedApi, prefill, models, integrationtest, innhentingService)
            .whereLayer(vedlegg).mayOnlyBeAccessedByLayers(integrationtest, innhentingService, bucSedApi)
            .whereLayer(geoService).mayOnlyBeAccessedByLayers(prefill)
            .whereLayer(pensjonService).mayOnlyBeAccessedByLayers(health, prefill, bucSedApi, integrationtest)

            .whereLayer(config).mayOnlyBeAccessedByLayers(personDataLosning)
            .whereLayer(security).mayOnlyBeAccessedByLayers(config, health, vedlegg, pensjonService, personDataLosning, personService, kodeverkService, integrationtest)
            .withOptionalLayers(true)
            .check(productionClasses)
    }

    @Test
    fun `main layers check`() {
        val frontendAPI = "Frontend API"
        val prefillCore = "Prefill"
        val integrationtest = "Integration tests"
        val services = "Services"
        val support = "Support"
        val vedlegg ="Vedlegg"
        val personoppslag = "Personoppslag"
        val euxmodel = "euxmodel"
        val statistikk = "statistikk"
        layeredArchitecture()
            .layer(frontendAPI).definedBy("$root.api..")
            .layer(prefillCore).definedBy("$root.prefill..")
            .layer(integrationtest).definedBy("$root.integrationtest..")
            .layer(services).definedBy("$root.services..")
            .layer(personoppslag).definedBy("$root.personoppslag..")
            .layer(vedlegg).definedBy("$root.vedlegg..")
            .layer(euxmodel).definedBy("$root.eux.model..")
            .layer(statistikk).definedBy("$root.statistikk..")
            .layer(support).definedBy(
                "$root.metrics..",
                "$root.security..",
                "$root.config..",
                "$root.logging..",
                "$root.utils.."
            )
            .whereLayer(frontendAPI).mayNotBeAccessedByAnyLayer()
            .whereLayer(prefillCore).mayOnlyBeAccessedByLayers(
                frontendAPI,
                integrationtest,
                services)
            .whereLayer(services).mayOnlyBeAccessedByLayers(
                frontendAPI,
                prefillCore,
                integrationtest)
            .whereLayer(support).mayOnlyBeAccessedByLayers(
                frontendAPI,
                prefillCore,
                services,
                personoppslag,
                vedlegg,
                integrationtest,
                euxmodel,
                statistikk)
            .withOptionalLayers(true)
            .check(productionClasses)
    }

    @Test
    fun `prefill structure test`() {
        val prefillRoot = "$root.prefill"

        println("root->$prefillRoot")

        layeredArchitecture()
            .layer("prefill").definedBy("$prefillRoot..")
            .layer("sed").definedBy("$prefillRoot.sed..")
            .layer("person").definedBy("$prefillRoot.person..")
            .layer("models").definedBy("$prefillRoot.models..")
            .whereLayer("sed").mayOnlyBeAccessedByLayers("prefill")
            .whereLayer("person").mayOnlyBeAccessedByLayers("sed", "prefill")
            .whereLayer("models").mayOnlyBeAccessedByLayers("sed", "person", "prefill")
            .check(productionClasses)
    }

    @Test
    fun `no cycles on any level for production classes`() {
        slices()
            .matching("$root..(*)")
            .should().beFreeOfCycles()
            .check(productionClasses)
    }

    @Test
    fun `controllers should have RestController-annotation`() {
        classes().that()
            .haveSimpleNameEndingWith("Controller")
            .should().beAnnotatedWith(RestController::class.java)
            .check(allClasses)
    }

    @Test
    fun `Restcontrollers should not call each other`() {
        classes()
            .that().areAnnotatedWith(RestController::class.java)
            .should().onlyHaveDependentClassesThat().areNotAnnotatedWith(RestController::class.java)
            .check(allClasses)
    }

    @Test
    fun `tests should assert, not log`() {
        noClasses().that().haveNameNotMatching(".*\\.logging\\..*") // we allow using slf4j in the logging-package
            .should().dependOnClassesThat().resideInAPackage("org.slf4j..")
            .because("Test should assert, not log; after you made your test the logs will not be checked")
            .check(testClasses)
    }

    @Test
    fun `Spring singleton components should not have mutable instance fields`() {

        class SpringStereotypeAnnotation:DescribedPredicate<JavaAnnotation<*>>("Spring component annotation") {
            override fun apply(input: JavaAnnotation<*>?) = input != null &&
                    (input.rawType.packageName.startsWith("org.springframework.stereotype") ||
                            input.rawType.isEquivalentTo(RestController::class.java))
        }

        val springStereotype = SpringStereotypeAnnotation()

        noMethods().that()
            .haveNameMatching("set[A-Z]+.*")
            .and().doNotHaveRawParameterTypes(MetricsHelper.Metric::class.java)
            .and().areDeclaredInClassesThat().areNotAnnotatedWith(Scope::class.java) // If scope is not singleton it might be ok
            .and().areDeclaredInClassesThat().haveNameNotMatching(".*(STSService|Template|Config)") // these use setter injection
            .should().beDeclaredInClassesThat().areAnnotatedWith(springStereotype)
            .because("Spring-components (usually singletons) must not have mutable instance fields " +
                    "as they can easily be misused and create 'race conditions'")
            .check(productionClasses)

        noFields().that()
            .areNotFinal()
            .and().doNotHaveRawType(MetricsHelper.Metric::class.java)
            .and().areDeclaredInClassesThat().areNotAnnotatedWith(Scope::class.java)// If scope is not singleton it might be ok
            .and().areDeclaredInClassesThat().haveNameNotMatching(".*(STSService|Template|Config)") // these use setter injection
            .should().beDeclaredInClassesThat().areAnnotatedWith(springStereotype)
            .because("Spring-components (usually singletons) must not have mutable instance fields " +
                    "as they can easily be misused and create 'race conditions'")
            .check(productionClasses)
    }

    @Test
    fun `No test classes should use inheritance`() {
        class TestSupportClasses:DescribedPredicate<JavaClass>("test support classes") {
            override fun apply(input: JavaClass?) = input != null &&
                    (!input.simpleName.endsWith("Test") &&
                            (!input.simpleName.endsWith("Tests")
                                    && input.name != "java.lang.Object"))
        }

        noClasses().that().haveSimpleNameEndingWith("Test").or().haveSimpleNameEndingWith("Tests")
            .should().beAssignableTo(TestSupportClasses())
            .because("it is hard to understand the logic of tests that inherit from other classes.")
            .check(testClasses)
    }
}
