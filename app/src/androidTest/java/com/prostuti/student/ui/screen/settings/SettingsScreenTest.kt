package com.prostuti.student.ui.screen.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.data.repository.fake.FakeDataStoreStorageRepository
import com.prostuti.student.data.repository.fake.FakeBookmarkRepository
import com.prostuti.student.data.repository.fake.FakeUserRepository
import com.prostuti.student.domain.ClearCachedMeUseCase
import com.prostuti.student.domain.ClearDataStoreUseCase
import com.prostuti.student.domain.ClearBookmarksUseCase
import com.prostuti.student.domain.ClearReportsUseCase
import com.prostuti.student.data.repository.fake.FakeReportRepository
import com.prostuti.student.data.repository.fake.FakeNotificationRepository
import com.prostuti.student.domain.notification.ClearPushTokenUseCase
import com.prostuti.student.domain.GetMeUseCase
import com.prostuti.student.domain.ObserveThemeModeUseCase
import com.prostuti.student.domain.SetThemeModeUseCase
import com.prostuti.student.data.repository.fake.FakeThemeRepository
import com.prostuti.student.data.model.enums.ThemeMode
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import com.prostuti.student.ui.screen.profile.PROFILE_NAME_TAG
import com.prostuti.student.ui.screen.profile.ProfileUiState
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // The default `values/` strings are Bangla and `values-en/` English; a plain test activity
    // follows the device locale (MainActivity forces Bangla), so resolve text from resources.
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun settingsViewModel(theme: FakeThemeRepository = FakeThemeRepository()) = SettingsViewModel(
        ClearDataStoreUseCase(FakeDataStoreStorageRepository()),
        ClearCachedMeUseCase(FakeUserRepository()),
        ClearBookmarksUseCase(FakeBookmarkRepository()),
        ClearReportsUseCase(FakeReportRepository()),
        GetMeUseCase(FakeUserRepository()),
        ObserveThemeModeUseCase(theme),
        SetThemeModeUseCase(theme),
        ClearPushTokenUseCase(FakeNotificationRepository())
    )

    @Test
    fun settingsScreen_displaysTitleAndLogoutButton() {
        composeTestRule.setContent {
            ProstutiTheme {
                SettingsScreen(
                    onLoggedOut = {},
                    onOpenBookmarks = {},
                    onOpenExamHistory = {},
                    viewModel = settingsViewModel()
                )
            }
        }

        // Compact windows title the page "Settings"; medium and wider show the Account panes.
        composeTestRule.onAllNodes(hasText(context.getString(R.string.settings_title)) or hasText(context.getString(R.string.nav_account)))
            .onFirst().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.settings_logout)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun tappingLogout_invokesOnLoggedOut() {
        var loggedOut = false
        composeTestRule.setContent {
            ProstutiTheme {
                SettingsScreen(
                    onLoggedOut = { loggedOut = true },
                    onOpenBookmarks = {},
                    onOpenExamHistory = {},
                    viewModel = settingsViewModel()
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.settings_logout)).performScrollTo().performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) { loggedOut }
    }

    @Test
    fun examHistoryRow_opensExamHistory() {
        var opened = false
        composeTestRule.setContent {
            ProstutiTheme {
                SettingsScreen(
                    onLoggedOut = {},
                    onOpenBookmarks = {},
                    onOpenExamHistory = { opened = true },
                    viewModel = settingsViewModel()
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.settings_section_activity)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.settings_exam_history)).performClick()
        composeTestRule.waitForIdle()
        assert(opened)
    }

    @Test
    fun themeSegmentSavesTheChoice() {
        val theme = FakeThemeRepository()
        composeTestRule.setContent {
            ProstutiTheme {
                SettingsScreen(onLoggedOut = {}, onOpenBookmarks = {}, onOpenExamHistory = {}, viewModel = settingsViewModel(theme))
            }
        }

        composeTestRule.onNodeWithTag(SETTINGS_THEME_TAG + "DARK").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(ThemeMode.DARK, theme.mode.value)
    }

    private fun setAccountContent(panes: Int, profile: ProfileUiState = ProfileUiState(profile = FakeUserRepository.me())) {
        composeTestRule.setContent {
            ProstutiTheme {
                SettingsContent(
                    state = SettingsUiState(userName = "Muwaz"),
                    language = "bn",
                    onOpenProfile = {}, onOpenPlayground = {}, onOpenExamHistory = {}, onOpenBookmarks = {},
                    onLanguage = {}, onThemeMode = {}, onHaptics = {}, onSound = {}, onLogout = {},
                    panes = panes,
                    profile = profile
                )
            }
        }
    }

    @Test
    fun onePane_showsProfileCardAndNoProfilePane() {
        setAccountContent(panes = 1)

        composeTestRule.onNodeWithTag(SETTINGS_PROFILE_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SETTINGS_PROFILE_PANE_TAG).assertDoesNotExist()
    }

    @Test
    fun twoPanes_showProfileBesideSettings() {
        setAccountContent(panes = 2)

        composeTestRule.onAllNodesWithText(context.getString(R.string.nav_account)).onFirst().assertIsDisplayed()
        composeTestRule.onNodeWithTag(SETTINGS_PROFILE_PANE_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PROFILE_NAME_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SETTINGS_PROFILE_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(SETTINGS_EXAM_HISTORY_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SETTINGS_LOGOUT_TAG).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun threePanes_showProfileActivityAndPreferences() {
        setAccountContent(panes = 3)

        composeTestRule.onNodeWithTag(PROFILE_NAME_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SETTINGS_EXAM_HISTORY_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SETTINGS_THEME_TAG + "DARK").assertIsDisplayed()
        composeTestRule.onNodeWithTag(SETTINGS_PROFILE_TAG).assertDoesNotExist()
    }

    @Test
    fun profilePane_withNothingCached_showsRetry() {
        var retried = false
        composeTestRule.setContent {
            ProstutiTheme {
                SettingsContent(
                    state = SettingsUiState(),
                    language = "bn",
                    onOpenProfile = {}, onOpenPlayground = {}, onOpenExamHistory = {}, onOpenBookmarks = {},
                    onLanguage = {}, onThemeMode = {}, onHaptics = {}, onSound = {}, onLogout = {},
                    panes = 2,
                    profile = ProfileUiState(),
                    onProfileRefresh = { retried = true }
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.profile_retry)).performClick()
        assert(retried)
    }
}
