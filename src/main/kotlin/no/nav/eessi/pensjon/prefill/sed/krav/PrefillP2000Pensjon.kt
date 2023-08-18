package no.nav.eessi.pensjon.prefill.sed.krav

import no.nav.eessi.pensjon.eux.model.sed.*
import no.nav.eessi.pensjon.pensjonsinformasjon.KravHistorikkHelper.finnKravHistorikkForDato
import no.nav.eessi.pensjon.pensjonsinformasjon.KravHistorikkHelper.hentKravHistorikkForsteGangsBehandlingUtlandEllerForsteGang
import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
import no.nav.eessi.pensjon.prefill.sed.krav.PrefillP2xxxPensjon.createYtelserItem
import no.nav.eessi.pensjon.prefill.sed.krav.PrefillP2xxxPensjon.hentYtelsePerMaanedDenSisteFraKrav
import no.nav.eessi.pensjon.prefill.sed.krav.PrefillP2xxxPensjon.opprettForkortetYtelsesItem
import no.nav.eessi.pensjon.prefill.sed.krav.PrefillP2xxxPensjon.opprettMeldingBasertPaaSaktype
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.utils.simpleFormat
import no.nav.pensjon.v1.kravhistorikk.V1KravHistorikk
import no.nav.pensjon.v1.sak.V1Sak
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Hjelpe klasse for sak som fyller ut NAV-SED-P2000 med pensjondata fra PESYS.
 */
object PrefillP2000Pensjon {
    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP2000Pensjon::class.java) }


    /**
     * 9.1- 9.2
     *
     *  Fra PSAK, kravdato på alderspensjonskravet
     *  Fra PSELV eller manuell kravblankett:
     *  Fyller ut fra hvilket tidspunkt bruker ønsker å motta pensjon fra Norge.
     *  Det er et spørsmål i søknadsdialogen og på manuell kravblankett. Det er ikke nødvendigvis lik virkningstidspunktet på pensjonen.
     */
    private fun createKravDato(valgtKrav: V1KravHistorikk?): Krav? {
        logger.debug("9.1        Dato Krav (med korrekt data fra PESYS krav.virkningstidspunkt)")
        logger.debug("KravType   :  ${valgtKrav?.kravType}")
        logger.debug("mottattDato:  ${valgtKrav?.mottattDato}")
        logger.debug("--------------------------------------------------------------")

        logger.debug("Prøver å sette kravDato til Virkningstidpunkt: ${valgtKrav?.kravType} og dato: ${valgtKrav?.mottattDato}")

        if (valgtKrav != null && valgtKrav.mottattDato != null) {
            return Krav(dato = valgtKrav.mottattDato?.simpleFormat())
        }
        return null
    }

    /**
     *
     *  4.1
     *  Vi må hente informasjon fra PSAK:
     *  - Hvilke saker bruker har
     *  - Status på hver sak
     *  - Hvilke kravtyper det finnes på hver sak
     *  - Saksnummer på hver sak
     *  - første virk på hver sak
     *  Hvis bruker mottar en løpende ytelse, er det denne ytelsen som skal vises.
     *  Hvis bruker mottar både uføretrygd og alderspensjon, skal det vises alderspensjon.
     *  Hvis bruker ikke mottar løpende ytelse, skal man sjekke om han har søkt om en norsk ytelse.
     *  Hvis han har søkt om flere ytelser, skal man bruke den som det sist er søkt om.
     *  Det skal vises resultatet av denne søknaden, dvs om saken er avslått eller under behandling.
     *  For å finne om han har søkt om en norsk ytelse, skal man se om det finnes krav av typen:  «Førstegangsbehandling», «Førstegangsbehandling Norge/utland»,
     *  «Førstegangsbehandling bosatt utland» eller «Mellombehandling».
     *  Obs, krav av typen «Førstegangsbehandling kun utland» eller Sluttbehandling kun utland» gjelder ikke norsk ytelse.
     */
    fun populerMeldinOmPensjon(personNr: String,
                               penSaksnummer: String,
                               pensak: V1Sak?,
                               andreinstitusjonerItem: AndreinstitusjonerItem?,
                               bruker: Bruker? = null,
                               kravId: String? = null): MeldingOmPensjonP2000 {

        logger.info("4.1           Informasjon om ytelser")


        val ytelselist = mutableListOf<YtelserItem>()

        val v1KravHistorikk = finnKravHistorikkForDato(pensak)
        val melding = opprettMeldingBasertPaaSaktype(v1KravHistorikk, kravId, pensak?.sakType)
        val krav = createKravDato(v1KravHistorikk)

        logger.info("Krav (dato) = $krav")

        when (pensak?.ytelsePerMaanedListe) {
            null -> {
                logger.info("forkortet ytelsebehandling ved ytelsePerMaanedListe = null, status: ${pensak?.status}")
                ytelselist.add(opprettForkortetYtelsesItem(pensak, personNr, penSaksnummer, andreinstitusjonerItem))
            }
            else -> {
                try {
                    logger.info("sakType: ${pensak.sakType}")
                    val ytelseprmnd = hentYtelsePerMaanedDenSisteFraKrav(hentKravHistorikkForsteGangsBehandlingUtlandEllerForsteGang(pensak.kravHistorikkListe, pensak.sakType), pensak)
                    ytelselist.add(createYtelserItem(ytelseprmnd, pensak, personNr, penSaksnummer, andreinstitusjonerItem))
                } catch (ex: Exception) {
                    logger.warn(ex.message, ex)
                    ytelselist.add(opprettForkortetYtelsesItem(pensak, personNr, penSaksnummer, andreinstitusjonerItem))
                }
            }
        }

        return MeldingOmPensjonP2000(
            melding = melding,
            pensjon = P2000Pensjon(
                ytelser = ytelselist
            )
        )
    }

    fun populerPensjon(
        prefillData: PrefillDataModel,
        sak: V1Sak?
    ): P2000Pensjon? {
        val andreInstitusjondetaljer = EessiInformasjon().asAndreinstitusjonerItem()

        //valider pensjoninformasjon,
        return try {
            val meldingOmPensjon = populerMeldinOmPensjon(
                prefillData.bruker.norskIdent,
                prefillData.penSaksnummer,
                sak,
                andreInstitusjondetaljer
            )
            P2000Pensjon(kravDato = meldingOmPensjon.pensjon.kravDato)
            } catch (ex: Exception) {
            logger.error(ex.message, ex)
            null
            //hvis feiler lar vi SB få en SED i RINA
        }
    }
}
