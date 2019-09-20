package no.nav.eessi.pensjon.fagmodul.prefill.sed

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.eessi.pensjon.fagmodul.prefill.model.Prefill
import no.nav.eessi.pensjon.fagmodul.prefill.model.PrefillDataModel
import no.nav.eessi.pensjon.fagmodul.prefill.person.PrefillPerson
import no.nav.eessi.pensjon.fagmodul.sedmodel.PinLandItem
import no.nav.eessi.pensjon.fagmodul.sedmodel.SED
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.typeRefs
import org.bouncycastle.asn1.x500.style.RFC4519Style.l
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PrefillH02X(private val prefillPerson: PrefillPerson) : Prefill<SED> {

    private val logger: Logger =  LoggerFactory.getLogger(PrefillH02X::class.java)

    override fun prefill(prefillData: PrefillDataModel): SED {

        val personSed = prefillPerson.prefill(prefillData)

        val pinitem = personSed.nav?.bruker?.person?.pin?.first()

        val pinLand = PinLandItem(
                oppholdsland = pinitem?.land,
                kompetenteuland = pinitem?.identifikator
        )
        val sed = prefillData.sed
        sed.nav?.bruker?.person?.pinland = pinLand

        return prefillData.sed
    }
}