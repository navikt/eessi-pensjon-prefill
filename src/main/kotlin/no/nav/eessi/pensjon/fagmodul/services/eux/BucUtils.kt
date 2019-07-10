package no.nav.eessi.pensjon.fagmodul.services.eux

import no.nav.eessi.pensjon.fagmodul.models.IkkeGyldigKallException
import no.nav.eessi.pensjon.fagmodul.models.InstitusjonItem
import no.nav.eessi.pensjon.fagmodul.models.SEDType
import no.nav.eessi.pensjon.fagmodul.services.eux.bucmodel.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId


class BucUtils(private val buc: Buc ) {

    private val logger = LoggerFactory.getLogger(BucUtils::class.java)

    private fun getBuc(): Buc {
        return buc
    }

    fun getStatus(): String? {
        return getBuc().status
    }

    fun getCreator(): Creator? {
        return getBuc().creator
    }

    fun getCreatorContryCode(): Map<String, String> {
        val countryCode = getCreator()?.organisation?.countryCode ?: "N/A"
        return mapOf(Pair("countrycode", countryCode))
    }

    fun getSubject(): Subject {
        return getBuc().subject ?: throw NoSuchFieldException("Fant ikke Subject")
    }

    fun getDocuments(): List<DocumentsItem> {
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

    fun getStartDate(): LocalDate {
        val date = getBuc().startDate
        return getLocalDate(date)
    }

    fun getLastDate(): LocalDate {
        val date = getBuc().lastUpdate
        return getLocalDate(date)
    }

    fun getStartDateLong(): Long {
        val date = getBuc().startDate
        return getLocalDateTimeToLong(date)
    }

    fun getLastDateLong(): Long {
        val date = getBuc().lastUpdate
        return getLocalDateTimeToLong(date)
    }


    fun getLocalDate(date: Any?): LocalDate {
        return if (date is Long) {
            Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate()
        } else if (date is String) {
            val datestr = date.substring(0, date.indexOf('T'))
            LocalDate.parse(datestr)
        } else {
            LocalDate.now().minusYears(1000)
        }
    }

    fun getLocalDateTimeToLong(dateTime: Any?): Long {
        val zoneId = ZoneId.systemDefault()
        return getLocalDateTime(dateTime).atZone(zoneId).toEpochSecond()
    }

    fun getLocalDateTime(dateTime: Any?): LocalDateTime {
        return if (dateTime is Long) {
            Instant.ofEpochSecond(dateTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
        } else if (dateTime is String) {
            //val datestr = dateTime.substring(0, dateTime.indexOf('T'))
            LocalDateTime.parse(dateTime)
        } else {
            LocalDateTime.now().minusYears(1000)
        }
    }

    fun getProcessDefinitionName(): String? {
        return getBuc().processDefinitionName
    }

    fun getProcessDefinitionVersion(): String? {
        return getBuc().processDefinitionVersion
    }

    fun findFirstDocumentItemByType(sedType: SEDType): ShortDocumentItem? {
        return findFirstDocumentItemByType(sedType.name)
    }

    fun findFirstDocumentItemByType(sedType: String): ShortDocumentItem? {
        val documents = getDocuments()
        documents.forEach {
            if (sedType == it.type) {
                val shortdoc = createShortDocument(it)
                return shortdoc
            }
        }
        return null
    }

    private fun createShortDocument(documentItem: DocumentsItem): ShortDocumentItem {
        return ShortDocumentItem(
                id = documentItem.id,
                type = documentItem.type,
                displayName = documentItem.displayName,
                status = documentItem.status,
                creationDate = getLocalDateTimeToLong(documentItem.creationDate),
                lastUpdate = getLocalDateTimeToLong(documentItem.lastUpdate),
                participants = createParticipants(documentItem.conversations),
                attachments = createShortAttachemnt(documentItem.attachments)
        )
    }

    private fun createParticipants(conventions: List<ConversationsItem>?): List<ParticipantsItem?>? {

        conventions?.forEach {
            return it.participants
        }

        return null
    }


    private fun createShortAttachemnt(attachments: List<Attachment>?): List<ShortAttachment> {

        val list = mutableListOf<ShortAttachment>()
        attachments?.forEach {
            list.add(
                    ShortAttachment(
                            id = it.id,
                            name = it.name,
                            mimeType = it.mimeType,
                            fileName = it.fileName,
                            documentId = it.documentId,
                            lastUpdate = getLocalDateTimeToLong(it.lastUpdate),
                            medical = it.medical
                    )
            )
        }
        return list
    }

    fun getAllDocuments(): List<ShortDocumentItem> {
        val documents = getDocuments()
        val lists = mutableListOf<ShortDocumentItem>()
        documents.forEach {
            lists.add(createShortDocument(it))
        }
        return lists
    }

    fun findAndFilterDocumentItemByType(sedType: SEDType): List<ShortDocumentItem> {
        return findAndFilterDocumentItemByType(sedType.name)
    }

    fun findAndFilterDocumentItemByType(sedType: String): List<ShortDocumentItem> {
        val documents = getDocuments()
        val lists = mutableListOf<ShortDocumentItem>()
        documents.forEach {
            if (sedType == it.type) {
                lists.add(createShortDocument(it))
            }
        }
        return lists
    }

    //hjelpefunkson for å hente ut list over alle documentid til valgt SEDType (kan ha flere docid i type)
    fun findDocmentIdBySedType(sedType: SEDType): List<String?> {
        val doclist = findAndFilterDocumentItemByType(sedType)
        return doclist.map { it.id }.toList()
    }

    fun getSbdh(): List<Sbdh> {
        val lists = mutableListOf<Sbdh>()
        val documents = getDocuments()
        //Sbdh -> UserMessagesItem -> ConversationsItem -> DocumentsItem -> Buc
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

    fun getInternatinalId(): String? {
        return getBuc().internationalId
    }

    fun getParticipants(): List<ParticipantsItem> {
        return getBuc().participants ?: emptyList()
    }

    fun getBucAction(): List<ActionsItem>? {
        return getBuc().actions
    }

    fun getAksjonListAsString() : List<String> {
        val keywordCreate = "Create"
        val actions = getBuc().actions ?: listOf()
        val createAkjsonsliste = mutableListOf<String>()
        for(item in actions) {
            if (item.documentType != null && item.name == keywordCreate) {
                createAkjsonsliste.add(item.documentType)
            }
        }
        val aksjonlist = createAkjsonsliste
                .sortedBy { it }
                .toList()

        logger.debug("Seds AksjonList size: ${aksjonlist.size}")
        return aksjonlist
    }

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
                        .filter { it.role != "CaseOwner" }
                        .map {
                            InstitusjonItem(
                                    country = it.organisation?.countryCode ?: "",
                                    institution = it.organisation?.id ?: "",  //kan hende må være id?!
                                    name = "" //
                            )
                        }

        if (currentParticipants.isEmpty() && potentialNewParticipants.isEmpty()) {
            throw IkkeGyldigKallException("Ingen deltakere/Institusjon er tom")
        }

        return potentialNewParticipants.filter {
            candidate -> currentParticipants.none { current -> candidate.country == current.country && candidate.institution == current.institution }
        }
    }

}

