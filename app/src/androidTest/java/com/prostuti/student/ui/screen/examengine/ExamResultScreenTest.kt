package com.prostuti.student.ui.screen.examengine

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prostuti.student.data.model.enums.AttemptMode
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.ExamRepository
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.dsl.module
import com.prostuti.student.R
import androidx.test.platform.app.InstrumentationRegistry

@RunWith(AndroidJUnit4::class)
class ExamResultScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // Texts come from resources: values/ is Bangla, values-en/ English, the test follows the device locale.
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun overrideRepository(fakeRepo: FakeExamAttemptRepositoryForEngineTest) {
        GlobalContext.get().loadModules(listOf(module { single<ExamRepository> { fakeRepo } }), allowOverride = true)
    }

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun practiceModeResult_showsScoreTilesButNotLeaderboardOrRank() {
        val practiceResult = FakeExamAttemptRepositoryForEngineTest.defaultResult().copy(mode = AttemptMode.PRACTICE, rank = null)
        overrideRepository(FakeExamAttemptRepositoryForEngineTest(getAttemptResult = ApiResponse.Success(practiceResult)))

        composeTestRule.setContent {
            ProstutiTheme {
                ExamResultScreen(attemptId = "attempt1", titleBn = "প্র্যাকটিস এক্সাম", onViewAnswerSheet = {}, onViewAnalysis = {}, onViewLeaderboard = {}, onFinish = {})
            }
        }

        waitForText(context.getString(R.string.result_finished_title))
        composeTestRule.onNodeWithText("3").assertExists()   // right answer count tile (unique among the fixture's counts)
        composeTestRule.onAllNodesWithText(context.getString(R.string.result_view_leaderboard)).fetchSemanticsNodes().let {
            assert(it.isEmpty()) { "Leaderboard button must not show for a PRACTICE attempt" }
        }
    }

    @Test
    fun liveModeResult_showsLeaderboardButtonAndRank_andViewAnswerSheetTriggersCallback() {
        val liveResult = FakeExamAttemptRepositoryForEngineTest.defaultResult().copy(mode = AttemptMode.LIVE, rank = 2)
        overrideRepository(FakeExamAttemptRepositoryForEngineTest(getAttemptResult = ApiResponse.Success(liveResult)))
        var answerSheetClicked = false

        composeTestRule.setContent {
            ProstutiTheme {
                ExamResultScreen(
                    attemptId = "attempt1",
                    titleBn = "লাইভ এক্সাম",
                    onViewAnswerSheet = { answerSheetClicked = true },
                    onViewAnalysis = {},
                    onViewLeaderboard = {},
                    onFinish = {}
                )
            }
        }

        waitForText(context.getString(R.string.result_finished_title))
        composeTestRule.onNodeWithText(context.getString(R.string.result_view_leaderboard)).assertExists()
        composeTestRule.onNodeWithText("#2").assertExists()

        composeTestRule.onNodeWithText(context.getString(R.string.result_view_answer_sheet)).performClick()
        assert(answerSheetClicked)
    }

    @Test
    fun fromHistory_showsRetakeInsteadOfHome_andRetakeHandsOverTheNewAttempt() {
        overrideRepository(FakeExamAttemptRepositoryForEngineTest())
        var startedAttemptId: String? = null
        var analysisClicked = false

        composeTestRule.setContent {
            ProstutiTheme {
                ExamResultScreen(
                    attemptId = "attempt1",
                    titleBn = "প্র্যাকটিস এক্সাম",
                    fromHistory = true,
                    onViewAnswerSheet = {},
                    onViewAnalysis = { analysisClicked = true },
                    onViewLeaderboard = {},
                    onRetakeStarted = { startedAttemptId = it.attemptId },
                    onFinish = {}
                )
            }
        }

        waitForText(context.getString(R.string.result_title))
        composeTestRule.onAllNodesWithText(context.getString(R.string.result_go_home)).fetchSemanticsNodes().let { assert(it.isEmpty()) }
        composeTestRule.onNodeWithTag(RESULT_ANALYSIS_BUTTON_TAG).performClick()
        assert(analysisClicked)
        composeTestRule.onNodeWithTag(RESULT_RETAKE_BUTTON_TAG).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) { startedAttemptId != null }
        assert(startedAttemptId == "attempt1")
    }

    @Test
    fun paneList_replacesTheBigButtons_andMarksTheOpenView() {
        val liveResult = FakeExamAttemptRepositoryForEngineTest.defaultResult().copy(mode = AttemptMode.LIVE, rank = 2)
        overrideRepository(FakeExamAttemptRepositoryForEngineTest(getAttemptResult = ApiResponse.Success(liveResult)))
        var leaderboardClicked = false

        composeTestRule.setContent {
            ProstutiTheme {
                ExamResultScreen(
                    attemptId = "attempt1",
                    titleBn = "লাইভ এক্সাম",
                    onViewAnswerSheet = {},
                    onViewAnalysis = {},
                    onViewLeaderboard = { leaderboardClicked = true },
                    onFinish = {},
                    showPaneList = true,
                    selectedPane = ResultPane.ANALYSIS
                )
            }
        }

        waitForText(context.getString(R.string.result_finished_title))
        composeTestRule.onAllNodesWithText(context.getString(R.string.result_view_answer_sheet)).fetchSemanticsNodes().let {
            assert(it.isEmpty()) { "The pane list replaces the full-width buttons" }
        }
        composeTestRule.onNodeWithTag(RESULT_ANALYSIS_BUTTON_TAG).performScrollTo().assertIsSelected()
        composeTestRule.onNodeWithTag(RESULT_ANSWER_SHEET_ROW_TAG).assertIsNotSelected()
        composeTestRule.onNodeWithTag(RESULT_LEADERBOARD_ROW_TAG).performScrollTo().performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) { leaderboardClicked }
    }

    @Test
    fun paneList_practiceAttempt_hasNoLeaderboardRow() {
        val practiceResult = FakeExamAttemptRepositoryForEngineTest.defaultResult().copy(mode = AttemptMode.PRACTICE, rank = null)
        overrideRepository(FakeExamAttemptRepositoryForEngineTest(getAttemptResult = ApiResponse.Success(practiceResult)))

        composeTestRule.setContent {
            ProstutiTheme {
                ExamResultScreen(
                    attemptId = "attempt1", titleBn = "প্র্যাকটিস এক্সাম",
                    onViewAnswerSheet = {}, onViewAnalysis = {}, onViewLeaderboard = {}, onFinish = {},
                    showPaneList = true
                )
            }
        }

        waitForText(context.getString(R.string.result_finished_title))
        composeTestRule.onNodeWithTag(RESULT_ANSWER_SHEET_ROW_TAG).performScrollTo().assertIsNotSelected()
        composeTestRule.onNodeWithTag(RESULT_LEADERBOARD_ROW_TAG).assertDoesNotExist()
    }
}
