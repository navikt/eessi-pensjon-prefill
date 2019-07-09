package no.nav.eessi.pensjon.fagmodul.archtest

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import no.nav.eessi.pensjon.EessiFagmodulApplication
import org.junit.BeforeClass
import org.junit.Test

class ArchitectureTest {

    companion object {

        @JvmStatic
        private val root = EessiFagmodulApplication::class.qualifiedName!!.replace("." + EessiFagmodulApplication::class.simpleName, "")

        @JvmStatic
        lateinit var classesToAnalyze: JavaClasses

        @BeforeClass @JvmStatic
        fun `extract classes`() {
            classesToAnalyze = ClassFileImporter().importPackages(root)
        }
    }

    @Test
    fun `check architecture`() {

        // components
        val api = "fagmodul.api"
        val health = "fagmodul.health"
        val core = "fagmodul.core"
        val config = "fagmodul.config"
        val metrics = "fagmodul.metrics"
        val services = "services"
        val security = "security"
        val utils = "utils"

        val packages: Map<String, String> = mapOf(
                "$root.fagmodul.health.." to health,

                "$root.fagmodul.arkiv.." to api,
                "$root.fagmodul.geo.." to api,
                "$root.fagmodul.person.." to api,
                "$root.fagmodul.pensjon.." to api,

                "$root.fagmodul.controllers.." to core,
                "$root.fagmodul.prefill.." to core,
                "$root.fagmodul.services.." to core,
                "$root.fagmodul.pesys.." to core,
                "$root.fagmodul.models.." to core,

                "$root.fagmodul.config.." to config,
                "$root.fagmodul.metrics.." to metrics,

                "$root.services.." to services,

                "$root.security.." to security,

                "$root.metrics.." to utils,
                "$root.utils.." to utils,
                "$root.logging.." to utils)

        // packages in each component - default is the package with the component name
        fun packagesFor(layer: String) = packages.entries.filter { it.value == layer }.map { it.key }.toTypedArray()

        // mentally replace the word "layer" with "component":
        layeredArchitecture()
                .layer(api).definedBy(*packagesFor(api))
                .whereLayer(api).mayNotBeAccessedByAnyLayer()

                .layer(health).definedBy(*packagesFor(health))
                .whereLayer(health).mayNotBeAccessedByAnyLayer()

                .layer(core).definedBy(*packagesFor(core))
                .whereLayer(health).mayNotBeAccessedByAnyLayer()

                .layer(config).definedBy(*packagesFor(config))
                .whereLayer(config).mayNotBeAccessedByAnyLayer()

                .layer(metrics).definedBy(*packagesFor(metrics))
                .whereLayer(metrics).mayOnlyBeAccessedByLayers(health, api, core)

                .layer(services).definedBy(*packagesFor(services))
                .whereLayer(services).mayOnlyBeAccessedByLayers(api, health, core)

                .layer(security).definedBy(*packagesFor(security))
                .whereLayer(security).mayOnlyBeAccessedByLayers(health, services, core)

                .layer(utils).definedBy(*packagesFor(utils))
                .whereLayer(utils).mayOnlyBeAccessedByLayers(api, health, core, metrics, services, security, config)

                .check(classesToAnalyze)
    }
}