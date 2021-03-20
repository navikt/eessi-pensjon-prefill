package no.nav.eessi.pensjon.fagmodul.sedmodel

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.eessi.pensjon.fagmodul.models.SEDType

class P7000(
    @JsonProperty("sed")
    override var type: SEDType = SEDType.P7000,
    override val sedGVer: String? = "4",
    override var sedVer: String? = "1",
    override var nav: Nav? = null,
    @JsonProperty("pensjon")
    val p7000Pensjon: P7000Pensjon
) : SED(type, sedGVer, sedVer, nav)