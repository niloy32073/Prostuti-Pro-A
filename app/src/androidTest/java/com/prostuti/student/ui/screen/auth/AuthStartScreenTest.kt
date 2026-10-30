package com.prostuti.student.ui.screen.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.CheckUserResponse
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthStartScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val override = AuthKoinOverride()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        override.install()
    }

    @After
    fun teardown() {
        override.restore()
    }

    private fun setScreen() {
        composeTestRule.setContent {
            ProstutiTheme {
                AuthStartScreen(onNavigateToLogin = { _, _ -> }, onNavigateToSignup = {})
            }
        }
    }

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun displaysTitleAndContinueButton() {
        setScreen()
        waitForText(context.getString(R.string.auth_start_title))
        composeTestRule.onNodeWithText(context.getString(R.string.continue_action)).assertExists()
    }

    @Test
    fun enteringIdentifierAndContinuingWithExistingAccountNavigatesToLogin() {
        override.fakeRepository.checkUserResult = ApiResponse.Success(CheckUserResponse(exists = true, firstName = "Rahim"))
        var navigatedIdentifier: String? = null
        composeTestRule.setContent {
            ProstutiTheme {
                AuthStartScreen(
                    onNavigateToLogin = { identifier, _ -> navigatedIdentifier = identifier },
                    onNavigateToSignup = {}
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.email_or_phone_placeholder))
            .performTextInput("test@example.com")
        composeTestRule.onNodeWithText(context.getString(R.string.continue_action)).performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) { navigatedIdentifier != null }
    }
}
