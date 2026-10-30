package com.prostuti.student.ui.screen.qb

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.data.repository.fake.FakeQbCurriculumRepository
import com.prostuti.student.domain.FetchSubjectsUseCase
import com.prostuti.student.navigation.SubjectType
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The viewModel param is overridden directly with a fake-repo-backed real ViewModel instead of
 * going through Koin, so this test never touches the real (network-backed) CurriculumRepository
 * that's wired into the app's Koin graph. */
@RunWith(AndroidJUnit4::class)
class SubjectListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun fakeSubject(id: String, nameBn: String) = Subject(
        id = id, nameBn = nameBn, nameEn = nameBn, createdAt = "2025-01-01", createdBy = "admin"
    )

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun rendersEverySubjectFromTheViewModel() {
        val repo = FakeQbCurriculumRepository(
            subjectsResult = ApiResponse.Success(listOf(fakeSubject("s1", "পদার্থবিজ্ঞান"), fakeSubject("s2", "রসায়ন")))
        )
        composeTestRule.setContent {
            ProstutiTheme {
                SubjectListScreen(
                    onNavigate = {},
                    viewModel = SubjectListViewModel(FetchSubjectsUseCase(repo))
                )
            }
        }

        waitForText("পদার্থবিজ্ঞান")
        composeTestRule.onNodeWithText("রসায়ন").assertExists()
    }

    @Test
    fun tappingASubjectNavigatesWithItsId() {
        val repo = FakeQbCurriculumRepository(
            subjectsResult = ApiResponse.Success(listOf(fakeSubject("s1", "পদার্থবিজ্ঞান")))
        )
        var navigated: SubjectType? = null
        composeTestRule.setContent {
            ProstutiTheme {
                SubjectListScreen(
                    onNavigate = { navigated = it },
                    viewModel = SubjectListViewModel(FetchSubjectsUseCase(repo))
                )
            }
        }

        waitForText("পদার্থবিজ্ঞান")
        composeTestRule.onNodeWithText("পদার্থবিজ্ঞান").performClick()

        assert(navigated?.subjectId == "s1") { "expected navigation to subjectId=s1, got $navigated" }
    }
}
