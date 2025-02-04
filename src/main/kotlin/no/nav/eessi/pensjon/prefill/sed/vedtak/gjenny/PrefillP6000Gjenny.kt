//package no.nav.eessi.pensjon.prefill.sed.vedtak.gjenny
//
//import no.nav.eessi.pensjon.eux.model.sed.P6000
//import no.nav.eessi.pensjon.eux.model.sed.P6000Pensjon
//import no.nav.eessi.pensjon.prefill.etterlatte.EtterlatteResponse
//import no.nav.eessi.pensjon.prefill.models.EessiInformasjon
//import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
//import no.nav.eessi.pensjon.prefill.person.PrefillPDLNav
//import no.nav.eessi.pensjon.shared.api.PrefillDataModel
//import org.slf4j.Logger
//import org.slf4j.LoggerFactory
//
//
//class PrefillP6000Gjenny(private val prefillNav: PrefillPDLNav,
//                         private val eessiInfo: EessiInformasjon,
//                         private val vedtakFraGjenny: EtterlatteResponse?
//) {
//
//    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillP6000Gjenny::class.java) }
//
//    fun prefillGjenny(prefillData: PrefillDataModel, personData: PersonDataCollection): P6000 {
//        val sedType = prefillData.sedType
//
//        logger.info(
//            "----------------------------------------------------------"
//                    + "\nPreutfylling Pensjon for Gjenny  : P6000 "
//                    + "\n------------------| Preutfylling av [$sedType] START |------------------ "
//        )
//
//        logger.info("Henter ut lokal kontakt, institusjon (NAV Utland)")
//        val andreInstitusjondetaljer = eessiInfo.asAndreinstitusjonerItem()
//        logger.info("Andreinstitusjoner: $andreInstitusjondetaljer ")
//
//        logger.debug("Henter opp Persondata/Gjenlevende")
//        val gjenlevende = prefillData.avdod?.let { prefillNav.createGjenlevende(personData.forsikretPerson, prefillData.bruker) }
//
//        logger.debug("Henter inn vedtaksinformasjon fra Gjenny")
//        val p6000Pensjon = if(vedtakFraGjenny != null) prefillP6000PensjonGjenny(vedtakFraGjenny, gjenlevende, andreInstitusjondetaljer) else P6000Pensjon(gjenlevende)
//
//        val nav = prefillNav.prefill(
//            penSaksnummer = null,
//            bruker = prefillData.bruker,
//            avdod = prefillData.avdod,
//            personData = personData,
//            krav = p6000Pensjon.kravDato,
//            annenPerson = null,
//            bankOgArbeid = null
//        )
//
//        logger.info("-------------------| Preutfylling [$sedType] END |------------------- ")
//
//        return P6000(
//            type = sedType,
//            nav = nav,
//            pensjon = p6000Pensjon
//        )
//
//    }
//
//}
//
