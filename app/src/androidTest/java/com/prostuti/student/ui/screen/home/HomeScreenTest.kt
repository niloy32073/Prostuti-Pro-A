package com.prostuti.student.ui.screen.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.AttemptMode
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptResultResponse
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest
import com.prostuti.student.data.repository.fake.FakeUserRepository
import com.prostuti.student.domain.GetMeUseCase
import com.prostuti.student.domain.GetResumablePracticeAttemptUseCase
import com.prostuti.student.ui.screen.home.component.CONTINUE_ATTEMPT_CARD_TAG
import com.prostuti.student.data.repository.fake.FakeAttemptProgressRepository
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

    private val userRepo = FakeUserRepository(listOf(ApiResponse.Success(FakeUserRepository.me())))

    @Test
    fun homeScreen_displaysGreetingAndAdmissionReminder() {
        composeTestRule.setContent {
            ProstutiTheme {
                HomeScreen(
                    viewModel = HomeViewModel(
                        GetResumablePracticeAttemptUseCase(FakeExamAttemptRepositoryForEngineTest(), FakeAttemptProgressRepository()),
                        GetMeUseCase(userRepo)
                    )
                )
            }
        }

        composeTestRule.onNodeWithTag(HOME_GREETING_TAG)
            .assertIsDisplayed()
            .assertTextEquals(context.getString(R.string.home_greeting, "Muwaz Ahmed"))
        composeTestRule.onNodeWithText(context.getString(R.string.home_admission_title_demo)).assertIsDisplayed()
    }

    @Test
    fun homeScreen_showsUsernameAndOpensProfileFromAvatar() {
        var profileOpened = false
        composeTestRule.setContent {
            ProstutiTheme {
                HomeScreen(
                    onNavigateToProfile = { profileOpened = true },
                    viewModel = HomeViewModel(
                        GetResumablePracticeAttemptUseCase(FakeExamAttemptRepositoryForEngineTest(), FakeAttemptProgressRepository()),
                        GetMeUseCase(userRepo)
                    )
                )
            }
        }

        composeTestRule.onNodeWithTag(HOME_USERNAME_TAG).assertTextEquals("@muwy")
        // The my-rank card in the leaderboard uses the first name too.
        composeTestRule.onNodeWithText(context.getString(R.string.home_my_rank_label, "Muwaz")).assertExists()
        composeTestRule.onNodeWithTag(HOME_PROFILE_BUTTON_TAG).performClick()
        assertEquals(true, profileOpened)
    }

    @Test
    fun homeScreen_withoutProfileShowsGreetingWithoutName() {
        val failing = FakeUserRepository(listOf(ApiResponse.Failure(message = "offline", errorCode = "NETWORK_ERROR")))
        composeTestRule.setContent {
            ProstutiTheme {
                HomeScreen(
                    viewModel = HomeViewModel(
                        GetResumablePracticeAttemptUseCase(FakeExamAttemptRepositoryForEngineTest(), FakeAttemptProgressRepository()),
                        GetMeUseCase(failing)
                    )
                )
            }
        }

        composeTestRule.onNodeWithTag(HOME_GREETING_TAG).assertTextEquals(context.getString(R.string.home_greeting_no_name))
        assertEquals(0, composeTestRule.onAllNodesWithTag(HOME_USERNAME_TAG).fetchSemanticsNodes().size)
    }

    @Test
    fun homeScreen_showsUnfinishedAttemptAndOpensIt() {
        val repo = FakeExamAttemptRepositoryForEngineTest(
            listMyAttemptsResult = ApiResponse.Success(
                listOf(
                    ExamAttemptResultResponse(
                        id = "attempt9", examId = "exam9", studentId = "s1", mode = AttemptMode.PRACTICE,
                        startAt = java.time.Instant.now().toString(), endAt = null,
                        rightAnswerCount = 0, wrongAnswerCount = 0, skipCount = 0, totalScore = 0.0
                    )
                )
            )
        )
        var opened: Pair<String, String>? = null
        composeTestRule.setContent {
            ProstutiTheme {
                HomeScreen(
                    onContinueAttempt = { id, title -> opened = id to title },
                    viewModel = HomeViewModel(GetResumablePracticeAttemptUseCase(repo, FakeAttemptProgressRepository()), GetMeUseCase(userRepo))
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag(CONTINUE_ATTEMPT_CARD_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(CONTINUE_ATTEMPT_CARD_TAG).performClick()

        // The exam lookup is not configured in the fake, so the card falls back to the generic title.
        assertEquals("attempt9" to context.getString(R.string.home_continue_exam_fallback), opened)
    }

    @Test
    fun homeScreen_hidesContinueCardWhenNothingIsUnfinished() {
        composeTestRule.setContent {
            ProstutiTheme {
                HomeScreen(
                    viewModel = HomeViewModel(
                        GetResumablePracticeAttemptUseCase(FakeExamAttemptRepositoryForEngineTest(), FakeAttemptProgressRepository()),
                        GetMeUseCase(userRepo)
                    )
                )
            }
        }
        composeTestRule.waitForIdle()
        assertEquals(0, composeTestRule.onAllNodesWithTag(CONTINUE_ATTEMPT_CARD_TAG).fetchSemanticsNodes().size)
    }

    @Test
    fun homeScreen_groupMenuFollowsTheGroupScope() {
        fun home(vararg scopes: String) = HomeViewModel(
            GetResumablePracticeAttemptUseCase(FakeExamAttemptRepositoryForEngineTest(), FakeAttemptProgressRepository()),
            GetMeUseCase(FakeUserRepository(listOf(ApiResponse.Success(FakeUserRepository.me().copy(scopes = scopes.toList())))))
        )
        var withScope by mutableStateOf(true)
        composeTestRule.setContent {
            ProstutiTheme {
                key(withScope) { HomeScreen(viewModel = if (withScope) home("group:use", "routine:use") else home("exam:read")) }
            }
        }
        val group = context.getString(R.string.qm_group)
        composeTestRule.onNodeWithText(group).assertExists()
        withScope = false
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(group).assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(R.string.qm_routine)).assertExists()
    }
}

