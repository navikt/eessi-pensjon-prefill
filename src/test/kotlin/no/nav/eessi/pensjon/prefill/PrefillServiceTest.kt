package no.nav.eessi.pensjon.prefill
import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.eux.model.BucType.P_BUC_01
import no.nav.eessi.pensjon.eux.model.SedMock
import no.nav.eessi.pensjon.eux.model.SedType
import no.nav.eessi.pensjon.eux.model.sed.Bruker
import no.nav.eessi.pensjon.eux.model.sed.InstitusjonX005
import no.nav.eessi.pensjon.eux.model.sed.Kontekst
import no.nav.eessi.pensjon.eux.model.sed.Leggtilinstitusjon
import no.nav.eessi.pensjon.eux.model.sed.Navsak
import no.nav.eessi.pensjon.eux.model.sed.P2000
import no.nav.eessi.pensjon.eux.model.sed.Person
import no.nav.eessi.pensjon.eux.model.sed.SED
import no.nav.eessi.pensjon.eux.model.sed.X005
import no.nav.eessi.pensjon.eux.model.sed.XNav
import no.nav.eessi.pensjon.prefill.models.PersonDataCollection
import no.nav.eessi.pensjon.prefill.sed.PrefillSEDService
import no.nav.eessi.pensjon.shared.api.InstitusjonItem
import no.nav.eessi.pensjon.shared.api.PersonId
import no.nav.eessi.pensjon.shared.api.PrefillDataModel
import no.nav.eessi.pensjon.statistikk.AutomatiseringStatistikkService
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

class PrefillServiceTest {

    private val mockPrefillSEDService: PrefillSEDService = mockk()
    private val innhentingService: InnhentingService = mockk()
    private val automatiseringStatistikkService: AutomatiseringStatistikkService = mockk()

    private lateinit var prefillService: PrefillService
    private lateinit var personcollection: PersonDataCollection

    @BeforeEach
    fun `startup initilize testing`() {
        prefillService = PrefillService(mockPrefillSEDService, innhentingService, automatiseringStatistikkService)
        personcollection = PersonDataCollection(null, null)
    }
}
