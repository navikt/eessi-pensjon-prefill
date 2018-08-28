package no.nav.eessi.eessifagmodul.models

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
        SedP2000Test::class,
        SedP2200Test::class,
        SedP6000Test::class,
        SedP5000Test::class,
        SedP4000Test::class)
class SedTestSuite
