package no.nav.eessi.pensjon.fagmodul.eux

import no.nav.eessi.pensjon.fagmodul.eux.basismodel.RinaAksjon
import no.nav.eessi.pensjon.fagmodul.eux.bucmodel.*
import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.models.SEDType
import no.nav.eessi.pensjon.utils.toJsonSkipEmpty
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


class BucUtils(private val buc: Buc ) {

    private val logger = LoggerFactory.getLogger(BucUtils::class.java)
    private val validbucsed = ValidBucAndSed()

    fun getBuc(): Buc {
        return buc
    }

    fun getStatus(): String? {
        return getBuc().status
    }

    fun getCreator(): Creator? {
        return getBuc().creator
    }

    fun getCreatorAsInstitusjonItem(): InstitusjonItem? {
        return InstitusjonItem(
                country = getCreator()?.organisation?.countryCode ?: "",
                institution = getCreator()?.organisation?.id ?: "",
                name = getCreator()?.organisation?.name
        )
    }

    fun getCaseOwnerOrCreator() = getCaseOwner() ?: getCreatorAsInstitusjonItem()

    fun getCaseOwner() : InstitusjonItem? {
        return try {
                getParticipants()
                    .asSequence()
                    .filter { it.role == "CaseOwner" }
                    .map {
                        InstitusjonItem(
                                country = it.organisation?.countryCode ?: "",
                                institution = it.organisation?.id ?: "",  //kan hende må være id?!
                                name = it.organisation?.name ?: "" //name optinal
                        )
                }.first()
        } catch (ex: Exception) {
            null
        }
    }

    fun getCreatorContryCode(): Map<String, String> {
        val countryCode = getCreator()?.organisation?.countryCode ?: "N/A"
        return mapOf(Pair("countrycode", countryCode))
    }

    private fun getDocuments(): List<DocumentsItem> {
        return getBuc().documents ?: throw NoSuchFieldException("Fant ikke DocumentsItem")
    }

    fun findDocument(documentId: String): ShortDocumentItem {
        getAllDocuments().forEach {
            if (documentId == it.id) {
                return it
            }
        }
        return ShortDocumentItem(id = documentId)
    }

    fun getBucAttachments(): List<Attachment>? {
        return getBuc().attachments
    }

    fun getLastDate(): LocalDate {
        val date = getBuc().lastUpdate
        return getLocalDate(date)
    }

    fun getStartDateLong(): Long {
        val date = getBuc().startDate
        return getDateTimeToLong(date)
    }

    fun getLastDateLong(): Long {
        val date = getBuc().lastUpdate
        return getDateTimeToLong(date)
    }

    private fun getLocalDate(date: Any?): LocalDate =
            when (date) {
                is Long -> Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate()
                is String -> {
                    val datestr = date.substring(0, date.indexOf('T'))
                    LocalDate.parse(datestr)
                }
                else -> LocalDate.now().minusYears(1000)
            }

    private fun getDateTimeToLong(dateTime: Any?): Long {
        return getDateTime(dateTime).millis
    }

    private fun getDateTime(dateTime: Any?): DateTime  {
        val zoneId = DateTimeZone.forID(ZoneId.systemDefault().id)

            return when (dateTime) {
                is Long -> DateTime(DateTime(dateTime).toInstant(),zoneId)
                is String -> DateTime(DateTime.parse(dateTime).toInstant(),zoneId)
                else -> DateTime.now().minusYears(1000)
            }
    }

    fun getProcessDefinitionName() = getBuc().processDefinitionName

    fun getProcessDefinitionVersion() = getBuc().processDefinitionVersion ?: ""

    fun findFirstDocumentItemByType(sedType: SEDType) = findFirstDocumentItemByType(sedType.name)

    fun findFirstDocumentItemByType(sedType: String) = getDocuments().find { sedType == it.type }?.let { createShortDocument(it) }

