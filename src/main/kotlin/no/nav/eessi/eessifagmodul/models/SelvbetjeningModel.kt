package no.nav.eessi.eessifagmodul.models


/**
 * Nav SED for P4000
 */
data class PersonTrygdeTid (
        val andrePerioder: List<StandardItem>? = null,
        val arbeidsledigPerioder: List<StandardItem>? = null,
        val boPerioder: List<StandardItem>? = null,
        val opplaeringPerioder: List<StandardItem>? = null,
        val sykePerioder: List<StandardItem>? = null,
        val barnepassPerioder: List<BarnepassItem>? = null,
        val ansattSelvstendigPerioder: List<AnsattSelvstendigItem>? = null,
        val forsvartjenestePerioder: List<StandardItem>? = null,
        val foedselspermisjonPerioder: List<StandardItem>? = null,
        val frivilligPerioder: List<StandardItem>? = null
)
data class TrygdeTidPeriode (
        //med sluttdato
        val lukketPeriode: Periode? = null,
        //uten sluttdato
        val openPeriode: Periode? = null
)
data class AnsattSelvstendigItem (
        val jobbUnderAnsattEllerSelvstendig: String? = null,
        val annenInformasjon: String? = null,
        val adresseFirma: Adresse? = null,
        val periode: TrygdeTidPeriode? = null,
        val forsikkringEllerRegistreringNr: String? = null,
        val navnFirma: String? = null,
        val typePeriode: String? = null,
        val usikkerDatoIndikator: String? = null
)
data class BarnepassItem(
        val annenInformasjon: String? = null,
        val informasjonBarn: InformasjonBarn? = null,
        val periode: TrygdeTidPeriode? = null,
        val usikkerDatoIndikator: String? = null
)
data class InformasjonBarn (
        val fornavn: String? = null,
        val land: String? = null,
        val etternavn: String? = null,
        val foedseldato: String? = null
)
data class StandardItem (
        val land: String? = null,
        val annenInformasjon: String? = null,
        val periode: TrygdeTidPeriode? = null,
        val usikkerDatoIndikator: String? = null,
        //arbeidsledig -- //opplæring -- //sykedom
        val navnPaaInstitusjon: String? = null,
        //for 'andreperioder' ektra info.
        val typePeriode: String? = null
)


