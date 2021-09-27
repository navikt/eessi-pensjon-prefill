package no.nav.eessi.pensjon.prefill

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.eessi.pensjon.eux.model.sed.SedType
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.personoppslag.Fodselsnummer
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.PersonoppslagException
import no.nav.eessi.pensjon.personoppslag.pdl.model.AktoerId
import no.nav.eessi.pensjon.personoppslag.pdl.model.Familierelasjonsrolle
import no.nav.eessi.pensjon.personoppslag.pdl.model.Ident
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentGruppe
import no.nav.eessi.pensjon.personoppslag.pdl.model.IdentType
import no.nav.eessi.pensjon.personoppslag.pdl.model.NorskIdent
import no.nav.eessi.pensjon.personoppslag.pdl.model.Person
import no.nav.eessi.pensjon.personoppslag.pdl.model.Sivilstand
import no.nav.eessi.pensjon.personoppslag.pdl.model.Sivilstandstype
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import javax.annotation.PostConstruct

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Service
class PersonDataService(private val personService: PersonService,
                        @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper(SimpleMeterRegistry())) {

    private val logger: Logger = LoggerFactory.getLogger(PersonDataService::class.java)

    private lateinit var HentPerson: MetricsHelper.Metric

    @PostConstruct
    fun initMetrics() {
        HentPerson = metricsHelper.init("HentPerson", ignoreHttpCodes = listOf(HttpStatus.BAD_REQUEST))
    }

    fun <T : IdentType, R : IdentType> hentIdent(identTypeWanted: R, ident: Ident<T>): Ident<R> {
        return personService.hentIdent(identTypeWanted, ident)
    }

    fun hentPersonData(prefillData: PrefillDataModel) : PersonDataCollection {
        return when (prefillData.sedType) {
            //alle med barn
            SedType.P2000, SedType.P2200, SedType.P2100, SedType.P6000 -> hentPersonerMedBarn(prefillData)
            //alle uten barn
            else -> hentPersoner(prefillData)
        }
    }

    private fun personServiceHentPerson(ident: NorskIdent): Person? {
        return try {
            personService.hentPerson(ident) ?: throw NullPointerException()
        } catch (np: NullPointerException) {
            logger.error("PDL Person null")
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Person ikke funnet")
        } catch (pe: PersonoppslagException) {
            logger.error("PersonoppslagExcpetion sanity check")
            logger.error("PersonoppslagExcpetion: ${pe.message}")
            when(pe.message) {
                "not_found: Fant ikke person" -> throw ResponseStatusException(HttpStatus.NOT_FOUND, "Person ikke funnet")
                "unauthorized: Ikke tilgang til å se person" -> throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ikke tilgang til å se person")
                else -> throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, pe.message)
            }
        }
    }

    private fun hentPersonerMedBarn(prefillData: PrefillDataModel) = hentPersoner(prefillData, true)

    //Henter inn alle personer fra ep-personoppslag  først før preutfylling
    private fun hentPersoner(prefillData: PrefillDataModel, fyllUtBarnListe: Boolean = false): PersonDataCollection {
        return HentPerson.measure {
            logger.info("Henter hovedperson/forsikret/gjenlevende")
            val forsikretPerson = personServiceHentPerson(NorskIdent(prefillData.bruker.norskIdent))

            val gjenlevendeEllerAvdod = if (prefillData.avdod != null) {
                logger.info("Henter avød person/forsikret")
                personService.hentPerson(NorskIdent(prefillData.avdod.norskIdent))
            } else {
                logger.info("Ingen avdød så settes til forsikretPerson")
                forsikretPerson
            }

            val sivilstand = filterEktefelleRelasjon(forsikretPerson)
            val sivilstandType = sivilstand?.type

            val ektefellePerson = hentHovedpersonEktefelle(sivilstand)

            val barnPerson = hentHovedpersonBarn(forsikretPerson, fyllUtBarnListe)

            logger.debug("gjenlevendeEllerAvdod: ${gjenlevendeEllerAvdod?.navn?.sammensattNavn }, forsikretPerson: ${forsikretPerson?.navn?.sammensattNavn }")

            PersonDataCollection(gjenlevendeEllerAvdod = gjenlevendeEllerAvdod, forsikretPerson = forsikretPerson!!, ektefellePerson = ektefellePerson,  sivilstandstype =  sivilstandType, barnPersonList = barnPerson)
        }
    }

    //sjekk for om sb har tilgang til person, null hvis ikke tilgang
    private fun hentHovedpersonEktefelle(sivilstand: Sivilstand?): Person? {
        return try {
            logger.info("Henter ektefelle/partner (ekteType: ${sivilstand?.type})")

            val ektefelleBruker = sivilstand?.relatertVedSivilstand?.let { personService.hentPerson(NorskIdent(it)) }
            ektefelleBruker?.takeUnless { it.erDoed() }
        } catch (ex: Exception) {
            logger.warn(ex.message)
            null
        }
    }

    //sjekk for om sb har tilgang til person, null hvis ikke tilgang
    private fun hentHovedpersonBarn(hovedPerson: Person?, fyllUtBarnListe: Boolean): List<Person> {
        return try {
            logger.info("Henter barn")
            if (hovedPerson == null || !fyllUtBarnListe) emptyList() else hentBarn(hovedPerson)
        } catch (ex: Exception) {
            logger.warn(ex.message)
            emptyList()
        }
    }

    private fun hentBarn(hovedPerson: Person): List<Person> {
        logger.info("henter ut relasjon BARN")
        val barnepinListe = hovedPerson.forelderBarnRelasjon
            .filter { it.relatertPersonsRolle == Familierelasjonsrolle.BARN }
            .map { it.relatertPersonsIdent }
            .filter { barnPin -> Fodselsnummer.fra(barnPin)?.isUnder18Year() ?: false }
        logger.info("prøver å hente ut alle barn (filtrert) på hovedperson: " + barnepinListe.size )

        return barnepinListe
            .mapNotNull { barnPin -> personServiceHentPerson(NorskIdent(barnPin)) }
            .filterNot{ barn -> barn.erDoed() }
            .onEach {barn ->
                logger.debug("Hentet følgende barn fra PDL aktoerid: ${barn.identer.firstOrNull { it.gruppe == IdentGruppe.AKTORID }}")
            }
    }

    private fun filterEktefelleRelasjon(forsikretPerson: Person?): Sivilstand? {
        val validRelasjoner = listOf(Sivilstandstype.GIFT, Sivilstandstype.REGISTRERT_PARTNER)
        return forsikretPerson?.sivilstand
            ?.filter { validRelasjoner.contains(it.type) }
            ?.maxByOrNull { it.metadata.sisteRegistrertDato() }
    }

    fun hentFnrfraAktoerService(aktoerid: String?): String {
        if (aktoerid.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Fant ingen aktoerident")
        }
        return hentIdent (IdentType.NorskIdent, AktoerId(aktoerid)).id

    }

}