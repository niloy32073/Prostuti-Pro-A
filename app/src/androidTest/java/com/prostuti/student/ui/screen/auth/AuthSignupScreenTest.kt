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
import com.prostuti.student.data.model.response.PasswordBasedSignUpResponse
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthSignupScreenTest {

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

    private fun setScreen(onSignedUp: (String) -> Unit = {}) {
        composeTestRule.setContent {
            ProstutiTheme {
                AuthSignupScreen(identifier = "test@example.com", onBack = {}, onSignedUp = onSignedUp)
            }
        }
    }

    @Test
    fun displaysTitleAndSignupAction() {
        setScreen()
        waitForText(context.getString(R.string.auth_signup_title))
        composeTestRule.onNodeWithText(context.getString(R.string.auth_signup_action)).assertExists()
    }

    @Test
    fun signingUpSuccessfullyInvokesOnSignedUpWithIdentifier() {
        override.fakeRepository.signUpResult = ApiResponse.Success(
            PasswordBasedSignUpResponse(userId = 1, userName = "rahimislam", status = UserStatus.PENDING)
        )
        var signedUpIdentifier: String? = null
        setScreen(onSignedUp = { signedUpIdentifier = it })

        composeTestRule.onNodeWithText(context.getString(R.string.first_name_placeholder)).performTextInput("Rahim")
        composeTestRule.onNodeWithText(context.getString(R.string.last_name_placeholder)).performTextInput("Islam")
        composeTestRule.onNodeWithText(context.getString(R.string.new_password_placeholder)).performTextInput("password1")
        composeTestRule.onNodeWithText(context.getString(R.string.auth_signup_action)).performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) { signedUpIdentifier == "test@example.com" }
    }
}
