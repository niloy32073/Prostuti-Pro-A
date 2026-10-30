package com.prostuti.student.ui.screen.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.data.repository.fake.FakeDataStoreStorageRepository
import com.prostuti.student.domain.ClearDataStoreUseCase
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

    @Test
    fun settingsScreen_displaysTitleAndLogoutButton() {
        composeTestRule.setContent {
            ProstutiTheme {
                SettingsScreen(
                    onLoggedOut = {},
                    viewModel = SettingsViewModel(ClearDataStoreUseCase(FakeDataStoreStorageRepository()))
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.settings_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.settings_logout)).assertIsDisplayed()
    }

    @Test
    fun tappingLogout_invokesOnLoggedOut() {
        var loggedOut = false
        composeTestRule.setContent {
            ProstutiTheme {
                SettingsScreen(
                    onLoggedOut = { loggedOut = true },
                    viewModel = SettingsViewModel(ClearDataStoreUseCase(FakeDataStoreStorageRepository()))
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.settings_logout)).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) { loggedOut }
    }
}
