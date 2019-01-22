package no.nav.eessi.eessifagmodul.services.eux

import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class RinaActionsTest {

    private val logger: Logger by lazy { LoggerFactory.getLogger(RinaActionsTest::class.java) }

    @Mock
    private lateinit var mockEuxService: EuxService

    private lateinit var rinaActions: RinaActions

    @Before
    fun setup() {
        logger.debug("Starting tests.... ...")
        rinaActions = RinaActions(mockEuxService)
        rinaActions.waittime = "2"
        println("waittime: ${rinaActions.waittime}")
    }

    private fun mockNotValidData(): List<RINAaksjoner> {
        return listOf(
                RINAaksjoner(
                        navn = "Read",
                        id = "123123123123",
                        kategori = "Documents",
                        dokumentType = "P2000",
                        dokumentId = "23123123"
                ),
                RINAaksjoner(
                        navn = "Send",
                        id = "123123123123",
                        kategori = "Documents",
                        dokumentType = "P3000",
                        dokumentId = "23123123"
                ),
                RINAaksjoner(
                        navn = "Delete",
                        id = "123123123123",
                        kategori = "Documents",
                        dokumentType = "P3000",
                        dokumentId = "23123123"
                ),
                RINAaksjoner(
                        navn = "Read",
                        id = "123123123123",
                        kategori = "Documents",
                        dokumentType = "P4000",
                        dokumentId = "23123123"
                ),
                RINAaksjoner(
                        navn = "Send",
                        id = "123123123123",
                        kategori = "Documents",
                        dokumentType = "P4000",
                        dokumentId = "23123123"
                ),
                RINAaksjoner(
                        navn = "Delete",
                        id = "123123123123",
                        kategori = "Documents",
                        dokumentType = "P4000",
                        dokumentId = "23123123"
                )

        )
    }

    private fun mockValidData(navn: String): List<RINAaksjoner> {
        val aksjonlist: MutableList<RINAaksjoner> = mutableListOf()
        aksjonlist.addAll(mockNotValidData())
        aksjonlist.add(
                RINAaksjoner(
                        navn = navn,
                        id = "123123123123",
                        kategori = "Documents",
                        dokumentType = "P2000",
                        dokumentId = "23123123"
                )
        )
        aksjonlist.addAll(mockNotValidData())
        return aksjonlist
    }

    private fun mockValidDataAtOnce(navn: String): List<RINAaksjoner> {
        val aksjonlist: MutableList<RINAaksjoner> = mutableListOf()
        aksjonlist.add(
                RINAaksjoner(
                        navn = navn,
                        id = "123123123123",
                        kategori = "Documents",
                        dokumentType = "P2000",
                        dokumentId = "23123123"
                )
        )
        return aksjonlist
    }


    @Test
    fun `check canUpdate actions found at end`() {
        //val response = mockNotValidData()
        val finalResponse = mockValidData("Update")

        whenever(mockEuxService.getPossibleActions(ArgumentMatchers.anyString())).thenReturn(finalResponse)

        val result = rinaActions.canUpdate("P2000", "92223424234")
        assertEquals(true, result)
    }

    @Test
    fun `check canUpdate actions not found`() {
        val response = mockNotValidData()
        whenever(mockEuxService.getPossibleActions(ArgumentMatchers.anyString()))
                .thenReturn(response)

        val result = rinaActions.canUpdate("P2000", "92223424234")
        assertEquals(false, result)
    }

    @Test
    fun `check canUpdate actions valid at once`() {
        val finalResponse = mockValidDataAtOnce("Update")

        whenever(mockEuxService.getPossibleActions(ArgumentMatchers.anyString()))
                .thenReturn(finalResponse)

        val result = rinaActions.canUpdate("P2000", "92223424234")
        assertEquals(true, result)
    }

    @Test
    fun `check canCreate action can not create`() {
        val response = mockNotValidData()

        whenever(mockEuxService.getPossibleActions(ArgumentMatchers.anyString()))
                .thenReturn(response)
        val result = rinaActions.canCreate("P2000", "92223424234")
        assertEquals(false, result)
    }

    @Test
    fun `check canCreate actions can create selected SED`() {
        val response = mockValidData("Create")

        whenever(mockEuxService.getPossibleActions(ArgumentMatchers.anyString()))
                .thenReturn(response)
        val result = rinaActions.canCreate("P2000", "92223424234")
        assertEquals(true, result)
    }

    @Test
    fun `check canCreate actions can create selected SED at once`() {
        val response = mockValidDataAtOnce("Create")

        whenever(mockEuxService.getPossibleActions(ArgumentMatchers.anyString()))
                .thenReturn(response)
        val result = rinaActions.canCreate("P2000", "92223424234")
        assertEquals(true, result)
    }


}