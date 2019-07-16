package no.nav.eessi.pensjon.fagmodul.archtest

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import no.nav.eessi.pensjon.EessiFagmodulApplication
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
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

        @BeforeClass @JvmStatic
        fun `extract classes`() {
            allClasses = ClassFileImporter().importPackages(root)

            assertTrue("Sanity check on no. of classes to analyze", allClasses.size > 200)
            assertTrue("Sanity check on no. of classes to analyze", allClasses.size < 800)

            productionClasses = ClassFileImporter()
                    .withImportOption(ImportOption.DoNotIncludeTests())
                    .importPackages(root)

            assertTrue("Sanity check on no. of classes to analyze", productionClasses.size > 200)
            assertTrue("Sanity check on no. of classes to analyze", productionClasses.size < 800)

            testClasses = ClassFileImporter()
                    .withImportOption{ !ImportOption.DoNotIncludeTests().includes(it) }
                    .importPackages(root)

            assertTrue("Sanity check on no. of classes to analyze", testClasses.size > 100)
            assertTrue("Sanity check on no. of classes to analyze", testClasses.size < 500)
        }
    }

    @Test
    fun `check architecture in detail`() {

        // components
        val health = "fagmodul.health"
        val bucSedApi = "fagmodul.bucSedApi"
        val helper = "fagmodul.helper"
        val prefill = "fagmodul.prefill"
        val models = "fagmodul.models"
        val sedmodel = "fagmodul.sedmodel"
        val arkivApi = "api.arkiv"
        val geoApi = "api.geo"
        val personApi = "api.person"
        val pensjonApi = "api.pensjon"
        val pensjonUtlandApi = "api.pensjonUtland"
        val config = "fagmodul.config"
        val metrics = "fagmodul.metrics"
        val aktoerregisterService = "services.aktoerregister"
        val euxService = "fagmodul.euxservice"
        val euxBasisModel = "fagmodul.euxBasisModel"
        val euxBucModel = "fagmodul.euxBucModel"
        val arkivService = "services.arkiv"
        val geoService = "services.geo"
        val personService = "services.person"
        val pensjonService = "services.pensjon"
        val security = "security"
        val utils = "utils"

        val packages: Map<String, String> = mapOf(
                "$root.fagmodul.health.." to health,

                "$root.api.arkiv.." to arkivApi,
                "$root.api.geo.." to geoApi,
                "$root.api.person.." to personApi,
                "$root.api.pensjon.." to pensjonApi,

                "$root.fagmodul.api.." to bucSedApi,

                "$root.fagmodul.prefill.." to prefill,
                "$root.fagmodul.models.." to models,
                "$root.fagmodul.sedmodel.." to sedmodel,
                "$root.fagmodul.eux" to euxService,
                "$root.fagmodul.eux.basismodel.." to euxBasisModel,
                "$root.fagmodul.eux.bucmodel.." to euxBucModel,
                "$root.fagmodul.pesys.." to pensjonUtlandApi,

                "$root.fagmodul.config.." to config,
                "$root.fagmodul.metrics.." to metrics,

                "$root.helper.." to helper, /* TODO This should be removed */

                "$root.services.aktoerregister" to aktoerregisterService,
                "$root.services.arkiv" to arkivService,
                "$root.services.geo" to geoService,
                "$root.services.personv3" to personService,
                "$root.services.pensjonsinformasjon" to pensjonService,

                "$root.security.." to security,

                "$root.metrics.." to utils,
                "$root.utils.." to utils,
                "$root.logging.." to utils)

        // packages in each component - default is the package with the component name
        fun packagesFor(layer: String) = packages.entries.filter { it.value == layer }.map { it.key }.toTypedArray()

        // mentally replace the word "layer" with "component":
        layeredArchitecture()
                .layer(health).definedBy(*packagesFor(health))

                .layer(arkivApi).definedBy(*packagesFor(arkivApi))
                .layer(geoApi).definedBy(*packagesFor(geoApi))
                .layer(personApi).definedBy(*packagesFor(personApi))
                .layer(pensjonApi).definedBy(*packagesFor(pensjonApi))

                .layer(pensjonUtlandApi).definedBy(*packagesFor(pensjonUtlandApi))

                .layer(bucSedApi).definedBy(*packagesFor(bucSedApi))
                .layer(prefill).definedBy(*packagesFor(prefill))
                .layer(euxService).definedBy(*packagesFor(euxService))
                .layer(euxBasisModel).definedBy(*packagesFor(euxBasisModel))
                .layer(euxBucModel).definedBy(*packagesFor(euxBucModel))
                .layer(models).definedBy(*packagesFor(models))
                .layer(sedmodel).definedBy(*packagesFor(sedmodel))

                .layer(helper).definedBy(*packagesFor(helper))

                .layer(aktoerregisterService).definedBy(*packagesFor(aktoerregisterService))
                .layer(arkivService).definedBy(*packagesFor(arkivService))
                .layer(geoService).definedBy(*packagesFor(geoService))
                .layer(personService).definedBy(*packagesFor(personService))
                .layer(pensjonService).definedBy(*packagesFor(pensjonService))

                .layer(config).definedBy(*packagesFor(config))
                .layer(metrics).definedBy(*packagesFor(metrics))
                .layer(security).definedBy(*packagesFor(security))
                .layer(utils).definedBy(*packagesFor(utils))

                .whereLayer(health).mayNotBeAccessedByAnyLayer()

                .whereLayer(arkivApi).mayNotBeAccessedByAnyLayer()
                .whereLayer(geoApi).mayNotBeAccessedByAnyLayer()
                .whereLayer(personApi).mayNotBeAccessedByAnyLayer()
                .whereLayer(pensjonApi).mayNotBeAccessedByAnyLayer()

                .whereLayer(pensjonUtlandApi).mayNotBeAccessedByAnyLayer()
                .whereLayer(bucSedApi).mayNotBeAccessedByAnyLayer()
                .whereLayer(prefill).mayOnlyBeAccessedByLayers(bucSedApi)
                .whereLayer(euxService).mayOnlyBeAccessedByLayers(health, bucSedApi)
                .whereLayer(euxBasisModel).mayOnlyBeAccessedByLayers(euxService, bucSedApi)
                .whereLayer(euxBucModel).mayOnlyBeAccessedByLayers(euxService, bucSedApi)
                .whereLayer(euxService).mayOnlyBeAccessedByLayers(health, bucSedApi)
                .whereLayer(models).mayOnlyBeAccessedByLayers(prefill, /* TODO consider this list */ euxService, pensjonUtlandApi, bucSedApi)

                .whereLayer(sedmodel).mayOnlyBeAccessedByLayers(prefill, euxService, pensjonUtlandApi, bucSedApi)

                .whereLayer(helper).mayOnlyBeAccessedByLayers(bucSedApi, pensjonApi)

                .whereLayer(aktoerregisterService).mayOnlyBeAccessedByLayers(personApi, helper)
                .whereLayer(arkivService).mayOnlyBeAccessedByLayers(arkivApi, bucSedApi)
                .whereLayer(geoService).mayOnlyBeAccessedByLayers(geoApi, pensjonUtlandApi, prefill)
                .whereLayer(personService).mayOnlyBeAccessedByLayers(health, personApi, prefill)
                .whereLayer(pensjonService).mayOnlyBeAccessedByLayers(health, pensjonApi, prefill)

                .whereLayer(config).mayNotBeAccessedByAnyLayer()
                .whereLayer(metrics).mayOnlyBeAccessedByLayers(health, euxService, pensjonUtlandApi)
                .whereLayer(security).mayOnlyBeAccessedByLayers(health, euxService, aktoerregisterService, arkivService, pensjonService, personService)

                .check(allClasses)
    }

    @Test
    fun `main layers check`() {
        val frontendAPI = "Frontend API"
        val fagmodulCore = "Fagmodul Core"
        val helper = "Helper"
        val services = "Services"
        val support = "Support"
        layeredArchitecture()
                .layer(frontendAPI).definedBy("$root.api..")
                .layer(fagmodulCore).definedBy("$root.fagmodul..")
                .layer(helper).definedBy("$root.helper..")
                .layer(services).definedBy("$root.services..")
                .layer(support).definedBy(
                        "$root.metrics..",
                        "$root.security..",
                        "$root.logging..",
                        "$root.utils.."
                )
                .whereLayer(frontendAPI).mayNotBeAccessedByAnyLayer()
                .whereLayer(fagmodulCore).mayNotBeAccessedByAnyLayer()
                .whereLayer(helper).mayOnlyBeAccessedByLayers(
                        frontendAPI,
                        fagmodulCore)
                .whereLayer(services).mayOnlyBeAccessedByLayers(
                        frontendAPI,
                        fagmodulCore,
                        helper)
                .whereLayer(support).mayOnlyBeAccessedByLayers(
                        frontendAPI,
                        fagmodulCore,
                        helper,
                        services)
                .check(allClasses)
    }

    @Test
    fun `prefill structure test`() {
        val prefillRoot = "$root.prefill"
        val sedModel = "$root.sedModel"
        val prefillClasses = ClassFileImporter()
                .importPackages(prefillRoot, sedModel)

        layeredArchitecture()
                .layer("service").definedBy(prefillRoot)
                .layer("sed").definedBy("$prefillRoot.sed..")
                .layer("person").definedBy("$prefillRoot.person..")
                .layer("pen").definedBy("$prefillRoot.pen..")
                .layer("tps").definedBy("$prefillRoot.tps..")
                .layer("eessi").definedBy("$prefillRoot.eessi..")
                .layer("model").definedBy("$prefillRoot.model..")
                .whereLayer("service").mayNotBeAccessedByAnyLayer()
                .whereLayer("sed").mayOnlyBeAccessedByLayers("service")
                .whereLayer("person").mayOnlyBeAccessedByLayers("sed")
                .whereLayer("pen").mayOnlyBeAccessedByLayers("sed")
                .whereLayer("tps").mayOnlyBeAccessedByLayers("sed")
                .whereLayer("eessi").mayOnlyBeAccessedByLayers("sed", "tps")
                .check(prefillClasses)
    }
    @Test
    fun `no cycles on top level`() {
        slices()
                .matching("$root.(*)..")
                .should().beFreeOfCycles()
                .check(allClasses)
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
    fun `controllers should not call each other`() {
        classes().that()
                .areAnnotatedWith(RestController::class.java)
                .should().onlyBeAccessed().byClassesThat().areNotAnnotatedWith(RestController::class.java)
                .because("Controllers should not call each other")
                .check(allClasses)
    }

    @Test
    fun `tests should assert, not log`() {
        noClasses()
                .should().dependOnClassesThat().resideInAPackage("org.slf4j..")
                .because("Test should assert, not log; after you made your test the logs will not be checked")
                .check(testClasses)
    }
}

