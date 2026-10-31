package com.prostuti.student.ui.screen.qb

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prostuti.student.data.model.enums.ExamTypes
import com.prostuti.student.data.model.enums.Level
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.MCQExam
import com.prostuti.student.data.repository.fake.FakeQbMcqExamRepository
import com.prostuti.student.domain.StartOrResumeExamAttemptUseCase
import com.prostuti.student.domain.FetchMCQExamsUseCase
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The viewModel param is overridden with a fake-repo-backed real ViewModel instead of going
 * through Koin, so this test never touches the real (network-backed) MCQExamRepository that's
 * wired into the app's Koin graph. */
@RunWith(AndroidJUnit4::class)
class MCQExamListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun fakeExam(id: String, titleBn: String, examType: ExamTypes) = MCQExam(
        id = id, duration = 60, level = Level.ACADEMIC, examType = examType, examYear = 2024,
        markPerQuestion = 1.0, negativeMarkPerWrongAnswer = 0.25, questionCount = 10,
        showExplanationAfter = "submission", shuffleOptions = false, shuffleQuestions = false,
        titleBn = titleBn, titleEn = titleBn
    )

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun rendersBoardExamsOnTheDefaultTab() {
        val repo = FakeQbMcqExamRepository(fetchResult = { type ->
            if (type == ExamTypes.BOARD) ApiResponse.Success(listOf(fakeExam("b1", "বোর্ড মডেল টেস্ট ১", type)))
            else ApiResponse.Success(listOf(fakeExam("c1", "কলেজ মডেল টেস্ট ১", type)))
        })
        composeTestRule.setContent {
            ProstutiTheme {
                MCQExamListScreen(
                    subjectId = "phy",
                    nameBn = "পদার্থবিজ্ঞান",
                    nameEn = "Physics",
                    onBack = {},
                    onNavigateToExamEngine = {},
                    onNavigateToReader = {},
                    onNavigateToChapterBrowse = {},
                    viewModel = MCQExamListViewModel("phy", FetchMCQExamsUseCase(repo), StartOrResumeExamAttemptUseCase(repo))
                )
            }
        }

        waitForText("বোর্ড মডেল টেস্ট ১")
        composeTestRule.onAllNodesWithText("কলেজ মডেল টেস্ট ১").fetchSemanticsNodes().let {
            assert(it.isEmpty()) { "college exam shouldn't be visible while the BOARD tab is selected" }
        }
    }

    @Test
    fun loadFailureSurfacesTheErrorMessageInTheErrorOverlay() {
        val repo = FakeQbMcqExamRepository(fetchResult = { ApiResponse.Failure("লোড করা যায়নি", "LOAD_FAIL") })
        composeTestRule.setContent {
            ProstutiTheme {
                MCQExamListScreen(
                    subjectId = "phy",
                    nameBn = "পদার্থবিজ্ঞান",
                    nameEn = "Physics",
                    onBack = {},
                    onNavigateToExamEngine = {},
                    onNavigateToReader = {},
                    onNavigateToChapterBrowse = {},
                    viewModel = MCQExamListViewModel("phy", FetchMCQExamsUseCase(repo), StartOrResumeExamAttemptUseCase(repo))
                )
            }
        }

        waitForText("লোড করা যায়নি")
    }

    @Test
    fun searchFieldPlaceholderIsVisible() {
        val repo = FakeQbMcqExamRepository()
        composeTestRule.setContent {
            ProstutiTheme {
                MCQExamListScreen(
                    subjectId = "phy",
                    nameBn = "পদার্থবিজ্ঞান",
                    nameEn = "Physics",
                    onBack = {},
                    onNavigateToExamEngine = {},
                    onNavigateToReader = {},
                    onNavigateToChapterBrowse = {},
                    viewModel = MCQExamListViewModel("phy", FetchMCQExamsUseCase(repo), StartOrResumeExamAttemptUseCase(repo))
                )
            }
        }

        composeTestRule.onNodeWithText("বোর্ড, কলেজ বা মডেল টেস্টের নাম খুঁজো...").assertExists()
    }
}
