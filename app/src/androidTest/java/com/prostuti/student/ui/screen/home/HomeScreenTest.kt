package com.prostuti.student.ui.screen.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // The default `values/` strings are Bangla and `values-en/` English; a plain test activity
    // follows the device locale (MainActivity forces Bangla), so resolve text from resources.
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun homeScreen_displaysGreetingAndAdmissionReminder() {
        composeTestRule.setContent {
            ProstutiTheme {
                HomeScreen(viewModel = HomeViewModel())
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.home_greeting_demo)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.home_admission_title_demo)).assertIsDisplayed()
    }
}
