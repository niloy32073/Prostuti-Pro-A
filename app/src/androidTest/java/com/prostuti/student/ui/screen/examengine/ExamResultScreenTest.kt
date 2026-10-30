package com.prostuti.student.ui.screen.examengine

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prostuti.student.data.model.enums.AttemptMode
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.MCQExamRepository
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.dsl.module

@RunWith(AndroidJUnit4::class)
class ExamResultScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun overrideRepository(fakeRepo: FakeExamAttemptRepositoryForEngineTest) {
        GlobalContext.get().loadModules(listOf(module { single<MCQExamRepository> { fakeRepo } }), allowOverride = true)
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
                ExamResultScreen(attemptId = "attempt1", titleBn = "প্র্যাকটিস এক্সাম", onViewAnswerSheet = {}, onViewLeaderboard = {}, onFinish = {})
            }
        }

        waitForText("এক্সাম শেষ!")
        composeTestRule.onNodeWithText("3").assertExists()   // right answer count tile (unique among the fixture's counts)
        composeTestRule.onAllNodesWithText("লিডারবোর্ড দেখো").fetchSemanticsNodes().let {
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
                    onViewLeaderboard = {},
                    onFinish = {}
                )
            }
        }

        waitForText("এক্সাম শেষ!")
        composeTestRule.onNodeWithText("লিডারবোর্ড দেখো").assertExists()
        composeTestRule.onNodeWithText("#2").assertExists()

        composeTestRule.onNodeWithText("অ্যান্সার শিট দেখো").performClick()
        assert(answerSheetClicked)
    }
}
