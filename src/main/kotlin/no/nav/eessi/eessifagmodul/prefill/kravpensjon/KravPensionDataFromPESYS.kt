package no.nav.eessi.eessifagmodul.prefill.kravpensjon

import no.nav.eessi.eessifagmodul.models.Barn
import no.nav.eessi.eessifagmodul.models.Pensjon
import no.nav.eessi.eessifagmodul.prefill.Prefill
import no.nav.eessi.eessifagmodul.prefill.PrefillDataModel
import no.nav.eessi.eessifagmodul.prefill.vedtak.PensjonData
import no.nav.eessi.eessifagmodul.services.pensjonsinformasjon.PensjonsinformasjonService
import no.nav.pensjon.v1.pensjonsinformasjon.Pensjonsinformasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
/**
 * Hjelpe klasse for sak som fyller ut NAV-SED-P2000 med pensjondata fra PESYS.
 */
class KravPensionDataFromPESYS(private val pensjonsinformasjonService: PensjonsinformasjonService) : PensjonData(), Prefill<Pensjon> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(KravPensionDataFromPESYS::class.java) }

    fun getPensjoninformasjonFraSak(sakId: String): Pensjonsinformasjon {
        val pendata: Pensjonsinformasjon = pensjonsinformasjonService.hentAltSak(sakId) // ha med saknr og vedtak?
//        logger.debug("Pensjonsinformasjon: $pendata")
//        logger.debug("Pensjonsinformasjon.vedtak: ${pendata.vedtak}")
//        logger.debug("Pensjonsinformasjon.vedtak.virkningstidspunkt: ${pendata.vedtak.virkningstidspunkt}")
//        logger.debug("Pensjonsinformasjon.sak: ${pendata.sak}")
//        logger.debug("Pensjonsinformasjon.trygdetidListe: ${pendata.trygdetidListe}")
//        logger.debug("Pensjonsinformasjon.vilkarsvurderingListe: ${pendata.vilkarsvurderingListe}")
//        logger.debug("Pensjonsinformasjon.ytelsePerMaanedListe: ${pendata.ytelsePerMaanedListe}")
//        logger.debug("Pensjonsinformasjon.trygdeavtale: ${pendata.trygdeavtale}")
//        logger.debug("Pensjonsinformasjon.person: ${pendata.person}")
//        logger.debug("Pensjonsinformasjon.person.pin: ${pendata.person.pid}")
        return pendata
    }


    fun createRelasjonerBarnOgAvdod(dataModel: PrefillDataModel, pendata: Pensjonsinformasjon): PrefillDataModel {

        val listbarmItem = mutableListOf<Barn>()
        pendata.brukersBarnListe.brukersBarnListe.forEach {
            val fnr = it.fnr
            val aktoerId = it.aktorId
            val type = it.type
            listbarmItem.add(Barn(
                    fnr = fnr,
                    aktoer = aktoerId,
                    type = type
            ))
        }
        dataModel.barnlist = listbarmItem

        dataModel.avdod = pendata.avdod.avdod ?: ""
        dataModel.avdodMor = pendata.avdod.avdodMor ?: ""
        dataModel.avdodFar = pendata.avdod.avdodFar ?: ""

        return dataModel
    }


    override fun prefill(prefillData: PrefillDataModel): Pensjon {

        val pendata: Pensjonsinformasjon = getPensjoninformasjonFraSak(prefillData.penSaksnummer)


        createRelasjonerBarnOgAvdod(prefillData, pendata)


        return Pensjon(


        )

    }
}
