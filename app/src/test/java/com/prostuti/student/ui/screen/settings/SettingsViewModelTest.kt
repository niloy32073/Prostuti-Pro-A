package com.prostuti.student.ui.screen.settings

import app.cash.turbine.test
import com.prostuti.student.data.repository.fake.FakeDataStoreStorageRepository
import com.prostuti.student.data.repository.fake.FakeBookmarkRepository
import com.prostuti.student.data.repository.fake.FakeUserRepository
import com.prostuti.student.data.repository.fake.FakeThemeRepository
import com.prostuti.student.data.model.enums.ThemeMode
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.domain.GetMeUseCase
import com.prostuti.student.domain.ObserveThemeModeUseCase
import com.prostuti.student.domain.SetThemeModeUseCase
import com.prostuti.student.domain.ClearCachedMeUseCase
import com.prostuti.student.domain.ClearDataStoreUseCase
import com.prostuti.student.domain.ClearBookmarksUseCase
import com.prostuti.student.domain.ClearReportsUseCase
import com.prostuti.student.data.repository.fake.FakeReportRepository
import com.prostuti.student.data.repository.fake.FakeNotificationRepository
import com.prostuti.student.domain.notification.ClearPushTokenUseCase
import com.prostuti.student.dataStore.PrefKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeStorage: FakeDataStoreStorageRepository
    private val fakeUserRepo = FakeUserRepository()
    private val fakeBookmarkRepo = FakeBookmarkRepository()
    private val fakeReportRepo = FakeReportRepository()
    // JWT seen at the moment the push token is cleared — the PUT needs the session to still exist.
    private var jwtWhenPushCleared: String? = null
    private val fakeNotificationRepo = FakeNotificationRepository(onClear = { jwtWhenPushCleared = fakeStorage.getJWTToken() })

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeStorage = FakeDataStoreStorageRepository()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private val fakeTheme = FakeThemeRepository()

    private fun viewModel() = SettingsViewModel(
        ClearDataStoreUseCase(fakeStorage),
        ClearCachedMeUseCase(fakeUserRepo),
        ClearBookmarksUseCase(fakeBookmarkRepo),
        ClearReportsUseCase(fakeReportRepo),
        GetMeUseCase(fakeUserRepo),
        ObserveThemeModeUseCase(fakeTheme),
        SetThemeModeUseCase(fakeTheme),
        ClearPushTokenUseCase(fakeNotificationRepo)
    )

    @Test fun themeChoiceIsSavedAndReflectedInState() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, vm.state.value.themeMode)

        vm.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, fakeTheme.mode.value)
        assertEquals(ThemeMode.DARK, vm.state.value.themeMode)
    }

    @Test fun headerShowsTheCachedProfileName() = runTest {
        fakeUserRepo.results = listOf(ApiResponse.Success(FakeUserRepository.me(firstName = "Nifa", lastName = "Rahman")))
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals("Nifa Rahman", vm.state.value.userName)
    }

    @Test fun placeholderTogglesFlipInState() {
        val vm = viewModel()
        vm.setSoundEnabled(true)
        vm.setHapticsEnabled(false)
        assertTrue(vm.state.value.soundEnabled)
        assertFalse(vm.state.value.hapticsEnabled)
    }

    @Test fun initialStateIsNotLoggedOut() {
        assertFalse(viewModel().state.value.isLoggedOut)
    }

    @Test fun logoutClearsStorageAndMarksStateLoggedOut() = runTest {
        val vm = viewModel()

        vm.state.test {
            assertFalse(awaitItem().isLoggedOut)
            vm.logout()
            assertTrue(awaitItem().isLoggedOut)
        }

        assertEquals(1, fakeStorage.clearAllCallCount)
        assertEquals(1, fakeUserRepo.clearCount) // cached profile is dropped too
        assertEquals(1, fakeBookmarkRepo.clearCount) // and the cached bookmarks
        assertEquals(1, fakeReportRepo.clearCount) // and the reported MCQs
    }

    @Test fun logoutClearsPushTokenWhileStillSignedIn() = runTest {
        fakeStorage.putString(PrefKeys.JWT_TOKEN, "session")
        val vm = viewModel()

        vm.logout()
        advanceUntilIdle()

        assertEquals(1, fakeNotificationRepo.clearCount)
        assertEquals("session", jwtWhenPushCleared) // sent before the DataStore was wiped
        assertTrue(vm.state.value.isLoggedOut)
    }
}
