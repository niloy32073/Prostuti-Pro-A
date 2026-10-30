package com.prostuti.student.ui.screen.qb

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** ChapterBrowseViewModel has no repository dependency yet (dummy data, see its TODOs), so this
 * test constructs it directly with no fakes needed. */
@RunWith(AndroidJUnit4::class)
class ChapterBrowseScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

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
                    viewModel = ChapterBrowseViewModel("phy")
                )
            }
        }

        waitForText("পদার্থবিজ্ঞান")
        composeTestRule.onNodeWithText("১ম অধ্যায়: ভৌত রাশি ও পরিমাপ").assertExists()
    }

    @Test
    fun selectingAnotherChapterSwapsItsQuestions() {
        composeTestRule.setContent {
            ProstutiTheme {
                ChapterBrowseScreen(
                    subjectId = "phy",
                    nameBn = "পদার্থবিজ্ঞান",
                    onBack = {},
                    viewModel = ChapterBrowseViewModel("phy")
                )
            }
        }

        waitForText("দৈর্ঘ্যের SI একক কী?") // ch1's first question
        composeTestRule.onNodeWithText("২য় অধ্যায়: গতিবিদ্যা").performClick()

        waitForText("সমবেগে চলমান বস্তুর ত্বরণ কত?") // ch2's first question
        composeTestRule.onAllNodesWithText("দৈর্ঘ্যের SI একক কী?").fetchSemanticsNodes().let {
            assert(it.isEmpty()) { "ch1's question should no longer be shown after switching to ch2" }
        }
    }
}
