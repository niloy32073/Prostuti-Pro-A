package com.prostuti.student.domain.widget

import com.prostuti.student.data.repository.WidgetPromoDismissals
import com.prostuti.student.data.repository.WidgetPromoRepository
import com.prostuti.student.ui.screen.home.HomeWidgetPromoViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant

class FakeHomeWidgetHost(var canPin: Boolean = true, var placed: Boolean = false, var pinAccepted: Boolean = true) : HomeWidgetHost {
    var pinRequests = 0
    override fun canPin() = canPin
    override suspend fun isPlaced() = placed
    override suspend fun requestPin(): Boolean = pinAccepted.also { pinRequests++ }
}

class FakeWidgetPromoRepository(var dismissals: WidgetPromoDismissals = WidgetPromoDismissals()) : WidgetPromoRepository {
    override suspend fun dismissals() = dismissals
    override suspend fun recordDismissal(atMillis: Long) {
        dismissals = WidgetPromoDismissals(dismissals.count + 1, atMillis)
    }
}

/** The "add the widget" card on Home: when it shows, closing it, adding the widget. */
@OptIn(ExperimentalCoroutinesApi::class)
class WidgetPromoTest {
    private val dispatcher = StandardTestDispatcher()
    private val now = Instant.parse("2026-11-12T10:00:00Z")
    private val host = FakeHomeWidgetHost()
    private val promo = FakeWidgetPromoRepository()
    private val shouldShow = ShouldShowWidgetPromoUseCase(host, promo)

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun closedAgo(days: Long, times: Int = 1) =
        WidgetPromoDismissals(count = times, lastAtMillis = now.minus(Duration.ofDays(days)).toEpochMilli())

    @Test fun `shows while no widget is placed and the launcher can add one`() = runTest(dispatcher) {
        assertTrue(shouldShow(now))
        host.placed = true
        assertFalse("already on the home screen", shouldShow(now))
        host.placed = false
        host.canPin = false
        assertFalse("launcher can't add it from the app", shouldShow(now))
    }

    @Test fun `closing hides it for seven days`() = runTest(dispatcher) {
        promo.dismissals = closedAgo(days = 6)
        assertFalse(shouldShow(now))
        promo.dismissals = closedAgo(days = 7)
        assertTrue(shouldShow(now))
    }

    @Test fun `five closes hide it for good`() = runTest(dispatcher) {
        promo.dismissals = closedAgo(days = 30, times = 4)
        assertTrue(shouldShow(now))
        promo.dismissals = closedAgo(days = 30, times = 5)
        assertFalse(shouldShow(now))
    }

    @Test fun `the card closes at once and counts the close`() = runTest(dispatcher) {
        val vm = HomeWidgetPromoViewModel(shouldShow, DismissWidgetPromoUseCase(promo), AddHomeWidgetUseCase(host), now = { now })
        vm.refresh()
        advanceUntilIdle()
        assertTrue(vm.state.value.visible)

        vm.dismiss()
        assertFalse("hidden before the write finishes", vm.state.value.visible)
        advanceUntilIdle()
        assertEquals(WidgetPromoDismissals(1, now.toEpochMilli()), promo.dismissals)
    }

    @Test fun `adding asks the launcher, and a refused request hides the card`() = runTest(dispatcher) {
        val vm = HomeWidgetPromoViewModel(shouldShow, DismissWidgetPromoUseCase(promo), AddHomeWidgetUseCase(host), now = { now })
        vm.refresh()
        advanceUntilIdle()

        vm.add()
        advanceUntilIdle()
        assertEquals(1, host.pinRequests)
        assertTrue("stays until the widget is really placed", vm.state.value.visible)

        host.placed = true
        vm.refresh()   // back from the launcher's dialog
        advanceUntilIdle()
        assertFalse(vm.state.value.visible)

        host.placed = false
        host.pinAccepted = false
        vm.refresh()
        advanceUntilIdle()
        vm.add()
        advanceUntilIdle()
        assertFalse(vm.state.value.visible)
    }
}
