package no.nav.eessi.pensjon.fagmodul.archtest

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.library.Architectures.layeredArchitecture
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
        lateinit var classesToAnalyze: JavaClasses

        @BeforeClass @JvmStatic
        fun `extract classes`() {
            classesToAnalyze = ClassFileImporter().importPackages(root)

            assertTrue("Sanity check on no. of classes to analyze", classesToAnalyze.size > 200)
            assertTrue("Sanity check on no. of classes to analyze", classesToAnalyze.size < 800)
        }
    }

    @Test
    fun `check architecture`() {

        // components
        val health = "fagmodul.health"
        val bucSedApi = "fagmodul.bucSedApi"
        val helper = "fagmodul.helper"
        val prefill = "fagmodul.prefill"
        val models = "fagmodul.models"
        val arkivApi = "api.arkiv"
        val geoApi = "api.geo"
        val personApi = "api.person"
        val pensjonApi = "api.pensjon"
        val pensjonUtlandApi = "api.pensjonUtland"
        val config = "fagmodul.config"
        val metrics = "fagmodul.metrics"
        val aktoerregisterService = "services.aktoerregister"
        val euxService = "services.eux"
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

                "$root.fagmodul.controllers.." to bucSedApi,

                "$root.fagmodul.prefill.." to prefill,
                "$root.fagmodul.services" to prefill,
                "$root.fagmodul.models.." to models,
                "$root.fagmodul.services.eux.." to euxService,
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
                .whereLayer(health).mayNotBeAccessedByAnyLayer()

                .layer(bucSedApi).definedBy(*packagesFor(bucSedApi))
                .whereLayer(bucSedApi).mayNotBeAccessedByAnyLayer()

                .layer(pensjonUtlandApi).definedBy(*packagesFor(pensjonUtlandApi))
                .whereLayer(pensjonUtlandApi).mayNotBeAccessedByAnyLayer()

                .layer(prefill).definedBy(*packagesFor(prefill))
                .whereLayer(prefill).mayOnlyBeAccessedByLayers(bucSedApi)

                .layer(euxService).definedBy(*packagesFor(euxService))
                .whereLayer(euxService).mayOnlyBeAccessedByLayers(health, bucSedApi)

                .layer(models).definedBy(*packagesFor(models))
                .whereLayer(models).mayOnlyBeAccessedByLayers(prefill, /* TODO consider this list */ euxService, pensjonUtlandApi, bucSedApi)

                .layer(arkivApi).definedBy(*packagesFor(arkivApi))
                .whereLayer(arkivApi).mayNotBeAccessedByAnyLayer()

                .layer(geoApi).definedBy(*packagesFor(geoApi))
                .whereLayer(geoApi).mayNotBeAccessedByAnyLayer()

                .layer(personApi).definedBy(*packagesFor(personApi))
                .whereLayer(personApi).mayNotBeAccessedByAnyLayer()

                .layer(pensjonApi).definedBy(*packagesFor(pensjonApi))
                .whereLayer(pensjonApi).mayNotBeAccessedByAnyLayer()

                .layer(helper).definedBy(*packagesFor(helper)) /** TODO This layer should be removed */
                .whereLayer(helper).mayOnlyBeAccessedByLayers(bucSedApi, pensjonApi, /* TODO consider this */ prefill)

                .layer(config).definedBy(*packagesFor(config))
                .whereLayer(config).mayNotBeAccessedByAnyLayer()

                .layer(metrics).definedBy(*packagesFor(metrics))
                .whereLayer(metrics).mayOnlyBeAccessedByLayers(health, euxService, pensjonUtlandApi)

                .layer(aktoerregisterService).definedBy(*packagesFor(aktoerregisterService))
                .whereLayer(aktoerregisterService).mayOnlyBeAccessedByLayers(personApi, helper)

                .layer(arkivService).definedBy(*packagesFor(arkivService))
                .whereLayer(arkivService).mayOnlyBeAccessedByLayers(arkivApi, bucSedApi, /* TODO consider this */ euxService)

                .layer(geoService).definedBy(*packagesFor(geoService))
                .whereLayer(geoService).mayOnlyBeAccessedByLayers(geoApi, pensjonUtlandApi, prefill)

                .layer(personService).definedBy(*packagesFor(personService))
                .whereLayer(personService).mayOnlyBeAccessedByLayers(health, personApi, prefill)

                .layer(pensjonService).definedBy(*packagesFor(pensjonService))
                .whereLayer(pensjonService).mayOnlyBeAccessedByLayers(health, pensjonApi, prefill)

                .layer(security).definedBy(*packagesFor(security))
                .whereLayer(security).mayOnlyBeAccessedByLayers(health, euxService, aktoerregisterService, arkivService, pensjonService, personService)

                .layer(utils).definedBy(*packagesFor(utils))

                .check(classesToAnalyze)
    }

    @Test
    fun `controllers should have RestController-annotation`() {
        classes().that()
                .haveSimpleNameEndingWith("Controller")
                .should().beAnnotatedWith(RestController::class.java)
                .check(classesToAnalyze)
    }

    @Test
    fun `controllers should not call each other`() {
        classes().that()
                .areAnnotatedWith(RestController::class.java)
                .should().onlyBeAccessed().byClassesThat().areNotAnnotatedWith(RestController::class.java)
                .check(classesToAnalyze)
    }
}