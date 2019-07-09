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
        private val root = EessiFagmodulApplication::class.qualifiedName!!
                .replace("." + EessiFagmodulApplication::class.simpleName, "") +
                ".fagmodul" // TODO Fix

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
        val health = "health"
        val coreDomain = "core"
        val subdomainPensjon = "pensjon"
        val subdomainPerson = "person"
        val subdomainArkiv = "arkiv"
        val subdomainGeo = "geo"
        val config = "config"
        val security = "security"
        val utils = "utils"

        // packages in each component - default is the package with the component name
        val packages: Map<String, Array<String>> = mapOf(
                coreDomain to arrayOf(/* TODO there is too much stuff here */
                        "$root.controllers..", "$root.prefill..", "$root.services..", "$root.pesys..", "$root.models.."),
                config to arrayOf("$root.config..", "$root.metrics"),
                utils to arrayOf("$root.utils..", "$root.logging"))
                .withDefault { layer -> arrayOf("$root.$layer..") }

        // mentally replace the word "layer" with "component":

        layeredArchitecture()
                .layer(health).definedBy(*packages.getValue(health))
                .whereLayer(health).mayNotBeAccessedByAnyLayer()

                .layer(coreDomain).definedBy(*packages.getValue(coreDomain))
                .whereLayer(coreDomain).mayOnlyBeAccessedByLayers(health)

                .layer(subdomainPensjon).definedBy(*packages.getValue(subdomainPensjon))
                .whereLayer(subdomainPensjon).mayOnlyBeAccessedByLayers(health, coreDomain)

                .layer(subdomainPerson).definedBy(*packages.getValue(subdomainPerson))
                .whereLayer(subdomainPerson).mayOnlyBeAccessedByLayers(health, coreDomain, /* TODO should not use other subdomain */ subdomainPensjon)

                .layer(subdomainArkiv).definedBy(*packages.getValue(subdomainArkiv))
                .whereLayer(subdomainArkiv).mayOnlyBeAccessedByLayers(health, coreDomain)

                .layer(subdomainGeo).definedBy(*packages.getValue(subdomainGeo))
                .whereLayer(subdomainGeo).mayOnlyBeAccessedByLayers(health, coreDomain)

                .layer(config).definedBy(*packages.getValue(config))
                .whereLayer(config).mayOnlyBeAccessedByLayers(health, coreDomain, subdomainGeo, subdomainArkiv, subdomainPensjon, subdomainPerson)

                .layer(security).definedBy(*packages.getValue(security))
                .whereLayer(security).mayOnlyBeAccessedByLayers(health, coreDomain, subdomainGeo, subdomainArkiv, subdomainPensjon, subdomainPerson)

                .layer(utils).definedBy(*packages.getValue(utils))
                .whereLayer(utils).mayOnlyBeAccessedByLayers(health, coreDomain, subdomainGeo, subdomainArkiv, subdomainPensjon, subdomainPerson, config, security)

                .check(classesToAnalyze)
    }
}