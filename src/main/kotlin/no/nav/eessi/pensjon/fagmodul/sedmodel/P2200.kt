package no.nav.eessi.pensjon.fagmodul.sedmodel

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.eessi.pensjon.fagmodul.models.SEDType

class P2200(
    @JsonProperty("sed")
    override var type: SEDType = SEDType.P2200,
    override val sedGVer: String? = "4",
    override var sedVer: String? = "1",
    override var nav: Nav? = null,
    override var pensjon: Pensjon?
) : SED(type, sedGVer, sedVer, nav, pensjon)