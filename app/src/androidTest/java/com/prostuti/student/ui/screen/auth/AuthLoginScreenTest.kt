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
import com.prostuti.student.data.repository.fake.fakeLogInResponse
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthLoginScreenTest {

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

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun displaysWelcomeWithFirstNameWhenProvided() {
        composeTestRule.setContent {
            ProstutiTheme {
                AuthLoginScreen(
                    identifier = "test@example.com", firstName = "Rahim",
                    onBack = {}, onForgotPassword = {}, onSignedIn = {}
                )
            }
        }
        waitForText(context.getString(R.string.auth_login_welcome_named, "Rahim"))
    }

    @Test
    fun displaysGenericWelcomeWhenFirstNameIsNull() {
        composeTestRule.setContent {
            ProstutiTheme {
                AuthLoginScreen(
                    identifier = "test@example.com", firstName = null,
                    onBack = {}, onForgotPassword = {}, onSignedIn = {}
                )
            }
        }
        waitForText(context.getString(R.string.auth_login_welcome))
    }

    @Test
    fun signingInSuccessfullyInvokesOnSignedIn() {
        override.fakeRepository.signInResult = ApiResponse.Success(fakeLogInResponse())
        var signedIn = false
        composeTestRule.setContent {
            ProstutiTheme {
                AuthLoginScreen(
                    identifier = "test@example.com", firstName = "Rahim",
                    onBack = {}, onForgotPassword = {}, onSignedIn = { signedIn = true }
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.password_placeholder)).performTextInput("password123")
        composeTestRule.onNodeWithText(context.getString(R.string.continue_action)).performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) { signedIn }
    }
}
