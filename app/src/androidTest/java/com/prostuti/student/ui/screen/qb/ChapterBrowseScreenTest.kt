package com.prostuti.student.ui.screen.qb

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prostuti.student.data.repository.fake.FakeBookmarkRepository
import com.prostuti.student.domain.AddBookmarkUseCase
import com.prostuti.student.domain.ObserveBookmarkedIdsUseCase
import com.prostuti.student.domain.RemoveBookmarkUseCase
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.prostuti.student.data.model.response.CurriculumName
import androidx.test.platform.app.InstrumentationRegistry

/** ChapterBrowseViewModel's questions are still dummy data (see its TODOs); only its bookmark
 * hearts need a repository, so this test builds it directly with a fake one. */
@RunWith(AndroidJUnit4::class)
class ChapterBrowseScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val bookmarkRepo = FakeBookmarkRepository()

    // Titles show in the device's language: Bangla in Bangla, English otherwise.
    private fun shown(bn: String, en: String): String =
        CurriculumName(bn, en).inLanguage(InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration.locales[0].language)

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun rendersTheFirstChapterAndTitle() {
        composeTestRule.setContent {
            ProstutiTheme {
                ChapterBrowseScreen(
                    subjectId = "phy",
                    nameBn = "পদার্থবিজ্ঞান",
                    onBack = {},
                    viewModel = ChapterBrowseViewModel("phy", ObserveBookmarkedIdsUseCase(bookmarkRepo), AddBookmarkUseCase(bookmarkRepo), RemoveBookmarkUseCase(bookmarkRepo))
                )
            }
        }

        waitForText("পদার্থবিজ্ঞান")
        composeTestRule.onNodeWithText(shown("১ম অধ্যায়: ভৌত রাশি ও পরিমাপ", "Chapter 1: Physical quantities and measurement")).assertExists()
    }

    @Test
    fun selectingAnotherChapterSwapsItsQuestions() {
        composeTestRule.setContent {
            ProstutiTheme {
                ChapterBrowseScreen(
                    subjectId = "phy",
                    nameBn = "পদার্থবিজ্ঞান",
                    onBack = {},
                    viewModel = ChapterBrowseViewModel("phy", ObserveBookmarkedIdsUseCase(bookmarkRepo), AddBookmarkUseCase(bookmarkRepo), RemoveBookmarkUseCase(bookmarkRepo))
                )
            }
        }

        waitForText("দৈর্ঘ্যের SI একক কী?") // ch1's first question
        composeTestRule.onNodeWithText(shown("২য় অধ্যায়: গতিবিদ্যা", "Chapter 2: Kinematics")).performClick()

        waitForText("সমবেগে চলমান বস্তুর ত্বরণ কত?") // ch2's first question
        composeTestRule.onAllNodesWithText("দৈর্ঘ্যের SI একক কী?").fetchSemanticsNodes().let {
            assert(it.isEmpty()) { "ch1's question should no longer be shown after switching to ch2" }
        }
    }
}
