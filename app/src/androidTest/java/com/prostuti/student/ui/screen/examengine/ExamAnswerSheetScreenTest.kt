package com.prostuti.student.ui.screen.examengine

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prostuti.student.data.repository.ExamRepository
import com.prostuti.student.data.repository.MCQRepository
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.dsl.module

@RunWith(AndroidJUnit4::class)
class ExamAnswerSheetScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun overrideRepository(fakeRepo: FakeExamAttemptRepositoryForEngineTest) {
        GlobalContext.get().loadModules(listOf(module {
            single<ExamRepository> { fakeRepo }
            single<MCQRepository> { fakeRepo }
        }), allowOverride = true)
    }

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun filterChipsShowTheRightCountsAndSwitchingFilterNarrowsTheList() {
        overrideRepository(FakeExamAttemptRepositoryForEngineTest())

        composeTestRule.setContent {
            ProstutiTheme { ExamAnswerSheetScreen(attemptId = "attempt1", titleBn = "প্র্যাকটিস এক্সাম", onBack = {}) }
        }

        waitForText("অ্যান্সার শিট")
        waitForText("সব (3)")  // the header shows before the answer sheet + questions finish loading
        composeTestRule.onNodeWithText("সব (3)").assertExists()
        composeTestRule.onNodeWithText("সঠিক (1)").assertExists()
        composeTestRule.onNodeWithText("ভুল (1)").assertExists()
        composeTestRule.onNodeWithText("স্কিপ (1)").assertExists()

        // All three fixture questions start visible under "সব".
        composeTestRule.onNodeWithText("Q1?").assertExists()
        composeTestRule.onNodeWithText("Q2?").assertExists()
        composeTestRule.onNodeWithText("Q3?").assertExists()

        composeTestRule.onNodeWithText("সঠিক (1)").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Q1?").assertExists()
        composeTestRule.onAllNodesWithText("Q2?").fetchSemanticsNodes().let {
            assert(it.isEmpty()) { "Q2 (wrong) must be hidden once filtered to সঠিক (correct)" }
        }
    }

    @Test
    fun emptyFilterResultShowsTheNoQuestionsMessage() {
        // A fixture where nothing is skipped — filtering by "স্কিপ" should show the empty-state text.
        val sheet = FakeExamAttemptRepositoryForEngineTest.defaultAnswerSheet().let { data ->
            data.copy(questions = data.questions.map {
                if (it.result == "SKIPPED") it.copy(selectedOptionId = "o6", result = "WRONG") else it
            })
        }
        overrideRepository(
            FakeExamAttemptRepositoryForEngineTest(
                answerSheetResult = com.prostuti.student.data.model.response.ApiResponse.Success(sheet)
            )
        )

        composeTestRule.setContent {
            ProstutiTheme { ExamAnswerSheetScreen(attemptId = "attempt1", titleBn = "প্র্যাকটিস এক্সাম", onBack = {}) }
        }

        waitForText("অ্যান্সার শিট")
        composeTestRule.onNodeWithText("স্কিপ (0)").performClick()
        waitForText("এই ফিল্টারে কোনো প্রশ্ন নেই")
    }

    @Test
    fun expandingAPassageQuestionShowsItsUddipok() {
        val fake = FakeExamAttemptRepositoryForEngineTest()
        fake.questionsWithOptionsResult = com.prostuti.student.data.model.response.ApiResponse.Success(
            FakeExamAttemptRepositoryForEngineTest.defaultQuestionsWithOptions().map {
                if (it.mcqId == "mcq2") it.copy(
                    passageId = "p1",
                    passage = com.prostuti.student.data.model.response.FetchPassageData("p1", passageText = "একটি উদ্দীপকের লেখা")
                ) else it
            }
        )
        overrideRepository(fake)

        composeTestRule.setContent {
            ProstutiTheme { ExamAnswerSheetScreen(attemptId = "attempt1", titleBn = "প্র্যাকটিস এক্সাম", onBack = {}) }
        }

        waitForText("Q2?")
        composeTestRule.onNodeWithText("Q2?").performClick()
        waitForText("একটি উদ্দীপকের লেখা")
        composeTestRule.onNodeWithText("উদ্দীপক").assertExists()
    }
}
