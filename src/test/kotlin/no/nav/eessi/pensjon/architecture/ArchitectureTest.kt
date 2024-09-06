package no.nav.eessi.pensjon.architecture

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
import no.nav.eessi.pensjon.EessiPrefillApplication
import no.nav.eessi.pensjon.metrics.MetricsHelper
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Scope
import org.springframework.web.bind.annotation.RestController

class ArchitectureTest {

    companion object {

        @JvmStatic
        private val root = EessiPrefillApplication::class.qualifiedName!!.replace("." + EessiPrefillApplication::class.simpleName, "")

        @JvmStatic
        lateinit var allClasses: JavaClasses

        @JvmStatic
        lateinit var productionClasses: JavaClasses

        @JvmStatic
        lateinit var testClasses: JavaClasses

        @BeforeAll @JvmStatic
        fun `extract classes`() {
            allClasses = ClassFileImporter().importPackages(root)

            productionClasses = ClassFileImporter()
                .withImportOption(ImportOption.DoNotIncludeTests())
                .withImportOption(ImportOption.DoNotIncludeJars()) //eux skaper problemer, vurderer om denne skal fjernes på et senere tidspunkt
                .importPackages(root)

            assertTrue(productionClasses.size in 90..850, "Sanity check on no. of classes to analyze (is ${productionClasses.size})")

            testClasses = ClassFileImporter()
                .withImportOption{ !ImportOption.DoNotIncludeTests().includes(it) }
                .importPackages(root)

            assertTrue(testClasses.size in 100..600, "Sanity check on no. of classes to analyze (is ${testClasses.size})")
        }
    }

    @Test
    fun `check architecture in detail`() {

        val config = "config"
        val health = "health"
        val prefill = "prefill"
        val models = "prefill.models"
        val utils = "utils"
        val statistikk = "statistikk"

        val packages: Map<String, String> = mapOf(
            "$root.shared.api.health.." to health,
            "$root.prefill.." to prefill,
            "$root.prefill.models.." to models,
            "$root.config.." to config,
            "$root.statistikk.." to statistikk,
            "$root.utils.." to utils
        )

        // packages in each component - default is the package with the component name
        fun packagesFor(layer: String) = packages.entries.filter { it.value == layer }.map { it.key }.toTypedArray()

        // mentally replace the word "layer" with "component":
        layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage(root)
            .layer(health).definedBy(*packagesFor(health))

            .layer(prefill).definedBy(*packagesFor(prefill))
            .layer(models).definedBy(*packagesFor(models))
            .layer(config).definedBy(*packagesFor(config))
            .layer(utils).definedBy(*packagesFor(utils))

            .whereLayer(health).mayNotBeAccessedByAnyLayer()
            .whereLayer(models).mayOnlyBeAccessedByLayers(prefill)

            .whereLayer(config).mayNotBeAccessedByAnyLayer()
            .withOptionalLayers(false)
            .check(productionClasses)
    }

    @Test
    fun `main layers check`() {
        val prefillCore = "Prefill"
        val support = "Support"
        val statistikk = "statistikk"
        layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage(root)
            .layer(prefillCore).definedBy("$root.prefill..")
            .layer(statistikk).definedBy("$root.statistikk..")
            .layer(support).definedBy(
                "$root.config..",
                "$root.utils.."
            )
            .withOptionalLayers(false)
            .check(productionClasses)
    }

    @Test
    fun `prefill structure test`() {
        val prefillRoot = "$root.prefill"

        println("root->$prefillRoot")

        layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage(root)
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
    fun `Spring singleton components should not have mutable instance fields`() {

        class SpringStereotypeAnnotation:DescribedPredicate<JavaAnnotation<*>>("Spring component annotation") {
            override fun test(input: JavaAnnotation<*>?) = input != null &&
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
            override fun test(input: JavaClass?) = input != null &&
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
