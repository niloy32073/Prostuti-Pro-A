package com.prostuti.student.ui.screen.examengine

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.data.repository.ExamRepository
import com.prostuti.student.data.repository.MCQRepository
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Assert.assertFalse
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
        GlobalContext.get().loadModules(listOf(module { single<ExamRepository> { fakeRepo }; single<MCQRepository> { fakeRepo } }), allowOverride = true)
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
                    // Started "now" so the real clock does not find the 10-minute exam already over.
                    FakeExamAttemptRepositoryForEngineTest.defaultAttemptData(
                        questions = listOf(question),
                        startAt = java.time.Instant.now().toString()
                    )
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

    /** One pick per question: after the first answer every option of that question is locked. */
    @Test
    fun optionsAreLockedAfterTheFirstPick() {
        val question = FakeExamAttemptRepositoryForEngineTest.fakeQuestion(mcqId = "mcq1")
        overrideRepository(
            FakeExamAttemptRepositoryForEngineTest(
                attemptQuestionsResult = com.prostuti.student.data.model.response.ApiResponse.Success(
                    FakeExamAttemptRepositoryForEngineTest.defaultAttemptData(
                        questions = listOf(question),
                        startAt = java.time.Instant.now().toString()
                    )
                )
            )
        )
        composeTestRule.setContent {
            ProstutiTheme { ExamAttemptScreen(attemptId = "attempt1", onBack = {}, onSubmitted = {}) }
        }
        waitForText("0/1 উত্তর দেওয়া হয়েছে")
        composeTestRule.onNodeWithText("Option B").assertIsEnabled()

        composeTestRule.onNodeWithText("Option A").performClick()
        waitForText("1/1 উত্তর দেওয়া হয়েছে")

        composeTestRule.onNodeWithText("Option A").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Option B").assertIsNotEnabled()
    }

    /** An exam whose time ran out is submitted on its own; the student sees why and moves on
     *  to the result only from the dialog. */
    @Test
    fun timeUpShowsTheDialogAndOpensTheResultFromIt() {
        overrideRepository(
            FakeExamAttemptRepositoryForEngineTest(
                attemptQuestionsResult = com.prostuti.student.data.model.response.ApiResponse.Success(
                    FakeExamAttemptRepositoryForEngineTest.defaultAttemptData(
                        questions = listOf(FakeExamAttemptRepositoryForEngineTest.fakeQuestion(mcqId = "mcq1")),
                        mode = com.prostuti.student.data.model.enums.AttemptMode.LIVE,
                        // Started 11 minutes ago — the 10-minute exam is already over.
                        startAt = java.time.Instant.now().minusSeconds(11 * 60).toString()
                    )
                )
            )
        )
        var resultOpened = false
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val viewResult = context.getString(R.string.exam_time_up_view_result)

        composeTestRule.setContent {
            ProstutiTheme { ExamAttemptScreen(attemptId = "attempt1", onBack = {}, onSubmitted = { resultOpened = true }) }
        }

        waitForText(context.getString(R.string.exam_time_up_title))
        waitForText(viewResult)
        assertFalse(resultOpened) // waits for the student instead of jumping away

        composeTestRule.onNodeWithText(viewResult).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) { resultOpened }
    }
}
