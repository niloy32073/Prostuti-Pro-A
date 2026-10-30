package com.prostuti.student.ui.screen.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.UserStatus
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.VerifyEmailResponse
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthVerifyOtpScreenTest {

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
    fun displaysTitleAndSendsOtpOnEntry() {
        composeTestRule.setContent {
            ProstutiTheme {
                AuthVerifyOtpScreen(identifier = "test@example.com", onBack = {}, onVerified = {})
            }
        }
        waitForText(context.getString(R.string.auth_verify_otp_title))
        composeTestRule.waitUntil(timeoutMillis = 5_000) { override.fakeRepository.sendOtpCallCount >= 1 }
    }

    @Test
    fun verifyingCorrectCodeInvokesOnVerified() {
        override.fakeRepository.verifyEmailResult = ApiResponse.Success(VerifyEmailResponse(UserStatus.ACTIVE, isEmailVerified = true))
        var verified = false
        composeTestRule.setContent {
            ProstutiTheme {
                AuthVerifyOtpScreen(identifier = "test@example.com", onBack = {}, onVerified = { verified = true })
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.otp_placeholder)).performTextInput("123456")
        composeTestRule.onNodeWithText(context.getString(R.string.otp_verify_action)).performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) { verified }
    }
}
