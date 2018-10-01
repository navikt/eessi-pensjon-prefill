package no.nav.eessi.eessifagmodul.services.pensjonsinformasjon

enum class InformasjonsType : Metadata {
    AVDOD {
        override fun elementName() = "avdod"
        override fun schemaLocation() = "../validation/v1.Avdod.xsd"
        override fun namespace() = "http://nav.no/pensjon/v1/avdod"
        override fun typeName() = "v1.Avdod"
    },
    INNGANG_OG_EXPORT {
        override fun elementName() = "inngangOgEksport"
        override fun schemaLocation() = "../validation/v1.InngangOgEksport.xsd"
        override fun namespace() = "http://nav.no/pensjon/v1/inngangOgEksport"
        override fun typeName() = "v1.InngangOgEksport"
    },
    PERSON {
        override fun elementName() = "person"
        override fun schemaLocation() = "../validation/v1.Person.xsd"
        override fun namespace() = "http://nav.no/pensjon/v1/person"
        override fun typeName() = "v1.Person"
    },
    SAK {
        override fun elementName() = "sak"
        override fun schemaLocation() = "../validation/v1.Sak.xsd"
        override fun namespace() = "http://nav.no/pensjon/v1/sak"
        override fun typeName() = "v1.Sak"
    },
    TRYGDEAVTALE {
        override fun elementName() = "trygdeavtale"
        override fun schemaLocation() = "../validation/v1.Trygdeavtale.xsd"
        override fun namespace() = "http://nav.no/pensjon/v1/trygdeavtale"
        override fun typeName() = "v1.Trygdeavtale"
    },
    TRYGDETID_AVDOD_FAR_LISTE {
        override fun elementName() = "trygdetidAvdodFarListe"
        override fun schemaLocation() = "../validation/v1.TrygdetidAvdodFarListe.xsd"
        override fun namespace() = "http://nav.no/pensjon/v1/trygdetidAvdodFarListe"
        override fun typeName() = "v1.TrygdetidAvdodFarListe"
    },
    TRYGDETID_AVDOD_LISTE  {
        override fun elementName() = "trygdetidAvdodListe"
        override fun schemaLocation() = "../validation/v1.TrygdetidAvdodListe.xsd"
        override fun namespace() = "http://nav.no/pensjon/v1/trygdetidAvdodListe"
        override fun typeName() = "v1.TrygdetidAvdodListe"
    },
    TRYGDETID_AVDOD_MOR_LISTE{
        override fun elementName() = "trygdetidAvdodMorListe"
        override fun schemaLocation() = "../validation/v1.TrygdetidAvdodMorListe.xsd"
        override fun namespace() = "http://nav.no/pensjon/v1/trygdetidAvdodMorListe"
        override fun typeName() = "v1.TrygdetidAvdodMorListe"
    },
    TRYGDETID_LISTE{
        override fun elementName() = "trygdetidListe"
        override fun schemaLocation() = "../validation/v1.TrygdetidListe.xsd"
        override fun namespace() = "http://nav.no/pensjon/v1/trygdetidListe"
        override fun typeName() = "v1.TrygdetidListe"
    },
    VEDTAK {
        override fun elementName() = "vedtak"
        override fun schemaLocation() = "../validation/v1.Vedtak.xsd"
        override fun namespace() = "http://nav.no/pensjon/v1/vedtak"
        override fun typeName() = "v1.Vedtak"
    },
    VILKARSVURDERING_LISTE {
        override fun elementName() = "vilkarsvurderingListe"
        override fun schemaLocation() = "../validation/v1.VilkarsvurderingListe.xsd"
        override fun namespace() = "http://nav.no/pensjon/v1/vilkarsvurderingListe"
        override fun typeName() = "v1.VilkarsvurderingListe"
    },
    YTELSE_PR_MAANED_LISTE {
        override fun elementName() = "ytelsePerMaanedListe"
        override fun schemaLocation() = "../validation/v1.YtelsePerMaanedListe.xsd"
        override fun namespace() = "http://nav.no/pensjon/v1/ytelsePerMaanedListe"
        override fun typeName() = "v1.YtelsePerMaanedListe"
    }
}

interface Metadata {
    fun elementName(): String
    fun schemaLocation(): String
    fun namespace(): String
    fun typeName(): String
}