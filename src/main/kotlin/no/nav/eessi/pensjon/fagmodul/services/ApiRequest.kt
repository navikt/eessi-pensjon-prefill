package no.nav.eessi.pensjon.fagmodul.services

import no.nav.eessi.pensjon.fagmodul.models.IkkeGyldigKallException
import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.models.SED
import no.nav.eessi.pensjon.fagmodul.models.SEDType
import no.nav.eessi.pensjon.fagmodul.prefill.PrefillDataModel
import org.slf4j.LoggerFactory

//Samme som SedRequest i frontend-api
data class ApiRequest(
        val sakId: String,
        val vedtakId: String? = null,
        val kravId: String? = null,
        val aktoerId: String? = null,
        val fnr: String? = null,
        val payload: String? = null,
        val buc: String? = null,
        val sed: String? = null,
        val documentid: String? = null,
        val euxCaseId: String? = null,
        val institutions: List<InstitusjonItem>? = null,
        val subjectArea: String? = null,
        val skipSEDkey: List<String>? = null,
        val mockSED: Boolean? = null
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ApiRequest::class.java)

        //validatate request and convert to PrefillDataModel
        fun buildPrefillDataModelOnExisting(request: ApiRequest, fodselsnr: String): PrefillDataModel {
            return when {
                //request.sakId == null -> throw IkkeGyldigKallException("Mangler Saksnummer")
                request.sed == null -> throw IkkeGyldigKallException("Mangler SED")
                request.aktoerId == null -> throw IkkeGyldigKallException("Mangler AktoerID")
                request.euxCaseId == null -> throw IkkeGyldigKallException("Mangler euxCaseId (RINANR)")
                request.institutions == null -> throw IkkeGyldigKallException("Mangler Institusjoner")

                SEDType.isValidSEDType(request.sed) -> {
                    logger.info("ALL SED on existing Rina -> SED: ${request.sed} -> euxCaseId: ${request.sakId}")
                    val pinid = fodselsnr
                    PrefillDataModel().apply {
                        penSaksnummer = request.sakId
                        sed = SED.create(request.sed)
                        aktoerID = request.aktoerId
                        personNr = pinid
                        euxCaseID = request.euxCaseId
                        institution = request.institutions
                        vedtakId = request.vedtakId ?: ""
                        partSedAsJson[request.sed] = request.payload ?: "{}"
                        skipSedkey = request.skipSEDkey
                                ?: listOf("PENSED") //skipper all pensjon utfylling untatt kravdato
                    }
                }
                else -> throw IkkeGyldigKallException("Mangler SED, eller ugyldig type SED")
            }
        }

        //validatate request and convert to PrefillDataModel
        fun buildPrefillDataModelConfirm(request: ApiRequest, fodselsnr: String): PrefillDataModel {
            return when {
                //request.sakId == null -> throw IkkeGyldigKallException("Mangler Saksnummer")
                request.sed == null -> throw IkkeGyldigKallException("Mangler SED")
                request.aktoerId == null -> throw IkkeGyldigKallException("Mangler AktoerID")

                SEDType.isValidSEDType(request.sed) -> {
                    PrefillDataModel().apply {
                        penSaksnummer = request.sakId
                        sed = SED.create(request.sed)
                        aktoerID = request.aktoerId
                        personNr = fodselsnr
                        vedtakId = request.vedtakId ?: ""
                        partSedAsJson[request.sed] = request.payload ?: "{}"
//                    if (request.payload != null) {
//                        partSedAsJson[request.sed] = request.payload
//                    }
                        skipSedkey = request.skipSEDkey ?: listOf("PENSED")
                    }
                }
                else -> throw IkkeGyldigKallException("Mangler SED, eller ugyldig type SED")
            }
        }

        //validatate request and convert to PrefillDataModel
        fun buildPrefillDataModelOnNew(request: ApiRequest, fodselsnr: String): PrefillDataModel {
            return when {
                //request.sakId == null -> throw IkkeGyldigKallException("Mangler Saksnummer")
                request.sed == null -> throw IkkeGyldigKallException("Mangler SED")
                request.aktoerId == null -> throw IkkeGyldigKallException("Mangler AktoerID")
                request.buc == null -> throw IkkeGyldigKallException("Mangler BUC")
                request.subjectArea == null -> throw IkkeGyldigKallException("Mangler Subjekt/Sektor")
                request.institutions == null -> throw IkkeGyldigKallException("Mangler Institusjoner")

                //Denne validering og utfylling kan benyttes på SED P2000,P2100,P2200
                SEDType.isValidSEDType(request.sed) -> {
                    logger.info("ALL SED on new RinaCase -> SED: ${request.sed}")
                    val pinid = fodselsnr
                    PrefillDataModel().apply {
                        penSaksnummer = request.sakId
                        buc = request.buc
                        rinaSubject = request.subjectArea
                        sed = SED.create(request.sed)
                        aktoerID = request.aktoerId
                        personNr = pinid
                        institution = request.institutions
                        vedtakId = request.vedtakId ?: ""
                        partSedAsJson[request.sed] = request.payload ?: "{}"
                        skipSedkey = request.skipSEDkey ?: listOf("PENSED")
                    }
                }
                else -> throw IkkeGyldigKallException("Mangler SED, eller ugyldig type SED")
            }
        }
    }
}