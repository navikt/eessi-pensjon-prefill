package no.nav.eessi.eessifagmodul.services.pensjonsinformasjon

enum class InformasjonsType : Metadata {
    AVDOD {
        override fun elementName() = "avdod"
        override fun typeName() = "v1.Avdod"
    },
    INNGANG_OG_EXPORT {
        override fun elementName() = "inngangOgEksport"
        override fun typeName() = "v1.InngangOgEksport"
    },
    PERSON {
        override fun elementName() = "person"
        override fun typeName() = "v1.Person"
    },
    SAK {
        override fun elementName() = "sak"
        override fun typeName() = "v1.Sak"
    },
    TRYGDEAVTALE {
        override fun elementName() = "trygdeavtale"
        override fun typeName() = "v1.Trygdeavtale"
    },
    TRYGDETID_AVDOD_FAR_LISTE {
        override fun elementName() = "trygdetidAvdodFarListe"
        override fun typeName() = "v1.TrygdetidAvdodFarListe"
    },
    TRYGDETID_AVDOD_LISTE {
        override fun elementName() = "trygdetidAvdodListe"
        override fun typeName() = "v1.TrygdetidAvdodListe"
    },
    TRYGDETID_AVDOD_MOR_LISTE {
        override fun elementName() = "trygdetidAvdodMorListe"
        override fun typeName() = "v1.TrygdetidAvdodMorListe"
    },
    TRYGDETID_LISTE {
        override fun elementName() = "trygdetidListe"
        override fun typeName() = "v1.TrygdetidListe"
    },
    VEDTAK {
        override fun elementName() = "vedtak"
        override fun typeName() = "v1.Vedtak"
    },
    VILKARSVURDERING_LISTE {
        override fun elementName() = "vilkarsvurderingListe"
        override fun typeName() = "v1.VilkarsvurderingListe"
    },
    YTELSE_PR_MAANED_LISTE {
        override fun elementName() = "ytelsePerMaanedListe"
        override fun typeName() = "v1.YtelsePerMaanedListe"
    }
}

interface Metadata {
    fun elementName(): String
    fun typeName(): String
}