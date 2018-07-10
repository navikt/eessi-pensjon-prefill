package no.nav.eessi.eessifagmodul.models


data class Sector(
        val name: String? = null,
        val description: String? = null,
        val activate: Boolean? = null,
        val buc: List<BUC>? = null
)

// BUC class man data between fagmodul and frontend.
// Business Use Case (buc)
data class BUC(
    val bucType: String? = null,
    val sed: List<SED>? = null,
    val description: String? = null
)

// SED class main request class to basis
data class SED (
        var nav: Nav? = null,
        val sed: String? = null,
        val sedGVer: String? = null,
        val sedVer: String? = null,
        var pensjon: Pensjon? = null,
        val ignore: Ignore? = null
        //@JsonProperty("Sector Components/Pensions/P6000 ")
        //val sector: String? = null,
)

fun createSED(sedName: String): SED {
    return SED (
        sed = sedName,
        sedVer = "0",
        sedGVer = "4"
    )
}

//mulig utfylling bruk av sector/buc/sed
//ingen henting fra EUX Basis...
fun createSectorPensjon() {
    val sector = Sector(
            name = "Pensjon",
            description = "Pensjon",
            activate = true,
            buc = createPensjonBucList()
    )
}

// genereate list of buc with seds only for penstion use
fun createPensjonBucList(): List<BUC> {

    return listOf(
        BUC(
                bucType = "P_BUC_01",
                description = "Krav om alderspensjon",
                sed = listOf(
                    createSED("P2000")
                )
        ),
        BUC(
                bucType = "P_BUC_02",
                description = "Krav om etterlattepensjon",
                sed = listOf(
                    createSED("P2100")
                )
        ),
        BUC(
                bucType = "P_BUC_03",
                description = "Krav om etterlattepensjon",
                sed = listOf(
                    createSED("P2200")
                )
        ),
        BUC(
            bucType = "P_BUC_04",
            description = "Anmodning om opplysninger om perioder med omsorg for barn",
            sed = listOf(
                createSED("P1000")
            )
        ),
        BUC(
            bucType = "P_BUC_05",
            description = "Adhoc anmodning om pensjonsopplysninger",
            sed = listOf(
                    createSED("P8000")
            )
        ),
        BUC(
            bucType = "P_BUC_06",
            description = "Notification of Pension Information",
            sed = listOf(
                createSED("P10000"),
                createSED("P6000"),
                createSED("P5000"),
                createSED("P7000")
            )
        ),
        BUC(
            bucType = "P_BUC_07",
            description = "Anmodning om pensjonsbeløp for å fastsette tillegg",
            sed = listOf(
                createSED("P11000")
            )
        ),
        BUC(
            bucType = "P_BUC_08",
            description = "Informasjon om pensjonsbeløp for å gi pensjonstillegg",
            sed = listOf(
                createSED("P120000")
            )
        ),
        BUC(
            bucType = "P_BUC_09",
            description = "Endring i personlige forhold",
            sed = listOf(
                createSED("P140000")
            )
        ),
        BUC(
            bucType = "P_BUC_10",
            description = "Transitional cases",
            sed = listOf(
                createSED("P150000")
            )
        )
    )
}
