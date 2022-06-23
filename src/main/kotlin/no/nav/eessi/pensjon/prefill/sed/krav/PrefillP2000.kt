package no.nav.eessi.pensjon.prefill.sed.krav

import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.Krav
import no.nav.eessi.pensjon.eux.model.sed.Nav
import no.nav.eessi.pensjon.eux.model.sed.P2000
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.models.PrefillDataModel
import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
import no.nav.pensjon.v1.sak.V1Sak
import no.nav.pensjon.v1.vedtak.V1Vedtak
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * preutfylling av NAV-P2000 SED for søknad krav om alderpensjon
 */
class PrefillP2000(private val prefillNav: PrefillPDLNav)  {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP2000::class.java) }

    fun prefillSed(prefillData: PrefillDataModel, personData: PersonDataCollection, sak: V1Sak?, vedtak: V1Vedtak? = null): SED {
        postPrefill(prefillData, sak, vedtak)

        val pensjon = PrefillP2xxxPensjon.populerPensjon(prefillData, sak)

        val nav = prefillPDLNav(prefillData, personData, pensjon?.kravDato)

        logger.info("kravdato : ${pensjon?.kravDato}")

        val sed = P2000(
            type = SedType.P2000,
            nav = nav,
            pensjon = pensjon
        )

        validate(sed)
        return sed
    }

    private fun prefillPDLNav(prefillData: PrefillDataModel, personData: PersonDataCollection, krav: Krav?): Nav {
        return prefillNav.prefill(
            penSaksnummer = prefillData.penSaksnummer,
            bruker = prefillData.bruker,
            avdod = prefillData.avdod,
            personData = personData,
            bankOgArbeid = prefillData.getBankOgArbeidFromRequest(),
            krav = krav,
            annenPerson = null
        )
    }

    private fun postPrefill(prefillData: PrefillDataModel, sak: V1Sak?, vedtak: V1Vedtak?) {
        val SedType = SedType.P2000
        PrefillP2xxxPensjon.validerGyldigVedtakEllerKravtypeOgArsak(sak, SedType, vedtak)
        logger.debug("----------------------------------------------------------"
                + "\nSaktype                 : ${sak?.sakType} "
                + "\nSøker etter SakId       : ${prefillData.penSaksnummer} "
                + "\nSøker etter aktoerid    : ${prefillData.bruker.aktorId} "
                + "\n------------------| Preutfylling [$SedType] START |------------------ ")
    }

    private fun validate(sed: SED) {
        when {
            sed.nav?.bruker?.person?.etternavn == null -> throw ValidationException("Etternavn mangler")
            sed.nav?.bruker?.person?.fornavn == null -> throw ValidationException("Fornavn mangler")
            sed.nav?.bruker?.person?.foedselsdato == null -> throw ValidationException("Fødseldsdato mangler")
            sed.nav?.bruker?.person?.kjoenn == null -> throw ValidationException("Kjønn mangler")
            sed.nav?.krav?.dato == null -> {
                logger.warn("Kravdato mangler! Gjelder utsendelsen 'Førstegangsbehandling kun utland', se egen rutine på Navet.")
                throw ValidationException("Kravdato mangler\nGjelder utsendelsen \"Førstegangsbehandling kun utland\", se egen rutine på Navet.")
            }
        }
    }
}

class ValidationException(message: String) : ResponseStatusException(HttpStatus.BAD_REQUEST, message)
