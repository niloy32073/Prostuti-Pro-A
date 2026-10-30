package com.prostuti.student.ui.screen.intro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prostuti.student.data.repository.fake.FakeDataStoreStorageRepository
import com.prostuti.student.domain.SaveBooleanToDataStoreUseCase
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun onboardingScreen_displaysFirstCardAndSkipAction() {
        composeTestRule.setContent {
            ProstutiTheme {
                OnboardingScreen(
                    onFinished = {},
                    viewModel = OnboardingViewModel(SaveBooleanToDataStoreUseCase(FakeDataStoreStorageRepository()))
                )
            }
        }

        composeTestRule.onNodeWithText("হাজারো MCQ দিয়ে প্র্যাকটিস করো").assertIsDisplayed()
        composeTestRule.onNodeWithText("স্কিপ").assertIsDisplayed()
    }

    @Test
    fun tappingSkip_invokesOnFinished() {
        var finished = false
        composeTestRule.setContent {
            ProstutiTheme {
                OnboardingScreen(
                    onFinished = { finished = true },
                    viewModel = OnboardingViewModel(SaveBooleanToDataStoreUseCase(FakeDataStoreStorageRepository()))
                )
            }
        }

        composeTestRule.onNodeWithText("স্কিপ").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) { finished }
    }
}
