package no.nav.eessi.pensjon.prefill

import jakarta.annotation.PostConstruct
import no.nav.eessi.pensjon.eux.model.SedType.P2000
import no.nav.eessi.pensjon.eux.model.SedType.P2100
import no.nav.eessi.pensjon.eux.model.SedType.P2200
import no.nav.eessi.pensjon.eux.model.SedType.P6000
import no.nav.eessi.pensjon.metrics.MetricsHelper
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.personoppslag.pdl.PersonoppslagException
import no.nav.eessi.pensjon.personoppslag.pdl.model.*
import no.nav.eessi.pensjon.personoppslag.pdl.model.Ident.Companion.bestemIdent
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.shared.person.Fodselsnummer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Service
class PersonDataService(private val personService: PersonService,
                        @Autowired(required = false) private val metricsHelper: MetricsHelper = MetricsHelper.ForTest()) {

    private val logger: Logger = LoggerFactory.getLogger(PersonDataService::class.java)

    private lateinit var HentPerson: MetricsHelper.Metric

    @PostConstruct
    fun initMetrics() {
        HentPerson = metricsHelper.init("HentPerson", ignoreHttpCodes = listOf(HttpStatus.BAD_REQUEST))
    }

    fun <R : IdentGruppe> hentIdent(identTypeWanted: R, ident: Ident): Ident? {
        return personService.hentIdent(identTypeWanted, ident)
    }

    fun hentPersonData(prefillData: PrefillDataModel) : PersonDataCollection {
        return when (prefillData.sedType) {
            //alle med barn
            P2000, P2200, P2100, P6000 -> hentPersonerMedBarn(prefillData)
            //alle uten barn
            else -> hentPersoner(prefillData)
        }
    }

    private fun personServiceHentPerson(ident: Ident): Person? {
        return try {
            personService.hentPerson(ident) ?: throw NullPointerException()
        } catch (np: NullPointerException) {
            logger.error("PDL Person null")
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Person ikke funnet")
        } catch (pe: PersonoppslagException) {
            logger.error("PersonoppslagException: ${pe.message}")
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
            val forsikretPerson = personServiceHentPerson(bestemIdent(prefillData.bruker.norskIdent))

            val gjenlevendeEllerAvdod = if (prefillData.avdod != null) {
                logger.info("Henter avød person")
                personService.hentPerson(Ident.bestemIdent(prefillData.avdod.norskIdent))
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

            val ektefelleBruker = sivilstand?.relatertVedSivilstand?.let { personService.hentPerson(bestemIdent(it)) }
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
                .also { logger.info("prøver å hente ut alle barn på hovedperson: " + it.size) }
            .onEach { barnPin ->  logger.info("Er under 18: " + Fodselsnummer.fra(barnPin)?.isUnder18Year() ) }
            .filter { barnPin ->
                    try {
                        Fodselsnummer.fraMedValidation (barnPin)?.isUnder18Year()!!
                    } catch (ex: Exception) {
                        logger.warn("Feiler ved validering av fnr for barn ${ex.message}")
                        false
                    }
            }
        logger.info("prøver å hente ut alle barn (filtrert under 18) på hovedperson: " + barnepinListe.size)

        return barnepinListe
            .mapNotNull { barnPin -> barnPin?.let { NorskIdent(it) }?.let { personServiceHentPerson(it) } }
            .filterNot{ barn -> barn.erDoed() }
            .onEach {barn ->
                logger.debug("Hentet følgende barn fra PDL aktoerid: ${barn.identer.firstOrNull { it.gruppe == IdentGruppe.AKTORID }}")
            }
    }

    private fun filterEktefelleRelasjon(forsikretPerson: Person?): Sivilstand? {
        //Det støttes kun EKTEFELLE, REGISTERT_PARTNER og SAMBOER er de eneste sivilstand som støttes i RINA-SED
        //Vi aventer med støtte for SAMBOER fra pensjon-pdl lager en ny løsning for samboere
        val validRelasjoner = listOf(Sivilstandstype.GIFT, Sivilstandstype.REGISTRERT_PARTNER)
        return forsikretPerson?.sivilstand
            ?.filter { validRelasjoner.contains(it.type) }
            ?.maxByOrNull { it.metadata.sisteRegistrertDato() }
    }

    fun hentFnrEllerNpidFraAktoerService(aktoerid: String): String? {
        val fnr = hentIdent(IdentGruppe.FOLKEREGISTERIDENT, AktoerId(aktoerid))?.id
        if (fnr.isNullOrEmpty().not()) return fnr

        val npid = hentIdent(IdentGruppe.NPID, AktoerId(aktoerid))?.id
        if (npid.isNullOrEmpty().not()) return npid
        return null
    }

}