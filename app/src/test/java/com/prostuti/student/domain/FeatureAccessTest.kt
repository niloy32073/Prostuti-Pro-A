package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.MeData
import com.prostuti.student.data.repository.fake.FakeUserRepository
import com.prostuti.student.ui.screen.FeatureAccessViewModel
import com.prostuti.student.util.retryWhileAuthzUnavailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureAccessTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun me(vararg scopes: String): MeData = FakeUserRepository.me().copy(scopes = scopes.toList())

    // ── scopes ──

    @Test fun `each feature follows its own scope`() {
        assertEquals(FeatureAccess(group = true, routine = true), FeatureAccess.of(me("group:use", "routine:use", "exam:read")))
        assertEquals(FeatureAccess(group = false, routine = true), FeatureAccess.of(me("routine:use")))
        assertEquals(FeatureAccess(group = true, routine = false), FeatureAccess.of(me("group:use")))
    }

    @Test fun `an empty scope list turns both off, an unknown profile leaves both on`() {
        assertEquals(FeatureAccess(group = false, routine = false), FeatureAccess.of(me()))
        assertEquals(FeatureAccess(), FeatureAccess.of(null))
    }

    @Test fun `the view model reads the cached profile and ignores a failure`() = runTest(dispatcher) {
        val repo = FakeUserRepository(listOf(ApiResponse.Success(me("group:use")), ApiResponse.Failure("down", "NETWORK_ERROR")))
        val vm = FeatureAccessViewModel(GetMeUseCase(repo))
        val job = launch { vm.access.collect { } }
        advanceUntilIdle()
        assertTrue(vm.access.value.group)
        assertFalse(vm.access.value.routine)
        assertEquals(listOf(false), repo.refreshArgs)
        job.cancel()
    }

    // ── AUTHZ_UNAVAILABLE retry ──

    @Test fun `retries with backoff while authz is down, then returns the answer`() = runTest(dispatcher) {
        var calls = 0
        val r = retryWhileAuthzUnavailable {
            calls++
            if (calls < 3) ApiResponse.Failure("busy", "AUTHZ_UNAVAILABLE") else ApiResponse.Success("ok")
        }
        assertEquals(ApiResponse.Success("ok"), r)
        assertEquals(3, calls)
        assertEquals("1 s + 2 s", 3_000L, currentTime)
    }

    @Test fun `gives up after three retries`() = runTest(dispatcher) {
        var calls = 0
        val r = retryWhileAuthzUnavailable<String> { calls++; ApiResponse.Failure("busy", "AUTHZ_UNAVAILABLE") }
        assertEquals("AUTHZ_UNAVAILABLE", (r as ApiResponse.Failure).errorCode)
        assertEquals(4, calls)
        assertEquals(7_000L, currentTime)
    }

    @Test fun `insufficient scope and other failures aren't retried`() = runTest(dispatcher) {
        for (code in listOf("INSUFFICIENT_SCOPE", "NETWORK_ERROR")) {
            var calls = 0
            retryWhileAuthzUnavailable<String> { calls++; ApiResponse.Failure("no", code) }
            assertEquals(code, 1, calls)
        }
        assertEquals(0L, currentTime)
    }
}
