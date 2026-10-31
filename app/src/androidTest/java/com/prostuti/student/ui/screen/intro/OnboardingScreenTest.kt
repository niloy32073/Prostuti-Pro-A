package com.prostuti.student.ui.screen.intro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
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

    // The default `values/` strings are Bangla and `values-en/` English; a plain test activity
    // follows the device locale (MainActivity forces Bangla), so resolve text from resources.
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

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

        composeTestRule.onNodeWithText(context.getString(R.string.onboarding_card1_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.onboarding_skip)).assertIsDisplayed()
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

        composeTestRule.onNodeWithText(context.getString(R.string.onboarding_skip)).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) { finished }
    }

    @Test
    fun carousel_showsLastCardBeforeFirst_andNextCardAfter() {
        composeTestRule.setContent {
            ProstutiTheme {
                OnboardingContent(cards = carouselCards, onFinish = {}, visibleCount = 3)
            }
        }

        // The ring wraps: the first card sits in the middle, the last card on its left, the second on its right.
        composeTestRule.onNodeWithText(context.getString(R.string.onboarding_card1_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.onboarding_card3_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.onboarding_card2_title)).assertIsDisplayed()
    }

    @Test
    fun carousel_nextFromSecondToLast_showsStartButton() {
        composeTestRule.setContent {
            ProstutiTheme {
                OnboardingContent(cards = carouselCards, onFinish = {}, visibleCount = 3)
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.onboarding_next)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.onboarding_next)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.onboarding_start)).assertIsDisplayed()
    }

    private val carouselCards = listOf(
        OnboardingCard(R.string.onboarding_card1_title, R.string.onboarding_card1_desc, ContainerRole.PRIMARY),
        OnboardingCard(R.string.onboarding_card2_title, R.string.onboarding_card2_desc, ContainerRole.SECONDARY),
        OnboardingCard(R.string.onboarding_card3_title, R.string.onboarding_card3_desc, ContainerRole.TERTIARY)
    )
}
