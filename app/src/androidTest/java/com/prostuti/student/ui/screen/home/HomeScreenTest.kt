package com.prostuti.student.ui.screen.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_displaysGreetingAndAdmissionReminder() {
        composeTestRule.setContent {
            ProstutiTheme {
                HomeScreen(viewModel = HomeViewModel())
            }
        }

        composeTestRule.onNodeWithText("হ্যালো, নিফা 👋").assertIsDisplayed()
        composeTestRule.onNodeWithText("ঢাবি ক-ইউনিট ভর্তি ফরম").assertIsDisplayed()
    }
}