    private fun createShortDocument(documentItem: DocumentsItem) =
            ShortDocumentItem(
                id = documentItem.id,
                parentDocumentId = documentItem.parentDocumentId,
                type = documentItem.type,
                displayName = documentItem.displayName,
                status = documentItem.status,
                creationDate = getDateTimeToLong(documentItem.creationDate),
                lastUpdate = getDateTimeToLong(documentItem.lastUpdate),
                participants = createParticipants(documentItem.conversations),
                attachments = createShortAttachemnt(documentItem.attachments),
                version = getLatestDocumentVersion(documentItem.versions),
                firstVersion = getFirstVersion(documentItem.versions),
                lastVersion = getLastVersion(documentItem.versions),
                allowsAttachments = documentItem.allowsAttachments
        )

    private fun getLatestDocumentVersion(list: List<VersionsItem>?): String {
        return list?.sortedBy { it.id }
                ?.map { it.id }
                ?.lastOrNull() ?: "1"
    }

    private fun getLastVersion(list: List<VersionsItem>?): VersionsItemNoUser? {
        return list?.sortedBy { it.id }
                ?.map { VersionsItemNoUser(
                        id = it.id,
                        date = it.date
                )}
                ?.lastOrNull()
    }

    private fun getFirstVersion(list: List<VersionsItem>?): VersionsItemNoUser? {
        return list?.sortedBy { it.id }
                ?.map { VersionsItemNoUser(
                  id = it.id,
                  date = it.date
                )}
                ?.firstOrNull()
    }

    private fun createParticipants(conversations: List<ConversationsItem>?): List<ParticipantsItem?>? =
            if (conversations != null && conversations.any { it.userMessages != null }) {
                val conversation = conversations.findLast { it.userMessages != null }!!
                val userMessages = conversation.userMessages!!.filter { it.error == null }

                val senders = userMessages
                        .map { it.sender }
                        .distinctBy { it!!.id }
                        .map {
                            ParticipantsItem(
                                    role = "Sender",
                                    organisation = it as Sender,
                                    selected = false
                            )
                        }
                if (senders.isEmpty()) {
                    logger.warn("No " + "Sender" + "s found for conversation: ${conversation.id}")
                }

                val receivers = userMessages
                        .map { it.receiver }
                        .distinctBy { it!!.id }
                        .map {
                            ParticipantsItem(
                                    role = "Receiver",
                                    organisation = it as Receiver,
                                    selected = false
                            )
                        }
                if (receivers.isEmpty()) {
                    logger.warn("No " + "Receiver" + "s found for conversation: ${conversation.id}")
                }
                senders + receivers
            } else {
                conversations?.lastOrNull()?.participants
            }


    private fun createShortAttachemnt(attachments: List<Attachment>?) =
            attachments?.map {
                ShortAttachment(
                    id = it.id,
                    name = it.name,
                    mimeType = it.mimeType,
                    fileName = it.fileName,
                    documentId = it.documentId,
                    lastUpdate = getDateTimeToLong(it.lastUpdate),
                    medical = it.medical
                )
            }.orEmpty()

    fun getAllDocuments() = getDocuments().map { createShortDocument(it) }

    fun getDocumentByType(sedType: String): ShortDocumentItem {
        return getAllDocuments().filter { sedType == it.type }.get(0)
    }

    fun findAndFilterDocumentItemByType(sedType: SEDType) = findAndFilterDocumentItemByType(sedType.name)

    private fun findAndFilterDocumentItemByType(sedType: String) =
            getDocuments().filter { it.type == sedType }.map { createShortDocument(it) }

    fun getSbdh(): List<Sbdh> {
        val lists = mutableListOf<Sbdh>()
        val documents = getDocuments()
        for (doc in documents) {
            for (conv in doc.conversations!!) {
                val usermsgs = conv.userMessages
                usermsgs?.forEach {
                    val sbdh = it.sbdh!!
                    lists.add(sbdh)
                }
            }
        }
        return lists
    }

    fun getInternatinalId() = getBuc().internationalId

    fun getParticipants() = getBuc().participants ?: emptyList()

    fun getParticipantsAsInstitusjonItem() = getParticipants()
            .map {
                InstitusjonItem(
                        country = it.organisation?.countryCode ?: "",
                        institution = it.organisation?.id ?: "",  //kan hende må være id?!
                        name = it.organisation?.name ?: "" //name optinal
                )
            }.sortedBy { it.country }.toList()

