package no.nav.eessi.pensjon.config

import no.nav.eessi.pensjon.personoppslag.pdl.PdlToken
import no.nav.eessi.pensjon.personoppslag.pdl.PdlTokenCallBack
import no.nav.eessi.pensjon.personoppslag.pdl.PdlTokenImp
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component


@Component("PdlTokenComponent")
@Profile("unsecured-webmvctest", "!prod", "!test")
@Primary
@Order(Ordered.HIGHEST_PRECEDENCE)
class PdlPrefillTokenComponent: PdlTokenCallBack {

    override fun callBack(): PdlToken {
        return PdlTokenImp(systemToken = "token", userToken = "token", isUserToken = false)
    }
}

