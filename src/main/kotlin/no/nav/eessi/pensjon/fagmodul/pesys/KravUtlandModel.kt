package no.nav.eessi.pensjon.fagmodul.pesys

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.time.LocalDate


//Model for opprettelse av kravhode til pesys.
//Krever p2x00 og P3000_no, P4000,P5000 før en kan
//tilby tjeneste med nødvendig informasjon for PESYS til å opprette kravhode automatisk.

//omhandler alle SED?
data class KravUtland(
        val errorMelding: String? = null,

        @JsonDeserialize(using = LocalDateDeserializer::class)
        @JsonSerialize(using = LocalDateSerializer::class)
        val mottattDato: LocalDate? = null,  // 9.1 kravsato

        @JsonDeserialize(using = LocalDateDeserializer::class)
        @JsonSerialize(using = LocalDateSerializer::class)
        val iverksettelsesdato: LocalDate? = null, // 9.1 kravsato

        @JsonDeserialize(using = LocalDateDeserializer::class)
        @JsonSerialize(using = LocalDateSerializer::class)
        val fremsattKravdato: LocalDate? = null, // 9.1 kravsato - 3mnd og den 1 den mnd

        val uttaksgrad: String? = "0",         //P3000

        val vurdereTrygdeavtale: Boolean? = null,        //P5000

        val personopplysninger: SkjemaPersonopplysninger? = null,

        val utland: SkjemaUtland? = null, //utland opphold filtrer bort Norge

        var sivilstand: SkjemaFamilieforhold? = null, // gift, samb..

        val soknadFraLand: String? = null, //hvilket land kommer kravsøknad fra (buc-caseowner)

        val initiertAv: String = "BRUKER" //skal alltid være BRUKER
)


//private val errorMelding: String? = null
//private val initiertAv: String? = null
//
//@JsonProperty("iverksettelsesdato")
//private val iverksettelsesDato: Date? = null
//private val mottattDato: Date? = null
//private val fremsattKravdato: Date? = null
//private val personopplysninger: Personopplysninger? = null
//private val sivilstand: Sivilstand? = null
//private val soknadFraLand: String? = null
//private val utland: Utland? = null
//private val uttaksgrad: String? = null
//private val vurdereTrygdeavtale: Boolean? = null

data class SkjemaPersonopplysninger(
        val statsborgerskap: String? = null   //P2000 pkt. 2.2.1.1 land_3 tegn
)

//P4000
data class SkjemaUtland(
        val utlandsopphold: List<Utlandsoppholditem>? = null
)

//P4000 - P5000 (for bosted nå) P4000 kan f.eks inneholde kun norge noe pesys ikke vil ha
//da må vi også sende med data fra P5000.
data class Utlandsoppholditem(
        val land: String? = null,
        //2017-05-01T00:00:00+02:00
        @JsonDeserialize(using = LocalDateDeserializer::class)
        @JsonSerialize(using = LocalDateSerializer::class)
        //format pattern yyyy-MM-dd
        val fom: LocalDate? = null,
        @JsonDeserialize(using = LocalDateDeserializer::class)
        @JsonSerialize(using = LocalDateSerializer::class)
        //format pattern yyyy-MM-dd
        val tom: LocalDate? = null,
        val bodd: Boolean? = null,
        val arbeidet: Boolean? = null,
        val pensjonsordning: String? = null,
        val utlandPin: String? = null
)

//P2000
data class SkjemaFamilieforhold(
        //Sivilstand for søker. Må være en gyldig verdi fra T_K_SIVILSTATUS_T:
        //ENKE, GIFT, GJES, GJPA, GJSA, GLAD, PLAD, REPA,SAMB, SEPA, SEPR, SKIL, SKPA, UGIF.
        //Pkt p2000 - 2.2.2.1. Familiestatus
        val valgtSivilstatus: String? = null,
        @JsonDeserialize(using = LocalDateDeserializer::class)
        @JsonSerialize(using = LocalDateSerializer::class)
        //format pattern yyyy-MM-dd
        val sivilstatusDatoFom: LocalDate? = null
)
