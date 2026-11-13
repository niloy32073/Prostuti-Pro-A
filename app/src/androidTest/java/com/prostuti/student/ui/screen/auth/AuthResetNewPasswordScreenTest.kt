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
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthResetNewPasswordScreenTest {

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
    fun displaysTitleAndAction() {
        composeTestRule.setContent {
            ProstutiTheme {
                AuthResetNewPasswordScreen(identifier = "test@example.com", code = "123456", onBack = {}, onPasswordReset = {})
            }
        }
        waitForText(context.getString(R.string.auth_reset_new_password_title))
        composeTestRule.onNodeWithText(context.getString(R.string.auth_reset_password_action)).assertExists()
    }

    @Test
    fun resettingWithMismatchedPasswordsShowsErrorAndDoesNotInvokeCallback() {
        var resetDone = false
        composeTestRule.setContent {
            ProstutiTheme {
                AuthResetNewPasswordScreen(identifier = "test@example.com", code = "123456", onBack = {}, onPasswordReset = { resetDone = true })
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.new_password_placeholder)).performTextInput("Password1")
        composeTestRule.onNodeWithText(context.getString(R.string.confirm_password_placeholder)).performTextInput("Password2")
        composeTestRule.onNodeWithText(context.getString(R.string.auth_reset_password_action)).performClick()

        waitForText(context.getString(R.string.error_passwords_dont_match))
        assert(!resetDone)
    }

    @Test
    fun resettingWithMatchingValidPasswordsInvokesOnPasswordReset() {
        override.fakeRepository.resetPasswordResult = ApiResponse.Success(Unit)
        var resetDone = false
        composeTestRule.setContent {
            ProstutiTheme {
                AuthResetNewPasswordScreen(identifier = "test@example.com", code = "123456", onBack = {}, onPasswordReset = { resetDone = true })
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.new_password_placeholder)).performTextInput("Password1")
        composeTestRule.onNodeWithText(context.getString(R.string.confirm_password_placeholder)).performTextInput("Password1")
        composeTestRule.onNodeWithText(context.getString(R.string.auth_reset_password_action)).performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) { resetDone }
    }
}
