package com.prostuti.student.ui.screen.examengine

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prostuti.student.data.repository.MCQExamRepository
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.dsl.module

/** Selecting an MCQ option must update the "answered" count immediately and locally — it must
 * not wait for the best-effort background sync of the response to the server. */
@RunWith(AndroidJUnit4::class)
class ExamAttemptScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun drainAnyLeftoverStash() {
        ExamAttemptStore.consume()
    }

    private fun overrideRepository(fakeRepo: FakeExamAttemptRepositoryForEngineTest) {
        GlobalContext.get().loadModules(listOf(module { single<MCQExamRepository> { fakeRepo } }), allowOverride = true)
    }

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun selectingAnOptionUpdatesTheAnsweredCount() {
        val question = FakeExamAttemptRepositoryForEngineTest.fakeQuestion(mcqId = "mcq1")
        overrideRepository(
            FakeExamAttemptRepositoryForEngineTest(
                attemptQuestionsResult = com.prostuti.student.data.model.response.ApiResponse.Success(
                    FakeExamAttemptRepositoryForEngineTest.defaultAttemptData(questions = listOf(question))
                )
            )
        )

        composeTestRule.setContent {
            ProstutiTheme { ExamAttemptScreen(attemptId = "attempt1", onBack = {}, onSubmitted = {}) }
        }

        waitForText("0/1 উত্তর দেওয়া হয়েছে")

        composeTestRule.onNodeWithText("Option A").performClick()

        waitForText("1/1 উত্তর দেওয়া হয়েছে")
    }
}
