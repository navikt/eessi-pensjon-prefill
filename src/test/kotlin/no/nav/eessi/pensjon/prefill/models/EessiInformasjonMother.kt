package no.nav.eessi.pensjon.prefill.models

object EessiInformasjonMother {
    fun standardEessiInfo() = EessiInformasjon(
            institutionid = "NO:noinst002",
            institutionnavn = "NOINST002, NO INST002, NO",
            institutionGate = "Postboks 6600 Etterstad TEST",
            institutionBy = "Oslo",
            institutionPostnr = "0607",
            institutionLand = "NO"
    )

}
