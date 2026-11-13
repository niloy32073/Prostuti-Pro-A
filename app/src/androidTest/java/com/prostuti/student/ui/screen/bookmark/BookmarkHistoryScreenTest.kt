package com.prostuti.student.ui.screen.bookmark

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.R
import com.prostuti.student.data.model.response.McqDetail
import com.prostuti.student.data.model.response.McqDetailOption
import com.prostuti.student.data.model.enums.BookmarkResourceType
import com.prostuti.student.data.service.local.BookmarkEntity
import com.prostuti.student.ui.screen.bookmark.component.BOOKMARK_ANSWER_TOGGLE_TAG
import com.prostuti.student.ui.screen.bookmark.component.BOOKMARK_CARD_TAG
import com.prostuti.student.ui.screen.bookmark.component.BOOKMARK_REMOVE_TAG
import com.prostuti.student.ui.theme.ProstutiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Drives the stateless content with plain state, so no Koin or network is needed. */
@RunWith(AndroidJUnit4::class)
class BookmarkHistoryScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun mcq(id: String) = McqDetail(
        id = id,
        questionText = "Question $id",
        options = listOf(
            McqDetailOption(id = "$id-a", optionText = "Answer A", order = 1, isCorrect = true),
            McqDetailOption(id = "$id-b", optionText = "Answer B", order = 2)
        ),
        explanation = "Because of $id"
    )

    private val loaded = BookmarkHistoryUiState(
        hasLoaded = true,
        bookmarks = listOf(
            BookmarkEntity(BookmarkResourceType.MCQ, "m1", "b1", createdAt = "2026-10-02T10:00:00Z"),
            BookmarkEntity(BookmarkResourceType.MCQ, "m2", "b2", createdAt = "2026-10-01T10:00:00Z")
        ),
        mcqs = mapOf("m1" to mcq("m1")),
        failedMcqIds = setOf("m2")
    )

    private fun setContent(
        initial: BookmarkHistoryUiState,
        onRemove: (String) -> Unit = {},
        onRetry: (String) -> Unit = {}
    ) {
        composeTestRule.setContent {
            var state by mutableStateOf(initial)
            ProstutiTheme {
                BookmarkHistoryContent(
                    state = state,
                    onBack = {},
                    onRefresh = {},
                    onClearError = {},
                    onToggleAnswer = { id ->
                        state = state.copy(
                            revealedMcqIds = if (id in state.revealedMcqIds) state.revealedMcqIds - id else state.revealedMcqIds + id
                        )
                    },
                    onRemove = onRemove,
                    onRetryMcq = onRetry
                )
            }
        }
    }

    @Test
    fun showsEachBookmarkWithItsQuestionOrALoadError() {
        setContent(loaded)

        composeTestRule.onNodeWithTag(BOOKMARK_CARD_TAG + "m1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Question m1").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.bookmark_question_failed)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.bookmark_history_count, 2)).assertIsDisplayed()
    }

    @Test
    fun showAnswerRevealsTheExplanationInsideTheCard() {
        setContent(loaded)
        composeTestRule.onNodeWithText("Because of m1").assertDoesNotExist()

        composeTestRule.onNodeWithTag(BOOKMARK_ANSWER_TOGGLE_TAG + "m1").performClick()

        composeTestRule.onNodeWithText("Because of m1").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.bookmark_hide_answer)).assertIsDisplayed()
    }

    @Test
    fun heartAndRetryReportTheirMcq() {
        var removed: String? = null
        var retried: String? = null
        setContent(loaded, onRemove = { removed = it }, onRetry = { retried = it })

        composeTestRule.onNodeWithTag(BOOKMARK_REMOVE_TAG + "m1").performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.bookmark_retry)).performClick()

        assertEquals("m1", removed)
        assertEquals("m2", retried)
    }

    @Test
    fun noBookmarksShowsTheEmptyMessage() {
        setContent(BookmarkHistoryUiState(hasLoaded = true))

        composeTestRule.onNodeWithText(context.getString(R.string.bookmark_history_empty)).assertIsDisplayed()
    }
}
