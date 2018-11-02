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
    },
    SAKALDER {
        override fun elementName() = "sakAlder"
        override fun typeName() = "v1.SakAlder"
    },
    BRUKER_SAKER_LISTE {
        override fun elementName() = "brukerssakerliste"
        override fun typeName() = "v1.BrukersSakerListe"
    },
    EKTEFELLE_PARTNER_SAMBOER_LISTE {
        override fun elementName() = "ektefellepartnersamboerliste"
        override fun typeName() = "v1.EktefellePartnerSamboerListe"
    },
    KRAV_HISTORIKK_LISTE {
        override fun elementName() = "kravhistorikkliste"
        override fun typeName() = "v1.KravHistorikkListe"
    },
    BRUKERS_BARN_LISTE {
        override fun elementName() = "brukersBarnListe"
        override fun typeName() = "v1.BrukersBarnListe"
    },

}

interface Metadata {
    fun elementName(): String
    fun typeName(): String
}