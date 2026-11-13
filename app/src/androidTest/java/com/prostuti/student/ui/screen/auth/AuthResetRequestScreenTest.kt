package com.prostuti.student.ui.screen.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthResetRequestScreenTest {

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
    fun initiallyShowsSendCodeStep() {
        composeTestRule.setContent {
            ProstutiTheme {
                AuthResetRequestScreen(identifier = "test@example.com", onBack = {}, onCodeEntered = {})
            }
        }
        waitForText(context.getString(R.string.auth_forgot_password))
        composeTestRule.onNodeWithText(context.getString(R.string.auth_send_code)).assertExists()
    }

    @Test
    fun afterSendingCodeShowsOtpStepThenEnteringCodeHandsItOn() {
        var receivedCode: String? = null
        composeTestRule.setContent {
            ProstutiTheme {
                AuthResetRequestScreen(identifier = "test@example.com", onBack = {}, onCodeEntered = { receivedCode = it })
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.auth_send_code)).performClick()
        waitForText(context.getString(R.string.otp_verify_action))

        composeTestRule.onNodeWithText(context.getString(R.string.otp_placeholder)).performTextInput("123456")
        composeTestRule.onNodeWithText(context.getString(R.string.otp_verify_action)).performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) { receivedCode == "123456" }
    }
}