    fun getParticipantsLand() = getParticipantsAsInstitusjonItem().map { it.country }.joinToString(separator = ",")

    fun getBucAction() = getBuc().actions

    fun getGyldigeOpprettSedAksjonList() : List<String> {
        val actions = getBucAction()!!
        val keyWord = "Create"
        return actions.asSequence()
                .filter { item -> item.name == keyWord }
                .filterNot { item -> item.documentType == null }
                .map { item -> item.documentType!! }
                .toList()
                .sorted()
    }

    fun getFiltrerteGyldigSedAksjonListAsString(): List<String> {
        val backupList = validbucsed.getAvailableSedOnBuc(getProcessDefinitionName())
        val gyldigeSedList = getSedsThatCanBeCreated()
        val aksjonsliste = getGyldigeOpprettSedAksjonList()

        if (gyldigeSedList.contains("DummyChooseParts") && gyldigeSedList.size == 1) {
            logger.debug("benytter backupList : ${backupList.toJsonSkipEmpty()}")
            return backupList
        }
        if ( aksjonsliste.isNotEmpty()) {
            logger.debug("benytter seg av aksjonliste: ${aksjonsliste.toJsonSkipEmpty()}")
            return filterSektorPandRelevantHorizontalSeds(aksjonsliste)
        }
        logger.debug("benytter seg av gyldigeSedList : ${gyldigeSedList.toJsonSkipEmpty()}")
        return filterSektorPandRelevantHorizontalSeds(gyldigeSedList)
    }

    fun getSedsThatCanBeCreated() : List<String> {
        val keyWord = "empty"
        val docs = getAllDocuments()
        return docs.asSequence()
                .filter { item -> item.status == keyWord }
                .filterNot { item -> item.type == null }
                .map { item -> item.type!! }
                .toList()
                .sorted()
    }

    fun checkIfSedCanBeCreated(sedType: String?): Boolean {
        if (getFiltrerteGyldigSedAksjonListAsString().none { it == sedType }) {
            logger.warn("SED $sedType kan ikke opprettes i RINA")
            throw SedDokumentKanIkkeOpprettesException("SED $sedType kan ikke opprettes i RINA")
        }
        return true
    }


    fun filterSektorPandRelevantHorizontalSeds(list: List<String>) =
            list.filter {
                it.startsWith("P")
                        .or(it.startsWith("H12"))
                        .or(it.startsWith("H07"))
                        .or(it.startsWith("H02"))
            }.sorted()

    fun getRinaAksjon(): List<RinaAksjon> {
        val aksjoner = mutableListOf<RinaAksjon>()
        val actionitems = getBuc().actions
        val buctype = getProcessDefinitionName()
        actionitems?.forEach {
            if (it.documentType != null) {
                aksjoner.add(
                        RinaAksjon(
                                dokumentType = it.documentType,
                                navn = it.name,
                                dokumentId = it.documentId,
                                kategori = "Documents",
                                id = buctype
                        )
                )
            }
        }
        return aksjoner.sortedBy { it.dokumentType }.toList()
    }

    fun findNewParticipants(potentialNewParticipants: List<InstitusjonItem>): List<InstitusjonItem> {
        val currentParticipants =
                getParticipants()
                        .map {
                            InstitusjonItem(
                                    country = it.organisation?.countryCode ?: "",
                                    institution = it.organisation?.id ?: "",  //kan hende må være id?!
                                    name = it.organisation?.name ?: "" //name optinal
                            )
                        }
        if (currentParticipants.isEmpty() && potentialNewParticipants.isEmpty()) {
            throw ManglerDeltakereException("Ingen deltakere/Institusjon er tom")
        }
        return potentialNewParticipants.filter {
            candidate -> currentParticipants.none { current -> candidate.country == current.country && candidate.institution == current.institution }
        }
    }


}

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class ManglerDeltakereException(message: String) : IllegalStateException(message)

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class SedDokumentKanIkkeOpprettesException(message: String) : Exception(message)
