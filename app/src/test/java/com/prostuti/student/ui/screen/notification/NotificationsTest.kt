package com.prostuti.student.ui.screen.notification

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.MetaData
import com.prostuti.student.data.model.response.NotificationItem
import com.prostuti.student.data.repository.NotificationRepository
import com.prostuti.student.domain.notification.FetchNotificationsUseCase
import com.prostuti.student.domain.notification.MarkNotificationReadUseCase
import com.prostuti.student.domain.notification.NotificationTarget
import com.prostuti.student.ui.screen.group.testErrorMessages
import com.prostuti.student.ui.screen.home.HomeNotificationsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = mockk<NotificationRepository>()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun item(id: String, read: Boolean = false, url: String? = "/groups/g1") =
        NotificationItem(id = id, title = "t$id", message = "m$id", type = "GROUP_ACTIVITY", targetUrl = url, isRead = read, createdAt = "2026-11-08T10:00:00")

    private fun meta(totalPages: Int, totalItems: Int = 0) =
        MetaData(totalItems = totalItems, currentPage = 1, totalPages = totalPages, pageLimit = 20)

    private fun vm() = NotificationsViewModel(FetchNotificationsUseCase(repo), MarkNotificationReadUseCase(repo), testErrorMessages)

    // ── target_url ──

    @Test fun `target urls the app knows`() {
        assertEquals(NotificationTarget.Group("g1"), NotificationTarget.parse("/groups/g1"))
        assertEquals(NotificationTarget.Group("g1"), NotificationTarget.parse("/groups/g1/members"))
        assertEquals(NotificationTarget.JoinRequests("g1"), NotificationTarget.parse("/groups/g1/join-requests"))
        assertEquals(NotificationTarget.JoinRequests("g1"), NotificationTarget.parse("/groups/g1/join-requests/r9?x=1"))
        assertEquals(NotificationTarget.Routine, NotificationTarget.parse("/routines/slots/s1"))
        assertEquals(NotificationTarget.Routine, NotificationTarget.parse("/routines"))
        assertEquals("full URL with the API prefix", NotificationTarget.Group("g1"), NotificationTarget.parse("https://api.example.com/api/v1/groups/g1#top"))
    }

    @Test fun `unknown or missing target urls open nothing`() {
        assertNull(NotificationTarget.parse(null))
        assertNull(NotificationTarget.parse(""))
        assertNull(NotificationTarget.parse("/groups"))
        assertNull(NotificationTarget.parse("/exams/e1"))
    }

    // ── inbox ──

    @Test fun `pages until total_pages and switches to unread only`() = runTest(dispatcher) {
        coEvery { repo.list(false, 1, 20) } returns ApiResponse.Success(listOf(item("a")), metaData = meta(totalPages = 2))
        coEvery { repo.list(false, 2, 20) } returns ApiResponse.Success(listOf(item("b")), metaData = meta(totalPages = 2))
        coEvery { repo.list(true, 1, 20) } returns ApiResponse.Success(listOf(item("u")), metaData = meta(totalPages = 1))
        val vm = vm()
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()
        assertEquals(listOf("a", "b"), vm.state.value.items.map { it.id })
        assertFalse(vm.state.value.canLoadMore)

        vm.setUnreadOnly(true)
        advanceUntilIdle()
        assertEquals(listOf("u"), vm.state.value.items.map { it.id })
    }

    @Test fun `a tap marks read at once and opens its target`() = runTest(dispatcher) {
        coEvery { repo.list(false, 1, 20) } returns ApiResponse.Success(listOf(item("a")))
        coEvery { repo.markRead("a") } returns ApiResponse.Success(Unit)
        val vm = vm()
        advanceUntilIdle()

        vm.open(vm.state.value.items.single())
        assertTrue("optimistic", vm.state.value.items.single().isRead)
        assertEquals(NotificationTarget.Group("g1"), vm.state.value.target)
        advanceUntilIdle()
        coVerify(exactly = 1) { repo.markRead("a") }
        vm.consumeTarget()
        assertNull(vm.state.value.target)
    }

    @Test fun `a read one isn't marked again`() = runTest(dispatcher) {
        coEvery { repo.list(false, 1, 20) } returns ApiResponse.Success(listOf(item("a", read = true, url = null)))
        val vm = vm()
        advanceUntilIdle()

        vm.open(vm.state.value.items.single())
        advanceUntilIdle()

        coVerify(exactly = 0) { repo.markRead(any()) }
        assertNull("no target: nothing to open", vm.state.value.target)
    }

    @Test fun `a failed mark goes back to unread, but a missing one stays read`() = runTest(dispatcher) {
        coEvery { repo.list(false, 1, 20) } returns ApiResponse.Success(listOf(item("a"), item("b")))
        coEvery { repo.markRead("a") } returns ApiResponse.Failure("down", "NETWORK_ERROR")
        coEvery { repo.markRead("b") } returns ApiResponse.Failure("gone", "NOTIFICATION_NOT_FOUND")
        val vm = vm()
        advanceUntilIdle()

        vm.open(vm.state.value.items[0])
        vm.open(vm.state.value.items[1])
        advanceUntilIdle()

        assertFalse(vm.state.value.items[0].isRead)
        assertTrue(vm.state.value.items[1].isRead)
        assertEquals(listOf("mapped:NETWORK_ERROR"), vm.state.value.errors.values.toList())
    }

    // ── bell ──

    @Test fun `the bell dot follows the unread count`() = runTest(dispatcher) {
        coEvery { repo.list(true, 1, 1) } returns ApiResponse.Success(listOf(item("a")), metaData = meta(totalPages = 3, totalItems = 3))
        val bell = HomeNotificationsViewModel(FetchNotificationsUseCase(repo))
        bell.refresh()
        advanceUntilIdle()
        assertTrue(bell.hasUnread.value)

        coEvery { repo.list(true, 1, 1) } returns ApiResponse.Success(emptyList(), metaData = meta(totalPages = 0, totalItems = 0))
        bell.refresh()
        advanceUntilIdle()
        assertFalse(bell.hasUnread.value)

        coEvery { repo.list(true, 1, 1) } returns ApiResponse.Failure("down", "NETWORK_ERROR")
        bell.refresh()
        advanceUntilIdle()
        assertFalse("a failure keeps the last answer", bell.hasUnread.value)
    }
}
