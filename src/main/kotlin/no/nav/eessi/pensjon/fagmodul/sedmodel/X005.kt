package no.nav.eessi.pensjon.fagmodul.sedmodel

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.eessi.pensjon.fagmodul.models.SEDType

class X005(
    @JsonProperty("sed")
    override var type: SEDType = SEDType.X005,
    override val sedGVer: String? = "4",
    override var sedVer: String? = "1",
    override var nav: Nav? = null,
) : SED(type, sedGVer, sedVer, nav)