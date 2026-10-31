package com.prostuti.student.ui.screen.examengine

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class ExamLeaderboardScreenTest {
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
    fun entriesAreShownInServerRankOrderWithUsernameAndScore() {
        overrideRepository(FakeExamAttemptRepositoryForEngineTest())

        composeTestRule.setContent {
            ProstutiTheme { ExamLeaderboardScreen(examId = "exam1", titleBn = "লাইভ এক্সাম", onBack = {}) }
        }

        waitForText("লিডারবোর্ড")
        composeTestRule.onNodeWithText("#1").assertExists()
        composeTestRule.onNodeWithText("Alice").assertExists()
        composeTestRule.onNodeWithText("9.50").assertExists()
        composeTestRule.onNodeWithText("#3").assertExists()
        composeTestRule.onNodeWithText("Carol").assertExists()
    }

    @Test
    fun a404IsShownAsNotReadyYetMessage_notAsAnErrorCard() {
        overrideRepository(
            FakeExamAttemptRepositoryForEngineTest(
                getExamLeaderboardResult = ApiResponse.Failure(message = "Not found", errorCode = "404")
            )
        )

        composeTestRule.setContent {
            ProstutiTheme { ExamLeaderboardScreen(examId = "exam1", titleBn = "লাইভ এক্সাম", onBack = {}) }
        }

        waitForText("লিডারবোর্ড এখনো প্রস্তুত হয়নি — এক্সাম শেষ হওয়ার পর কিছুক্ষণের মধ্যে দেখা যাবে")
    }
}
