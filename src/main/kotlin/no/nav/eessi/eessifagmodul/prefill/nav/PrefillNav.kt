package no.nav.eessi.eessifagmodul.prefill.nav

import no.nav.eessi.eessifagmodul.models.*
import no.nav.eessi.eessifagmodul.prefill.Prefill
import no.nav.eessi.eessifagmodul.prefill.PrefillDataModel
import no.nav.eessi.eessifagmodul.utils.mapJsonToAny
import no.nav.eessi.eessifagmodul.utils.simpleFormat
import no.nav.eessi.eessifagmodul.utils.typeRefs
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

@Component
class PrefillNav(private val preutfyllingPersonFraTPS: PrefillPersonDataFromTPS) : Prefill<Nav> {

    private val logger: Logger by lazy { LoggerFactory.getLogger(PrefillNav::class.java) }
    private val barnSEDlist = listOf("P2000", "P2100", "P2200")

    @Value("\${eessi.pensjon.lokalid:NO:noinst002}")
    lateinit var institutionid: String

    @Value("\${eessi.pensjon.lokalnavn:NOINST002, NO INST002, NO}")
    lateinit var institutionnavn: String

    override fun prefill(prefillData: PrefillDataModel): Nav {
        logger.debug("perfill aktoerId: ${prefillData.aktoerID}")

        val nav = Nav(
                eessisak = createLokaltsaksnummer(prefillData),

                //skal denne kjøres hver gang? eller kun under P2000? P2100
                barn = createBarnlistefraTPS(prefillData),

                //createBrukerfraTPS død hvis etterlatt (etterlatt aktoerregister fylt ut)
                bruker = createBrukerfraTPS(prefillData),

                //benyttes i P2x000
                krav = createKrav()
        )

        logger.debug("Legger til 3. Informasjon om personens ansettelsesforhold og selvstendige næringsvirksomhet")
        nav.bruker?.arbeidsforhold = createInformasjonOmAnsettelsesforhold(prefillData)

        logger.debug("Legger til 8. Informasjon om betaling")
        nav.bruker?.bank = createBetaling(prefillData)

        //logger.debug("[${prefillData.getSEDid()}] Utfylling av NAV data med lokalsaksnr: ${prefillData.penSaksnummer}")

        return nav
    }

    //
    //TODO: Dette må hentes fra sak/krav
    private fun createKrav(): Krav {
        logger.debug("9.1      Dato for krav")
        return Krav(Date().simpleFormat())
    }


    // kan denne utfylling benyttes på alle SED?
    // etterlatt pensjon da er dette den avdøde.(ikke levende)
    // etterlatt pensjon da er den levende i pk.3 sed (gjenlevende) (pensjon.gjenlevende)
    private fun createBrukerfraTPS(utfyllingData: PrefillDataModel): Bruker {
        if (utfyllingData.erGyldigEtterlatt()) {
            logger.debug("2.1           Avdod person")
            return preutfyllingPersonFraTPS.prefillBruker(utfyllingData.avdod)
        }

        logger.debug("2.1       Forsikret person")
        return preutfyllingPersonFraTPS.prefillBruker(utfyllingData.personNr)
    }

    private fun createBarnlistefraTPS(utfyllingData: PrefillDataModel): List<BarnItem> {
        //sjekke om SED er P2x00 for utfylling av BARN
        if (barnSEDlist.contains(utfyllingData.getSEDid()).not()) {
            logger.debug("8.1           SKIP Preutfylling barn, ikke P2x00")
            return listOf()
        }
        logger.debug("8.1           Preutfylling barn")
        val barnaspin = preutfyllingPersonFraTPS.hentBarnaPinIdFraBruker(utfyllingData.personNr)
        val barna = mutableListOf<BarnItem>()
        barnaspin.forEach {
            val barnBruker = preutfyllingPersonFraTPS.prefillBruker(it)
            logger.debug("          Preutfylling barn")
            val barn = BarnItem(
                    person = barnBruker.person,
                    far = barnBruker.far,
                    mor = barnBruker.mor,
                    relasjontilbruker = "BARN"
            )
            barna.add(barn)
        }
        return barna.toList()
    }

    private fun createLokaltsaksnummer(prefillData: PrefillDataModel): List<EessisakItem> {
        logger.debug("1.1         Lokalt saksnummer (hvor hentes disse verider ifra?")
        //oppretter sakid
        return listOf(EessisakItem(
                institusjonsid = institutionid,
                institusjonsnavn = institutionnavn,
                saksnummer = prefillData.penSaksnummer,
                land = "NO"
        ))
    }


    private fun createInformasjonOmAnsettelsesforhold(prefillData: PrefillDataModel): List<ArbeidsforholdItem>? {
        logger.debug("3.1       Informasjon om ansettelsesforhold og selvstendig næringsvirksomhet ")
        val personInfo = hentPersonInformasjon(prefillData) ?: return null
        return listOf(createAnsettelsesforhold(personInfo))
    }

    fun createAnsettelsesforhold(personInfo: BrukerInformasjon): ArbeidsforholdItem {
        logger.debug("3.1.")

        return ArbeidsforholdItem(

                //3.1.1.
                yrke = "n/a",

                //3.1.2
                type = personInfo.workType,

                //3.1.3
                planlagtstartdato = personInfo.workStartDate?.simpleFormat() ?: null,

                //3.1.4
                sluttdato = personInfo.workEndDate?.simpleFormat() ?: null,

                //3.1.5
                planlagtpensjoneringsdato = personInfo.workEstimatedRetirementDate?.simpleFormat() ?: null,

                //3.1.6
                arbeidstimerperuke = personInfo.workHourPerWeek

        )
    }

    private fun createBetaling(prefillData: PrefillDataModel): Bank? {
        logger.debug("8    Informasjon om betaling")
        val personInfo = hentPersonInformasjon(prefillData) ?: return null

        return Bank(
                navn = personInfo.bankName,
                konto = Konto(
                        innehaver = Innehaver(
                                rolle = "01", //forsikkret bruker .. avventer med Verge "02",
                                navn = personInfo.bankName
                        ),
                        sepa = Sepa(
                                iban = personInfo.bankIban,
                                swift = personInfo.bankBicSwift
                        )

                ),
                adresse = Adresse(
                        gate = personInfo.bankAddress,
                        land = personInfo.bankCountry?.currencyLabel
                )
        )

    }

    //henter ut PersonInfo payload fra UI
    fun hentPersonInformasjon(prefillData: PrefillDataModel): BrukerInformasjon? {
        val persinfo = prefillData.getPartSEDasJson("PersonInfo") ?: return null
        return mapJsonToAny(persinfo, typeRefs())
    }

}

